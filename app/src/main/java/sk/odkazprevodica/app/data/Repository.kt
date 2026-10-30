package sk.odkazprevodica.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

/** Výsledok odoslania z pohľadu odosielateľa. Nikdy neprezrádza, či ŠPZ má konto. */
enum class SendResult { SENT, LIMIT_PLATE, LIMIT_DAY }

enum class AddCarResult { ADDED, INVALID, DUPLICATE, LIMIT }

/**
 * Jediný vstup UI k dátam.
 *
 * - Ak je v aplikácii Firebase (súbor google-services.json pri zostavení),
 *   odkazy idú cez server medzi zariadeniami – [RemoteBackend].
 * - Inak beží testovací režim: všetko v telefóne, odkaz na vlastnú ŠPZ
 *   príde do vlastnej schránky.
 */
object Repository {

    private const val PREFS = "odkaz_data"
    private const val KEY = "state_v1"

    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Jednorazové hlásenia pre UI (ID textu), napr. chyba odoslania. */
    val events: SharedFlow<Int> = _events.asSharedFlow()

    private var remote: RemoteBackend? = null

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            _state.value = AppData(online = true)
            remote = RemoteBackend(
                prefs = prefs,
                update = { f -> _state.value = f(_state.value) },
                current = { _state.value },
                emit = { res -> _events.tryEmit(res) },
            ).also { it.start() }
        } else {
            val loaded = prefs.getString(KEY, null)?.let { runCatching { decode(it) }.getOrNull() }
            _state.value = (loaded ?: firstRun()).purgeOld()
            saveLocal()
        }
    }

    internal fun emit(@StringRes res: Int) {
        _events.tryEmit(res)
    }

    /** Nový FCM token (z MessagingService). */
    fun onNewPushToken(token: String) {
        remote?.saveToken(token)
    }

    // ---- Autá ----

    fun addCar(plateInput: String, alias: String): AddCarResult {
        val plate = normalizePlate(plateInput)
        val s = _state.value
        val name = alias.trim().ifEmpty { displayPlate(plate) }
        return when {
            !isValidSkPlate(plate) -> AddCarResult.INVALID
            s.car(plate) != null -> AddCarResult.DUPLICATE
            s.cars.size >= MAX_CARS -> AddCarResult.LIMIT
            else -> {
                val r = remote
                if (r != null) r.addCar(plate, name)
                else updateLocal { it.copy(cars = it.cars + Car(plate, name)) }
                AddCarResult.ADDED
            }
        }
    }

    fun renameCar(plate: String, alias: String) {
        val name = alias.trim().ifEmpty { displayPlate(plate) }
        val r = remote
        if (r != null) r.renameCar(plate, name)
        else updateLocal { s -> s.copy(cars = s.cars.map { if (it.plate == plate) it.copy(alias = name) else it }) }
    }

    /** Odobratie auta zmaže aj jeho odkazy v tomto telefóne. */
    fun removeCar(plate: String) {
        val r = remote
        if (r != null) r.removeCar(plate)
        else updateLocal { s -> s.copy(cars = s.cars.filterNot { it.plate == plate }, inbox = s.inbox.filterNot { it.plate == plate }) }
    }

    // ---- Odosielanie ----

    fun sendMessage(plateInput: String, template: MessageTemplate): SendResult {
        val plate = normalizePlate(plateInput)
        val s = _state.value
        // Limity kontroluje aj server; tu iba aby používateľ dostal vysvetlenie.
        val today = s.sent.filter { isToday(it.timestamp) }
        if (today.size >= LIMIT_PER_DAY) return SendResult.LIMIT_DAY
        if (today.count { it.plate == plate } >= LIMIT_PER_PLATE_PER_DAY) return SendResult.LIMIT_PLATE

        val r = remote
        if (r != null) {
            r.sendMessage(plate, template)
        } else {
            val now = System.currentTimeMillis()
            updateLocal { st ->
                val id = newId()
                val sent = st.sent + SentMessage(id, plate, template, now)
                // Testovací režim: odkaz na vlastné auto príde do vlastnej schránky.
                val inbox = if (st.car(plate) != null) {
                    st.inbox + ReceivedMessage(id, plate, template, st.pseudonym, now)
                } else st.inbox
                st.copy(sent = sent, inbox = inbox)
            }
        }
        return SendResult.SENT
    }

    // ---- Prijaté odkazy ----

    fun markSeen(id: String) {
        val r = remote
        if (r != null) r.markSeen(id) else updateMessageLocal(id) { it.copy(seen = true) }
    }

    fun confirmRead(id: String) {
        val r = remote
        if (r != null) r.updateMessage(id, mapOf("readConfirmed" to true))
        else {
            updateMessageLocal(id) { it.copy(seen = true, readConfirmed = true) }
            updateLocal { s -> s.copy(sent = s.sent.map { if (it.id == id) it.copy(readConfirmed = true) else it }) }
        }
    }

    fun reply(id: String, reply: ReplyTemplate) {
        val r = remote
        if (r != null) {
            r.markSeen(id)
            r.updateMessage(id, mapOf("reply" to reply.id))
        } else {
            updateMessageLocal(id) { it.copy(seen = true, reply = reply) }
            // Testovací režim: odpoveď na vlastný odkaz sa ukáže v Odoslaných.
            updateLocal { s -> s.copy(sent = s.sent.map { if (it.id == id) it.copy(reply = reply) else it }) }
        }
    }

    fun report(id: String) {
        val r = remote
        if (r != null) r.updateMessage(id, mapOf("reported" to true))
        else updateMessageLocal(id) { it.copy(reported = true) }
    }

    fun blockSenderOf(id: String) {
        val m = _state.value.inbox.firstOrNull { it.id == id } ?: return
        val r = remote
        if (r != null) r.block(m.blockKey, m.senderPseudonym)
        else updateLocal { it.copy(blocked = it.blocked + (m.blockKey to m.senderPseudonym)) }
    }

    fun unblock(key: String) {
        val r = remote
        if (r != null) r.unblock(key) else updateLocal { it.copy(blocked = it.blocked - key) }
    }

    /** Zmaže všetko a vytvorí nové anonymné konto / nový pseudonym. */
    fun wipeAll() {
        val r = remote
        if (r != null) r.wipeAll()
        else {
            _state.value = AppData(pseudonym = newPseudonym())
            saveLocal()
        }
    }

    // ---- Lokálny režim ----

    private fun updateMessageLocal(id: String, f: (ReceivedMessage) -> ReceivedMessage) = updateLocal { s ->
        s.copy(inbox = s.inbox.map { if (it.id == id) f(it) else it })
    }

    private fun updateLocal(f: (AppData) -> AppData) {
        _state.value = f(_state.value)
        saveLocal()
    }

    private fun firstRun() = AppData(
        pseudonym = newPseudonym(),
        cars = DemoData.cars(),
        inbox = DemoData.inbox(),
    )

    private fun AppData.purgeOld(): AppData {
        val limit = System.currentTimeMillis() - RETENTION_DAYS * 24 * 60 * 60 * 1000L
        return copy(inbox = inbox.filter { it.timestamp >= limit }, sent = sent.filter { it.timestamp >= limit })
    }

    internal fun newPseudonym() = "Vodič #" + Random.nextInt(1000, 10000)
    private fun newId() = UUID.randomUUID().toString()

    private fun saveLocal() {
        if (::prefs.isInitialized && remote == null) prefs.edit().putString(KEY, encode(_state.value)).apply()
    }

    private fun encode(s: AppData): String = JSONObject().apply {
        put("pseudonym", s.pseudonym)
        put("cars", JSONArray().apply {
            s.cars.forEach { put(JSONObject().put("plate", it.plate).put("alias", it.alias)) }
        })
        put("inbox", JSONArray().apply {
            s.inbox.forEach {
                put(JSONObject()
                    .put("id", it.id).put("plate", it.plate).put("tpl", it.template.id)
                    .put("sender", it.senderPseudonym).put("ts", it.timestamp)
                    .put("seen", it.seen).put("read", it.readConfirmed)
                    .put("reply", it.reply?.id ?: "").put("reported", it.reported))
            }
        })
        put("sent", JSONArray().apply {
            s.sent.forEach {
                put(JSONObject().put("id", it.id).put("plate", it.plate).put("tpl", it.template.id).put("ts", it.timestamp)
                    .put("reply", it.reply?.id ?: "").put("read", it.readConfirmed))
            }
        })
        put("blocked", JSONObject().apply { s.blocked.forEach { (k, v) -> put(k, v) } })
    }.toString()

    private fun decode(raw: String): AppData {
        val o = JSONObject(raw)
        fun JSONObject.arr(name: String): List<JSONObject> {
            val a = optJSONArray(name) ?: return emptyList()
            return (0 until a.length()).map { a.getJSONObject(it) }
        }
        // v0.2 ukladala blokovaných ako zoznam pseudonymov
        val blocked: Map<String, String> = o.optJSONObject("blocked")?.let { b ->
            b.keys().asSequence().associateWith { b.optString(it) }
        } ?: o.optJSONArray("blocked")?.let { a ->
            (0 until a.length()).map { a.getString(it) }.associateWith { it }
        } ?: emptyMap()
        return AppData(
            pseudonym = o.optString("pseudonym").ifEmpty { newPseudonym() },
            cars = o.arr("cars").map { Car(it.getString("plate"), it.optString("alias")) },
            inbox = o.arr("inbox").mapNotNull { m ->
                val tpl = MessageTemplate.fromId(m.getString("tpl")) ?: return@mapNotNull null
                ReceivedMessage(
                    id = m.getString("id"),
                    plate = m.getString("plate"),
                    template = tpl,
                    senderPseudonym = m.optString("sender"),
                    timestamp = m.getLong("ts"),
                    seen = m.optBoolean("seen"),
                    readConfirmed = m.optBoolean("read"),
                    reply = ReplyTemplate.fromId(m.optString("reply")),
                    reported = m.optBoolean("reported"),
                )
            },
            sent = o.arr("sent").mapNotNull { m ->
                val tpl = MessageTemplate.fromId(m.getString("tpl")) ?: return@mapNotNull null
                SentMessage(
                    m.getString("id"), m.getString("plate"), tpl, m.getLong("ts"),
                    reply = ReplyTemplate.fromId(m.optString("reply")),
                    readConfirmed = m.optBoolean("read"),
                )
            },
            blocked = blocked,
        )
    }
}
