package com.todaysday.planner.domain.rules

import com.todaysday.planner.domain.model.Level
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/** Checks that every rule gets a name that says what it does. */
class RuleNamerTest {

    private fun name(rule: TimeRule) = RuleNamer.generate(rule)

    @Test
    fun sleep_routines_and_wake_up_time() {
        assertEquals("Sleep length: 8 h 30 min", name(SleepLengthRule()))
        assertEquals("Default wake-up time: 07:00", name(WakeUpTimeRule()))
        assertEquals("Morning routine: 45 min", name(MorningRoutineRule()))
        assertEquals("Evening routine: 45 min", name(EveningRoutineRule()))
    }

    @Test
    fun meals() {
        assertEquals("Lunch: 30 min, starts between 12:00 and 14:00", name(DefaultRules.meal(MealKind.LUNCH)))
        assertEquals("Dinner: 45 min, starts between 17:30 and 19:30", name(DefaultRules.meal(MealKind.DINNER)))
    }

    @Test
    fun free_time_wasted_time_and_half_state() {
        assertEquals("Free time: 2 h per day", name(DailyFreeTimeRule()))
        assertEquals("Wasted time: 1 h per day", name(DailyWasteRule()))
        assertEquals("Half state: 50% productivity", name(HalfStateRule()))
        assertEquals("Half state: 70% productivity", name(HalfStateRule(multiplier = 0.7)))
    }

    @Test
    fun productivity_rules_name_their_days() {
        assertEquals(
            "Tuesdays: 50% productivity",
            name(ProductivityRule(multiplier = 0.5, weekdays = setOf(DayOfWeek.TUESDAY)))
        )
        assertEquals("Every day: 80% productivity", name(ProductivityRule(multiplier = 0.8)))
        assertEquals(
            "Mon, Wed: 60% productivity",
            name(ProductivityRule(multiplier = 0.6, weekdays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY)))
        )
        assertEquals(
            "Monday to Friday: 90% productivity",
            name(
                ProductivityRule(
                    multiplier = 0.9,
                    weekdays = setOf(
                        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                    )
                )
            )
        )
        assertEquals(
            "Weekends: 120% productivity",
            name(ProductivityRule(multiplier = 1.2, weekdays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)))
        )
    }

    @Test
    fun productivity_rules_name_their_time_window_and_effort() {
        assertEquals(
            "Tuesdays, 14:00–16:00: 70% productivity",
            name(
                ProductivityRule(
                    multiplier = 0.7, weekdays = setOf(DayOfWeek.TUESDAY),
                    from = LocalTime.of(14, 0), until = LocalTime.of(16, 0)
                )
            )
        )
        assertEquals(
            "Every day: 90% productivity (high-effort entries only)",
            name(ProductivityRule(multiplier = 0.9, onlyForEffort = Level.HIGH))
        )
    }

    @Test
    fun low_effort_window() {
        assertEquals(
            "Prefer low-effort entries: Tuesdays, 14:00–16:00",
            name(LowProductivityWindowRule(weekdays = setOf(DayOfWeek.TUESDAY)))
        )
    }

    @Test
    fun a_name_typed_by_the_user_wins() {
        val rule = SleepLengthRule(customName = "My sleep")

        assertEquals("My sleep", RuleNamer.displayName(rule))
        // A blank name counts as no name.
        assertEquals("Sleep length: 8 h 30 min", RuleNamer.displayName(rule.copy(customName = "  ")))
        assertEquals("Sleep length: 8 h 30 min", RuleNamer.displayName(rule.copy(customName = null)))
    }

    @Test
    fun the_generated_name_follows_the_settings() {
        val rule = SleepLengthRule(minutes = 420)

        assertEquals("Sleep length: 7 h", RuleNamer.displayName(rule))
        assertEquals("Sleep length: 6 h 45 min", RuleNamer.displayName(rule.copy(minutes = 405)))
    }
}