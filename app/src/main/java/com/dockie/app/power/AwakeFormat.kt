package com.dockie.app.power

/** Presets and labels for the "stay awake" duration setting. */
object AwakeFormat {
    /** Minutes; 0 means "until removed". */
    val PRESETS_MINUTES = listOf(0, 5, 15, 30, 60, 120, 240, 480)

    fun label(minutes: Int): String {
        if (minutes <= 0) return "Until you remove it"
        if (minutes < 60) return "$minutes minutes"
        val hours = minutes / 60
        val rest = minutes % 60
        val hourPart = if (hours == 1) "1 hour" else "$hours hours"
        return if (rest == 0) hourPart else "$hourPart $rest min"
    }

    /** Short countdown text such as "24 min left" or "1 h 5 min left". */
    fun minutesLeft(remainingMs: Long): String {
        if (remainingMs <= 0) return "Ending"
        val totalMinutes = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
        if (totalMinutes < 60) return "$totalMinutes min left"
        val hours = totalMinutes / 60
        val rest = totalMinutes % 60
        return if (rest == 0) "$hours h left" else "$hours h $rest min left"
    }
}
