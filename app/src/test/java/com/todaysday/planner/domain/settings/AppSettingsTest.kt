package com.todaysday.planner.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** Checks the simple settings: defaults, saving as text, and fixing senseless values. */
class AppSettingsTest {

    @Test
    fun the_defaults() {
        val settings = AppSettings()

        assertEquals(15, settings.minSessionMinutes)
        assertEquals(120, settings.maxSessionMinutes)
        assertEquals(LocalTime.of(4, 0), settings.dayRollover)
    }

    @Test
    fun nothing_saved_gives_the_defaults() {
        assertEquals(AppSettings(), AppSettings.fromMap(emptyMap()))
    }

    @Test
    fun settings_survive_being_saved_as_text() {
        val settings = AppSettings(
            minSessionMinutes = 10, maxSessionMinutes = 90, dayRollover = LocalTime.of(5, 30)
        )

        assertEquals(settings, AppSettings.fromMap(settings.toMap()))
    }

    @Test
    fun unreadable_values_use_the_defaults_and_unknown_names_are_ignored() {
        val settings = AppSettings.fromMap(
            mapOf(
                AppSettings.KEY_MIN_SESSION to "abc",
                AppSettings.KEY_MAX_SESSION to "45",
                AppSettings.KEY_DAY_ROLLOVER to "not a time",
                "default_rules_seeded" to "true"
            )
        )

        assertEquals(15, settings.minSessionMinutes)
        assertEquals(45, settings.maxSessionMinutes)
        assertEquals(LocalTime.of(4, 0), settings.dayRollover)
    }

    @Test
    fun the_longest_session_is_never_shorter_than_the_shortest() {
        val fixed = AppSettings(minSessionMinutes = 60, maxSessionMinutes = 30).sanitized()

        assertEquals(60, fixed.minSessionMinutes)
        assertEquals(60, fixed.maxSessionMinutes)
    }

    @Test
    fun sessions_are_at_least_one_minute_and_at_most_a_day() {
        val tooSmall = AppSettings(minSessionMinutes = 0, maxSessionMinutes = -5).sanitized()
        assertEquals(1, tooSmall.minSessionMinutes)
        assertEquals(1, tooSmall.maxSessionMinutes)

        val tooBig = AppSettings(minSessionMinutes = 5000, maxSessionMinutes = 9000).sanitized()
        assertEquals(1440, tooBig.minSessionMinutes)
        assertEquals(1440, tooBig.maxSessionMinutes)
    }

    @Test
    fun values_read_from_text_are_fixed_too() {
        val settings = AppSettings.fromMap(
            mapOf(AppSettings.KEY_MIN_SESSION to "90", AppSettings.KEY_MAX_SESSION to "30")
        )

        assertEquals(90, settings.minSessionMinutes)
        assertEquals(90, settings.maxSessionMinutes)
    }
}