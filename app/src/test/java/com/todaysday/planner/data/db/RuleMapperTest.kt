package com.todaysday.planner.data.db

import com.todaysday.planner.data.db.entities.TimeRuleEntity
import com.todaysday.planner.data.db.entities.TimeRuleType
import com.todaysday.planner.domain.model.Level
import com.todaysday.planner.domain.rules.DailyFreeTimeRule
import com.todaysday.planner.domain.rules.DailyWasteRule
import com.todaysday.planner.domain.rules.EveningRoutineRule
import com.todaysday.planner.domain.rules.HalfStateRule
import com.todaysday.planner.domain.rules.LowProductivityWindowRule
import com.todaysday.planner.domain.rules.MealKind
import com.todaysday.planner.domain.rules.MealRule
import com.todaysday.planner.domain.rules.MorningRoutineRule
import com.todaysday.planner.domain.rules.ProductivityRule
import com.todaysday.planner.domain.rules.SleepLengthRule
import com.todaysday.planner.domain.rules.TimeRule
import com.todaysday.planner.domain.rules.WakeUpTimeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Checks that turning a rule into a database row and back gives exactly the same rule.
 * If a setting were forgotten in the mapper, one of these tests would fail.
 */
class RuleMapperTest {

    private val everyKind: List<TimeRule> = listOf(
        SleepLengthRule(id = 1, minutes = 400, customName = "My sleep"),
        WakeUpTimeRule(id = 2, enabled = false, time = LocalTime.of(6, 30)),
        MorningRoutineRule(id = 3, minutes = 20),
        EveningRoutineRule(id = 4, minutes = 30),
        MealRule(
            id = 5, meal = MealKind.DINNER, minutes = 50,
            earliestStart = LocalTime.of(18, 0), latestStart = LocalTime.of(20, 15)
        ),
        DailyFreeTimeRule(id = 6, minutes = 90),
        DailyWasteRule(id = 7, minutes = 15),
        HalfStateRule(id = 8, multiplier = 0.65),
        ProductivityRule(
            id = 9, multiplier = 0.7, weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            from = LocalTime.of(14, 0), until = LocalTime.of(16, 0), onlyForEffort = Level.HIGH
        ),
        ProductivityRule(id = 10, multiplier = 1.2), // no weekdays, no window, no effort
        LowProductivityWindowRule(
            id = 11, weekdays = setOf(DayOfWeek.MONDAY),
            from = LocalTime.of(13, 0), until = LocalTime.of(15, 30)
        )
    )

    @Test
    fun every_kind_of_rule_survives_a_round_trip() {
        for (rule in everyKind) {
            assertEquals(rule, rule.toEntity().toDomain())
        }
    }

    @Test
    fun every_kind_of_rule_has_its_own_type_in_the_database() {
        val types = everyKind.map { it.toEntity().type }.toSet()

        assertEquals(TimeRuleType.entries.toSet(), types)
    }

    @Test
    fun a_new_rule_keeps_id_zero() {
        assertEquals(0L, SleepLengthRule().toEntity().id)
    }

    @Test
    fun times_and_weekdays_are_saved_in_simple_columns() {
        val row = ProductivityRule(
            multiplier = 0.7, weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            from = LocalTime.of(14, 0), until = LocalTime.of(16, 30)
        ).toEntity()

        assertEquals("14:00", row.timeA)
        assertEquals("16:30", row.timeB)
        assertEquals(1 + 4, row.weekdaysMask)
        assertEquals(0.7, row.multiplier!!, 1e-9)
    }

    @Test
    fun a_productivity_rule_without_a_window_saves_no_times() {
        val row = ProductivityRule(multiplier = 0.8).toEntity()

        assertNull(row.timeA)
        assertNull(row.timeB)
        assertNull(row.effort)
        assertEquals(0, row.weekdaysMask)
    }

    @Test
    fun a_damaged_row_falls_back_to_safe_values() {
        // Nothing but the type: every setting falls back to the built-in value.
        assertEquals(SleepLengthRule(id = 1, minutes = 510),
            TimeRuleEntity(id = 1, type = TimeRuleType.SLEEP_LENGTH).toDomain())
        assertEquals(HalfStateRule(id = 2, multiplier = 0.5),
            TimeRuleEntity(id = 2, type = TimeRuleType.HALF_STATE).toDomain())
        assertEquals(WakeUpTimeRule(id = 3, time = LocalTime.of(7, 0)),
            TimeRuleEntity(id = 3, type = TimeRuleType.WAKE_UP_TIME, timeA = "nonsense").toDomain())

        // A meal without a kind becomes lunch, with lunch's built-in times.
        val meal = TimeRuleEntity(id = 4, type = TimeRuleType.MEAL).toDomain() as MealRule
        assertEquals(MealKind.LUNCH, meal.meal)
        assertEquals(30, meal.minutes)
        assertEquals(LocalTime.of(12, 0), meal.earliestStart)

        // A productivity rule without a readable percentage has no effect.
        val productivity = TimeRuleEntity(
            id = 5, type = TimeRuleType.PRODUCTIVITY, timeA = "nonsense", timeB = "14:00"
        ).toDomain() as ProductivityRule
        assertEquals(1.0, productivity.multiplier, 1e-9)
        assertNull(productivity.from)
        assertEquals(LocalTime.of(14, 0), productivity.until)
    }
}