package sk.odkazprevodica.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

/** Výsledok odoslania z pohľadu odosielateľa. Nikdy neprezrádza, či ŠPZ má konto. */
enum class SendResult { SENT, LIMIT_PLATE, LIMIT_DAY }

enum class AddCarResult { ADDED, INVALID, DUPLICATE, LIMIT }

/**
 * Lokálne úložisko (v0.2). Všetko je v telefóne – v ďalšej verzii sa
 * odosielanie a schránka presunú na Firebase, rozhranie zostane rovnaké.
 *
 * Kým nie je server: odkaz poslaný na vlastné auto sa doručí do vlastnej
 * schránky, aby sa dal celý tok otestovať na jednom telefóne.
 */
object Repository {

    private const val PREFS = "odkaz_data"
    private const val KEY = "state_v1"

    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY, null)
        val loaded = stored?.let { runCatching { decode(it) }.getOrNull() }
        _state.value = (loaded ?: firstRun()).purgeOld()
        save()
    }

    // ---- Autá ----

    fun addCar(plateInput: String, alias: String): AddCarResult {
        val plate = normalizePlate(plateInput)
        val s = _state.value
        return when {
            !isValidSkPlate(plate) -> AddCarResult.INVALID
            s.car(plate) != null -> AddCarResult.DUPLICATE
            s.cars.size >= MAX_CARS -> AddCarResult.LIMIT
            else -> {
                // TODO (server): ak ŠPZ už niekto sleduje, vyžiadať pozvánku alebo nahlásiť spor.
                update { it.copy(cars = it.cars + Car(plate, alias.trim().ifEmpty { displayPlate(plate) })) }
                AddCarResult.ADDED
            }
        }
    }

    fun renameCar(plate: String, alias: String) = update { s ->
        s.copy(cars = s.cars.map { if (it.plate == plate) it.copy(alias = alias.trim().ifEmpty { displayPlate(plate) }) else it })
    }

    /** Odobratie auta zmaže aj jeho odkazy. */
    fun removeCar(plate: String) = update { s ->
        s.copy(cars = s.cars.filterNot { it.plate == plate }, inbox = s.inbox.filterNot { it.plate == plate })
    }

    // ---- Odosielanie ----

    fun sendMessage(plateInput: String, template: MessageTemplate): SendResult {
        val plate = normalizePlate(plateInput)
        val s = _state.value
        val today = s.sent.filter { isToday(it.timestamp) }
        if (today.size >= LIMIT_PER_DAY) return SendResult.LIMIT_DAY
        if (today.count { it.plate == plate } >= LIMIT_PER_PLATE_PER_DAY) return SendResult.LIMIT_PLATE

        val now = System.currentTimeMillis()
        update { st ->
            val sent = st.sent + SentMessage(newId(), plate, template, now)
            // Lokálny test: odkaz na vlastné auto príde do vlastnej schránky.
            // TODO (server): Cloud Function doručí odkaz majiteľovi, inak ho zahodí bez stopy.
            val inbox = if (st.car(plate) != null) {
                st.inbox + ReceivedMessage(newId(), plate, template, st.pseudonym, now)
            } else st.inbox
            st.copy(sent = sent, inbox = inbox)
        }
        return SendResult.SENT
    }

    // ---- Prijaté odkazy ----

    fun markSeen(id: String) = updateMessage(id) { it.copy(seen = true) }
    fun confirmRead(id: String) = updateMessage(id) { it.copy(seen = true, readConfirmed = true) }
    fun reply(id: String, reply: ReplyTemplate) = updateMessage(id) { it.copy(seen = true, reply = reply) }
    fun report(id: String) = updateMessage(id) { it.copy(reported = true) }

    fun blockSenderOf(id: String) {
        val sender = _state.value.inbox.firstOrNull { it.id == id }?.senderPseudonym ?: return
        update { if (sender in it.blocked) it else it.copy(blocked = it.blocked + sender) }
    }

    fun unblock(pseudonym: String) = update { it.copy(blocked = it.blocked - pseudonym) }

    /** Zmaže všetko a vytvorí nový pseudonym (bez ukážkových dát). */
    fun wipeAll() {
        _state.value = AppData(pseudonym = newPseudonym())
        save()
    }

    // ---- Interné ----

    private fun updateMessage(id: String, f: (ReceivedMessage) -> ReceivedMessage) = update { s ->
        s.copy(inbox = s.inbox.map { if (it.id == id) f(it) else it })
    }

    private fun update(f: (AppData) -> AppData) {
        _state.value = f(_state.value)
        save()
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

    private fun newPseudonym() = "Vodič #" + Random.nextInt(1000, 10000)
    private fun newId() = UUID.randomUUID().toString()

    private fun save() {
        if (::prefs.isInitialized) prefs.edit().putString(KEY, encode(_state.value)).apply()
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
                put(JSONObject().put("id", it.id).put("plate", it.plate).put("tpl", it.template.id).put("ts", it.timestamp))
            }
        })
        put("blocked", JSONArray(s.blocked))
    }.toString()

    private fun decode(raw: String): AppData {
        val o = JSONObject(raw)
        fun JSONObject.arr(name: String): List<JSONObject> {
            val a = optJSONArray(name) ?: return emptyList()
            return (0 until a.length()).map { a.getJSONObject(it) }
        }
        val blockedArr = o.optJSONArray("blocked")
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
                SentMessage(m.getString("id"), m.getString("plate"), tpl, m.getLong("ts"))
            },
            blocked = blockedArr?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        )
    }
}
