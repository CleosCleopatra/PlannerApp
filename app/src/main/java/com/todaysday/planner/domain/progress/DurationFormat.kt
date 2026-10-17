package com.todaysday.planner.domain.progress

/**
 * Turns minutes into short readable text for the small "time left" labels:
 * 0 -> "0 min", 45 -> "45 min", 60 -> "1 h", 200 -> "3 h 20 min".
 */
fun formatMinutes(minutes: Int): String {
    val total = minutes.coerceAtLeast(0)
    val hours = total / 60
    val rest = total % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}