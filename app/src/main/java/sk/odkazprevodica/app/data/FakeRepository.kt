package sk.odkazprevodica.app.data

/**
 * Testovacie dáta pre vývoj UI. V ďalšom kroku sa nahradí Firebase
 * (Auth + Firestore + Cloud Functions).
 */
object FakeRepository {

    val myPseudonym = "Vodič #4821"

    val cars = listOf(
        Car("BA842MX", "Škoda Octavia", unreadCount = 2, lastMessageLabel = "dnes 14:21"),
        Car("TT519KD", "Dodávka", unreadCount = 0, lastMessageLabel = null),
        Car("PK207AB", "Auto – manželka", unreadCount = 1, lastMessageLabel = "včera"),
    )

    val messages = listOf(
        ReceivedMessage("m1", "BA842MX", "Škoda Octavia", MessageTemplate.LIGHTS, "Vodič #7193", "Dnes 14:21"),
        ReceivedMessage("m2", "BA842MX", "Škoda Octavia", MessageTemplate.BLOCKING, "Vodič #2280", "Dnes 08:05"),
        ReceivedMessage("m3", "PK207AB", "Auto – manželka", MessageTemplate.WINDOW, "Vodič #5512", "Včera 17:40"),
    )

    fun message(id: String): ReceivedMessage? = messages.firstOrNull { it.id == id }

    fun latestMessageFor(plate: String): ReceivedMessage? = messages.firstOrNull { it.plate == plate }

    /**
     * Odoslanie odkazu. Výsledok je pre odosielateľa vždy rovnaký –
     * nedozvie sa, či ŠPZ má konto ani či bol odkaz doručený.
     */
    fun sendMessage(plate: String, template: MessageTemplate) {
        // TODO: volanie Cloud Function `sendMessage` (limity, doručenie, zahodenie bez stopy).
    }

    fun sendReply(messageId: String, reply: ReplyTemplate) {
        // TODO: Firestore zápis odpovede.
    }

    fun markAsRead(messageId: String) { /* TODO */ }
    fun blockSender(messageId: String) { /* TODO */ }
    fun report(messageId: String) { /* TODO */ }
}
