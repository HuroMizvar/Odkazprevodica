package sk.odkazprevodica.app.data

/**
 * Ukážkové dáta pri prvom spustení, aby sa dala aplikácia vyskúšať bez servera.
 * Používateľ ich môže zmazať (odobrať autá alebo Nastavenia → Vymazať všetky údaje).
 */
object DemoData {

    fun cars() = listOf(
        Car("BA842MX", "Škoda Octavia"),
        Car("TT519KD", "Dodávka"),
        Car("PK207AB", "Auto – manželka"),
    )

    fun inbox(now: Long = System.currentTimeMillis()): List<ReceivedMessage> {
        val h = 60 * 60 * 1000L
        return listOf(
            ReceivedMessage("demo1", "BA842MX", MessageTemplate.LIGHTS, "Vodič #7193", now - 5 * 60 * 1000L),
            ReceivedMessage("demo2", "BA842MX", MessageTemplate.BLOCKING, "Vodič #2280", now - 6 * h),
            ReceivedMessage("demo3", "PK207AB", MessageTemplate.WINDOW, "Vodič #5512", now - 26 * h),
        )
    }
}
