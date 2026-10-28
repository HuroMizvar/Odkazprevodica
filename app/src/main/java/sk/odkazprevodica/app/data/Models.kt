package sk.odkazprevodica.app.data

/** Auto, ktoré používateľ sleduje (max. [MAX_CARS] na konto). */
data class Car(
    val plate: String,
    val alias: String,
    val unreadCount: Int,
    /** Čas posledného odkazu na zobrazenie, napr. „dnes 14:21“; null = žiadny. */
    val lastMessageLabel: String?,
)

/** Odkaz prijatý pre jedno z mojich áut. */
data class ReceivedMessage(
    val id: String,
    val plate: String,
    val carAlias: String,
    val template: MessageTemplate,
    val senderPseudonym: String,
    val timeLabel: String,
)

const val MAX_CARS = 20

/**
 * Normalizuje ŠPZ na tvar uložený v databáze: veľké písmená, bez medzier a pomlčiek.
 * „ba 842-mx“ → „BA842MX“.
 */
fun normalizePlate(input: String): String =
    input.uppercase().filter { it.isLetterOrDigit() }

/** Zobrazenie ŠPZ s pomlčkou za prvými dvoma znakmi: „BA842MX“ → „BA-842MX“. */
fun displayPlate(normalized: String): String =
    if (normalized.length > 2) normalized.take(2) + "-" + normalized.drop(2) else normalized

/** Slovenská ŠPZ: 7 znakov, začína dvoma písmenami. Zjednodušená kontrola pre v1. */
fun isValidSkPlate(normalized: String): Boolean =
    normalized.length == 7 && normalized.take(2).all { it in 'A'..'Z' }
