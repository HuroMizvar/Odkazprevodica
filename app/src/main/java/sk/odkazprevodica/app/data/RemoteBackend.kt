package sk.odkazprevodica.app.data

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import sk.odkazprevodica.app.R

/**
 * Odkazy cez Firebase.
 *
 * Štruktúra v databáze (Firestore):
 * - users/{uid}                 pseudonym, fcmToken, blocked {uid: pseudonym}
 * - users/{uid}/cars/{plate}    moje autá: alias, joinedAt
 * - users/{uid}/sent/{id}       moje odoslané odkazy (+ odpoveď, ktorú doplní server)
 * - plates/{plate}/members/{uid}  kto sleduje ŠPZ (vidí iba každý sám seba)
 * - plates/{plate}/messages/{id}  odkazy pre ŠPZ – odosielateľ ich nevie čítať
 *
 * Konto je anonymné (Firebase Anonymous Auth) – žiadny e-mail ani meno.
 */
internal class RemoteBackend(
    private val prefs: SharedPreferences,
    private val update: ((AppData) -> AppData) -> Unit,
    private val current: () -> AppData,
    private val emit: (Int) -> Unit,
) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val listeners = mutableListOf<ListenerRegistration>()
    private val plateListeners = mutableMapOf<String, ListenerRegistration>()
    private val messagesByPlate = mutableMapOf<String, List<ReceivedMessage>>()
    private val handler = Handler(Looper.getMainLooper())
    private var lastJoined: Map<String, Long> = emptyMap()
    private val seen: MutableSet<String> = prefs.getStringSet(KEY_SEEN, emptySet())!!.toMutableSet()

    private val uid: String? get() = auth.currentUser?.uid

    fun start() {
        val user = auth.currentUser
        if (user != null) onSignedIn(user.uid)
        else auth.signInAnonymously()
            .addOnSuccessListener { r -> r.user?.uid?.let(::onSignedIn) }
            .addOnFailureListener { emit(R.string.error_offline) }
    }

    private fun onSignedIn(uid: String) {
        ensureProfile(uid)
        listenUser(uid)
    }

    /** Vytvor profil s pseudonymom, ak ešte neexistuje. Bez internetu skúša znova. */
    private fun ensureProfile(uid: String) {
        val userRef = db.collection("users").document(uid)
        userRef.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    userRef.set(
                        mapOf(
                            "pseudonym" to Repository.newPseudonym(),
                            "blocked" to emptyMap<String, String>(),
                            "createdAt" to FieldValue.serverTimestamp(),
                        )
                    ).addOnSuccessListener { refreshToken() }
                } else refreshToken()
            }
            .addOnFailureListener { handler.postDelayed({ if (this.uid == uid) ensureProfile(uid) }, 10_000) }
    }

    private fun refreshToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { saveToken(it) }
    }

    fun saveToken(token: String) {
        val u = uid ?: return
        db.collection("users").document(u).set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    private fun listenUser(uid: String) {
        val userRef = db.collection("users").document(uid)

        listeners += userRef.addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            val blocked = (snap.get("blocked") as? Map<String, Any?>)
                ?.mapValues { it.value?.toString().orEmpty() } ?: emptyMap()
            update { it.copy(pseudonym = snap.getString("pseudonym").orEmpty(), blocked = blocked) }
        }

        listeners += userRef.collection("cars").addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val cars = snap.documents.map { Car(it.id, it.getString("alias") ?: displayPlate(it.id)) }
            val joined = snap.documents.associate { it.id to it.millis("joinedAt") }
            syncPlateListeners(joined)
            update { it.copy(cars = cars) }
        }

        listeners += userRef.collection("sent").addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val sent = snap.documents.mapNotNull { d ->
                val tpl = MessageTemplate.fromId(d.getString("templateId")) ?: return@mapNotNull null
                SentMessage(
                    id = d.id,
                    plate = d.getString("plate").orEmpty(),
                    template = tpl,
                    timestamp = d.millis("createdAt"),
                    reply = ReplyTemplate.fromId(d.getString("reply")),
                    readConfirmed = d.getBoolean("readConfirmed") == true,
                )
            }
            update { it.copy(sent = sent) }
        }
    }

    /** Pre každé moje auto počúvaj jeho odkazy (iba tie od chvíle, keď som ho pridal). */
    private fun syncPlateListeners(joined: Map<String, Long>) {
        lastJoined = joined
        (plateListeners.keys - joined.keys).forEach { plate ->
            plateListeners.remove(plate)?.remove()
            messagesByPlate.remove(plate)
        }
        joined.forEach { (plate, joinedAt) ->
            if (plate in plateListeners) return@forEach
            plateListeners[plate] = db.collection("plates").document(plate).collection("messages")
                .whereGreaterThanOrEqualTo("createdAt", Timestamp(java.util.Date(joinedAt - 60_000)))
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        // Napr. auto práve pridané a server ešte nepotvrdil členstvo – skús znova.
                        plateListeners.remove(plate)?.remove()
                        handler.postDelayed({ syncPlateListeners(lastJoined) }, 3_000)
                        return@addSnapshotListener
                    }
                    if (snap == null) return@addSnapshotListener
                    messagesByPlate[plate] = snap.documents.mapNotNull { it.toReceived(plate) }
                    publishInbox()
                }
        }
        publishInbox()
    }

    private fun DocumentSnapshot.toReceived(plate: String): ReceivedMessage? {
        val tpl = MessageTemplate.fromId(getString("templateId")) ?: return null
        return ReceivedMessage(
            id = id,
            plate = plate,
            template = tpl,
            senderPseudonym = getString("senderPseudonym").orEmpty(),
            senderUid = getString("senderUid").orEmpty(),
            timestamp = millis("createdAt"),
            seen = id in seen,
            readConfirmed = getBoolean("readConfirmed") == true,
            reply = ReplyTemplate.fromId(getString("reply")),
            reported = getBoolean("reported") == true,
        )
    }

    private fun publishInbox() {
        val all = messagesByPlate.values.flatten().map { it.copy(seen = it.id in seen) }
        update { it.copy(inbox = all) }
    }

    // ---- Akcie ----

    fun addCar(plate: String, alias: String) {
        val u = uid ?: return emit(R.string.error_offline)
        db.batch()
            .set(db.collection("users").document(u).collection("cars").document(plate),
                mapOf("alias" to alias, "joinedAt" to FieldValue.serverTimestamp()))
            .set(db.collection("plates").document(plate).collection("members").document(u),
                mapOf("joinedAt" to FieldValue.serverTimestamp()))
            .commit()
            .addOnFailureListener { emit(R.string.error_offline) }
    }

    fun renameCar(plate: String, alias: String) {
        val u = uid ?: return
        db.collection("users").document(u).collection("cars").document(plate).update("alias", alias)
    }

    fun removeCar(plate: String) {
        val u = uid ?: return
        db.batch()
            .delete(db.collection("users").document(u).collection("cars").document(plate))
            .delete(db.collection("plates").document(plate).collection("members").document(u))
            .commit()
    }

    fun sendMessage(plate: String, template: MessageTemplate) {
        val u = uid ?: return emit(R.string.error_offline)
        val msgRef = db.collection("plates").document(plate).collection("messages").document()
        db.batch()
            .set(msgRef, mapOf(
                "templateId" to template.id,
                "senderUid" to u,
                "senderPseudonym" to current().pseudonym,
                "createdAt" to FieldValue.serverTimestamp(),
            ))
            .set(db.collection("users").document(u).collection("sent").document(msgRef.id), mapOf(
                "plate" to plate,
                "templateId" to template.id,
                "createdAt" to FieldValue.serverTimestamp(),
            ))
            .commit()
            .addOnFailureListener { emit(R.string.error_offline) }
    }

    fun updateMessage(id: String, fields: Map<String, Any>) {
        val plate = current().inbox.firstOrNull { it.id == id }?.plate ?: return
        db.collection("plates").document(plate).collection("messages").document(id)
            .update(fields)
            .addOnFailureListener { emit(R.string.error_offline) }
    }

    fun markSeen(id: String) {
        if (seen.add(id)) {
            prefs.edit().putStringSet(KEY_SEEN, seen.toSet()).apply()
            publishInbox()
        }
    }

    fun block(senderUid: String, pseudonym: String) {
        val u = uid ?: return
        db.collection("users").document(u).update("blocked.$senderUid", pseudonym)
    }

    fun unblock(senderUid: String) {
        val u = uid ?: return
        db.collection("users").document(u).update("blocked.$senderUid", FieldValue.delete())
    }

    /** Zmaže moje údaje na serveri, zruší anonymné konto a založí nové. */
    fun wipeAll() {
        val user = auth.currentUser ?: return
        val u = user.uid
        val userRef = db.collection("users").document(u)
        val plates = current().cars.map { it.plate }
        val sentIds = current().sent.map { it.id }

        val batch = db.batch()
        plates.forEach { p ->
            batch.delete(userRef.collection("cars").document(p))
            batch.delete(db.collection("plates").document(p).collection("members").document(u))
        }
        sentIds.forEach { batch.delete(userRef.collection("sent").document(it)) }
        batch.delete(userRef)

        batch.commit().addOnCompleteListener {
            stopListeners()
            seen.clear()
            prefs.edit().remove(KEY_SEEN).apply()
            update { AppData(online = true) }
            user.delete().addOnCompleteListener {
                auth.signOut()
                start()
            }
        }
    }

    private fun stopListeners() {
        handler.removeCallbacksAndMessages(null)
        lastJoined = emptyMap()
        listeners.forEach { it.remove() }
        listeners.clear()
        plateListeners.values.forEach { it.remove() }
        plateListeners.clear()
        messagesByPlate.clear()
    }

    private fun DocumentSnapshot.millis(field: String): Long =
        getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: System.currentTimeMillis()

    companion object {
        private const val KEY_SEEN = "seen_ids"
    }
}
