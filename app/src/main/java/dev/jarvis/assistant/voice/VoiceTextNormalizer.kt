package dev.jarvis.assistant.voice

/**
 * Offline voice text preparation, independent of the Android framework.
 * Keeps personal message content on-device; never performs network access.
 */
object VoiceTextNormalizer {
    private val spaces = Regex("\\s+")
    private val battery = Regex("^Заряд батареи (\\d{1,3}) процентов$", RegexOption.IGNORE_CASE)
    private val time = Regex("^Сейчас (\\d{1,2}) (?:час|часа|часов)$", RegexOption.IGNORE_CASE)

    fun normalize(input: String): String {
        val text = input.trim().replace(spaces, " ")
        battery.matchEntire(text)?.let {
            val value = it.groupValues[1].toIntOrNull()
            if (value != null && value in 0..100) return "Заряд батареи: ${number(value)} ${percentUnit(value)}."
        }
        time.matchEntire(text)?.let {
            val value = it.groupValues[1].toIntOrNull()
            if (value != null && value in 0..23) return "Сейчас ${number(value)} ${hourUnit(value)}."
        }
        return text.take(700)
    }

    private fun number(v: Int): String {
        val ones = listOf("ноль","один","два","три","четыре","пять","шесть","семь","восемь","девять")
        val teen = listOf("десять","одиннадцать","двенадцать","тринадцать","четырнадцать","пятнадцать","шестнадцать","семнадцать","восемнадцать","девятнадцать")
        val tens = listOf("","","двадцать","тридцать","сорок","пятьдесят","шестьдесят","семьдесят","восемьдесят","девяносто")
        if (v == 100) return "сто"
        if (v < 10) return ones[v]
        if (v < 20) return teen[v - 10]
        return listOf(tens[v / 10], if (v % 10 == 0) "" else ones[v % 10])
            .filter { it.isNotBlank() }.joinToString(" ")
    }
    private fun percentUnit(value: Int): String =
        if (value % 100 in 11..14) "процентов" else when (value % 10) {
            1 -> "процент"
            2, 3, 4 -> "процента"
            else -> "процентов"
        }
    private fun hourUnit(value: Int): String =
        if (value % 100 in 11..14) "часов" else when (value % 10) {
            1 -> "час"
            2, 3, 4 -> "часа"
            else -> "часов"
        }
}
