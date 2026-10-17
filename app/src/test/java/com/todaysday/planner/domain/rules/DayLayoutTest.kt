package com.todaysday.planner.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Checks the fixed shape of a day. (17 October 2026 is a Saturday.) */
class DayLayoutTest {

    private val day = LocalDate.of(2026, 10, 17)

    private fun layout(vararg rules: TimeRule, wakeUp: LocalDateTime? = null) =
        RuleEngine(rules.toList()).dayLayout(day, wakeUp)

    @Test
    fun a_day_with_the_built_in_values() {
        val layout = layout()

        assertEquals(LocalDateTime.of(2026, 10, 17, 7, 0), layout.wakeUp)
        assertEquals(LocalDateTime.of(2026, 10, 17, 7, 45), layout.morningRoutineEnd)
        assertEquals(LocalDateTime.of(2026, 10, 17, 22, 30), layout.bedTime)
        assertEquals(LocalDateTime.of(2026, 10, 17, 21, 45), layout.eveningRoutineStart)
        assertEquals(930, layout.awakeMinutes)      // 15 h 30 min
        assertEquals(75, layout.mealMinutes)        // lunch 30 + dinner 45
        // 930 - 45 - 45 (routines) - 75 (meals) - 120 (free) - 60 (waste)
        assertEquals(585, layout.workCapacityMinutes)
    }

    @Test
    fun going_to_bed_moves_the_start_of_the_day() {
        // Went to bed at 01:00, which with 8 h 30 min of sleep means getting up at 09:30.
        val wakeUp = LocalDateTime.of(2026, 10, 17, 9, 30)
        val layout = layout(wakeUp = wakeUp)

        assertEquals(wakeUp, layout.wakeUp)
        assertEquals(LocalDateTime.of(2026, 10, 17, 10, 15), layout.morningRoutineEnd)
        // Bedtime does not move.
        assertEquals(LocalDateTime.of(2026, 10, 17, 22, 30), layout.bedTime)
        assertEquals(435, layout.workCapacityMinutes)
    }

    @Test
    fun less_sleep_means_a_later_bedtime() {
        val layout = layout(SleepLengthRule(minutes = 420)) // 7 hours

        assertEquals(LocalDateTime.of(2026, 10, 18, 0, 0), layout.bedTime)
        assertEquals(LocalDateTime.of(2026, 10, 17, 23, 15), layout.eveningRoutineStart)
        assertEquals(1020, layout.awakeMinutes)
    }

    @Test
    fun meal_windows_belong_to_the_day() {
        val meals = layout().meals

        assertEquals(listOf(MealKind.LUNCH, MealKind.DINNER), meals.map { it.kind })
        assertEquals(LocalDateTime.of(2026, 10, 17, 12, 0), meals[0].earliestStart)
        assertEquals(LocalDateTime.of(2026, 10, 17, 14, 0), meals[0].latestStart)
        assertEquals(LocalDateTime.of(2026, 10, 17, 17, 30), meals[1].earliestStart)
        assertEquals(LocalDateTime.of(2026, 10, 17, 19, 30), meals[1].latestStart)
    }

    @Test
    fun a_meal_window_that_ends_before_it_starts_is_made_into_one_moment() {
        val backwards = MealRule(
            meal = MealKind.LUNCH, minutes = 30,
            earliestStart = LocalTime.of(13, 0), latestStart = LocalTime.of(11, 0)
        )
        val lunch = layout(backwards).meals.first { it.kind == MealKind.LUNCH }

        assertEquals(LocalDateTime.of(2026, 10, 17, 13, 0), lunch.earliestStart)
        assertEquals(LocalDateTime.of(2026, 10, 17, 13, 0), lunch.latestStart)
    }

    @Test
    fun the_work_capacity_follows_the_rules() {
        // 90 minutes less free time: 585 + 90.
        assertEquals(675, layout(DailyFreeTimeRule(minutes = 30)).workCapacityMinutes)

        // A lunch of 0 minutes is skipped: 585 + 30.
        val noLunch = MealRule(
            meal = MealKind.LUNCH, minutes = 0,
            earliestStart = LocalTime.of(12, 0), latestStart = LocalTime.of(14, 0)
        )
        assertEquals(615, layout(noLunch).workCapacityMinutes)
    }

    @Test
    fun the_work_capacity_is_never_negative() {
        // 20 hours of sleep leaves only 4 awake hours, which the fixed things already use up.
        val layout = layout(SleepLengthRule(minutes = 1200))

        assertEquals(240, layout.awakeMinutes)
        assertEquals(0, layout.workCapacityMinutes)
    }

    @Test
    fun disabled_rules_do_not_change_the_day() {
        val layout = layout(DailyFreeTimeRule(minutes = 0, enabled = false))

        assertEquals(120, layout.freeTimeMinutes)
        assertEquals(585, layout.workCapacityMinutes)
    }
}