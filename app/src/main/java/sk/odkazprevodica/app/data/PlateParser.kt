package sk.odkazprevodica.app.data

/**
 * Hľadá slovenskú ŠPZ v rozpoznanom texte (formát AA123AA).
 * OCR často zamieňa podobné znaky – podľa pozície ich opraví
 * (na miestach číslic O→0, I→1, B→8…; na miestach písmen naopak).
 */
object PlateParser {

    private val pattern = Regex("[A-Z]{2}[0-9]{3}[A-Z]{2}")

    private val toDigit = mapOf(
        'O' to '0', 'Q' to '0', 'D' to '0', 'I' to '1', 'L' to '1',
        'Z' to '2', 'S' to '5', 'B' to '8', 'G' to '6', 'T' to '7',
    )
    private val toLetter = mapOf(
        '0' to 'O', '1' to 'I', '2' to 'Z', '5' to 'S', '8' to 'B', '6' to 'G', '7' to 'T',
    )

    /** Vráti prvú nájdenú ŠPZ (normalizovanú, napr. BA842MX) alebo null. */
    fun find(lines: List<String>): String? {
        for (line in lines) {
            val compact = line.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
            if (compact.length < 7) continue
            for (start in 0..compact.length - 7) {
                fix(compact.substring(start, start + 7))?.let { return it }
            }
        }
        return null
    }

    private fun fix(candidate: String): String? {
        val fixed = candidate.mapIndexed { i, ch ->
            if (i in 2..4) toDigit[ch] ?: ch else toLetter[ch] ?: ch
        }.joinToString("")
        return if (pattern.matches(fixed)) fixed else null
    }
}
