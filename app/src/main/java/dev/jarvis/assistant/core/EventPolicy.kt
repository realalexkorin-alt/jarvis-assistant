package dev.jarvis.assistant.core

object EventPolicy {
    fun crossedBatteryDecile(previous: Int, current: Int, charging: Boolean): Boolean {
        if (charging || previous !in 0..100 || current !in 0..100 || current >= previous) return false
        val threshold = ((previous - 1) / 10) * 10
        return threshold >= 10 && current <= threshold
    }

    fun hourlyDue(minute: Int, currentHour: String, announcedHour: String?): Boolean =
        minute == 0 && currentHour.isNotBlank() && currentHour != announcedHour

    fun hourPhrase(hour: Int): String {
        require(hour in 0..23)
        val suffix = if (hour % 100 in 11..14) "часов" else when (hour % 10) {
            1 -> "час"
            2, 3, 4 -> "часа"
            else -> "часов"
        }
        return "Сейчас " + hour + " " + suffix
    }
}
