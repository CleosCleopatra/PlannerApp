package com.todaysday.planner.domain.settings

import java.time.LocalTime

/**
 * The simple settings that are not time rules.
 *
 * - minSessionMinutes: the shortest stretch of work the scheduler will plan in one go
 * - maxSessionMinutes: the longest stretch of work it will plan in one go
 * - dayRollover:       if you don't press "Going to bed", a new day starts automatically at this time
 *
 * They are saved as name/value pairs (toMap / fromMap), so adding a setting later is easy.
 */
data class AppSettings(
    val minSessionMinutes: Int = DEFAULT_MIN_SESSION_MINUTES,
    val maxSessionMinutes: Int = DEFAULT_MAX_SESSION_MINUTES,
    val dayRollover: LocalTime = DEFAULT_DAY_ROLLOVER
) {
    /** A copy with values that make sense: at least 1 minute, and max never below min. */
    fun sanitized(): AppSettings {
        val min = minSessionMinutes.coerceIn(1, MAX_ALLOWED_MINUTES)
        val max = maxSessionMinutes.coerceIn(min, MAX_ALLOWED_MINUTES)
        return copy(minSessionMinutes = min, maxSessionMinutes = max)
    }

    /** The settings as name/value text pairs, ready to be saved. */
    fun toMap(): Map<String, String> = mapOf(
        KEY_MIN_SESSION to minSessionMinutes.toString(),
        KEY_MAX_SESSION to maxSessionMinutes.toString(),
        KEY_DAY_ROLLOVER to dayRollover.toString()
    )

    companion object {
        const val DEFAULT_MIN_SESSION_MINUTES = 15
        const val DEFAULT_MAX_SESSION_MINUTES = 120
        val DEFAULT_DAY_ROLLOVER: LocalTime = LocalTime.of(4, 0)

        private const val MAX_ALLOWED_MINUTES = 24 * 60

        const val KEY_MIN_SESSION = "min_session_minutes"
        const val KEY_MAX_SESSION = "max_session_minutes"
        const val KEY_DAY_ROLLOVER = "day_rollover_time"

        /**
         * Builds the settings from saved pairs. Missing, unreadable or senseless values are
         * replaced by the defaults, and unknown names are ignored.
         */
        fun fromMap(map: Map<String, String>): AppSettings = AppSettings(
            minSessionMinutes = map[KEY_MIN_SESSION]?.toIntOrNull() ?: DEFAULT_MIN_SESSION_MINUTES,
            maxSessionMinutes = map[KEY_MAX_SESSION]?.toIntOrNull() ?: DEFAULT_MAX_SESSION_MINUTES,
            dayRollover = map[KEY_DAY_ROLLOVER]
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                ?: DEFAULT_DAY_ROLLOVER
        ).sanitized()
    }
}