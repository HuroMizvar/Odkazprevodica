package sk.odkazprevodica.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Auto, ktoré používateľ sleduje (max. [MAX_CARS] na konto). */
data class Car(
    val plate: String,
    val alias: String,
)

/** Odkaz prijatý pre jedno z mojich áut. */
data class ReceivedMessage(
    val id: String,
    val plate: String,
    val template: MessageTemplate,
    val senderPseudonym: String,
    val timestamp: Long,
    /** Videné v aplikácii (iba lokálne, odosielateľ to nevidí). */
    val seen: Boolean = false,
    /** Majiteľ výslovne označil ako prečítané – toto by sa dozvedel odosielateľ. */
    val readConfirmed: Boolean = false,
    val reply: ReplyTemplate? = null,
    val reported: Boolean = false,
)

/** Odkaz, ktorý som odoslal. Bez stavu doručenia – zámerne. */
data class SentMessage(
    val id: String,
    val plate: String,
    val template: MessageTemplate,
    val timestamp: Long,
)

/** Celý lokálny stav aplikácie. */
data class AppData(
    val pseudonym: String = "",
    val cars: List<Car> = emptyList(),
    val inbox: List<ReceivedMessage> = emptyList(),
    val sent: List<SentMessage> = emptyList(),
    val blocked: List<String> = emptyList(),
) {
    /** Prijaté odkazy bez zablokovaných odosielateľov, najnovšie prvé. */
    val visibleInbox: List<ReceivedMessage>
        get() = inbox.filter { it.senderPseudonym !in blocked }.sortedByDescending { it.timestamp }

    fun inboxFor(plate: String) = visibleInbox.filter { it.plate == plate }
    fun unreadFor(plate: String) = inboxFor(plate).count { !it.seen }
    fun car(plate: String) = cars.firstOrNull { it.plate == plate }
}

const val MAX_CARS = 20

/** Limit odkazov od jedného odosielateľa na jednu ŠPZ za deň. */
const val LIMIT_PER_PLATE_PER_DAY = 3

/** Limit odkazov od jedného odosielateľa za deň celkom. */
const val LIMIT_PER_DAY = 20

/** Odkazy sa automaticky mažú po tomto počte dní. */
const val RETENTION_DAYS = 30L

/**
 * Normalizuje ŠPZ na tvar uložený v databáze: veľké písmená, bez medzier a pomlčiek.
 * „ba 842-mx“ → „BA842MX“.
 */
fun normalizePlate(input: String): String =
    input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

/** Zobrazenie ŠPZ s pomlčkou za prvými dvoma znakmi: „BA842MX“ → „BA-842MX“. */
fun displayPlate(normalized: String): String =
    if (normalized.length > 2) normalized.take(2) + "-" + normalized.drop(2) else normalized

/** Slovenská ŠPZ: 7 znakov, začína dvoma písmenami. Zjednodušená kontrola pre v1. */
fun isValidSkPlate(normalized: String): Boolean =
    normalized.length == 7 && normalized.take(2).all { it in 'A'..'Z' }

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("d. M.")

/** „dnes 14:21“, „včera 17:40“, „12. 10. 09:15“. */
fun formatTime(timestamp: Long, today: String = "dnes", yesterday: String = "včera"): String {
    val zone = ZoneId.systemDefault()
    val dt = Instant.ofEpochMilli(timestamp).atZone(zone)
    val now = LocalDate.now(zone)
    val day = when (dt.toLocalDate()) {
        now -> today
        now.minusDays(1) -> yesterday
        else -> dt.format(dateFmt)
    }
    return "$day ${dt.format(timeFmt)}"
}

fun isToday(timestamp: Long): Boolean {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate() == LocalDate.now(zone)
}
