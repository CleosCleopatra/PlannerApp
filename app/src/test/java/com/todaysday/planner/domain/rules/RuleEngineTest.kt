package com.todaysday.planner.domain.rules

import com.todaysday.planner.domain.model.Level
import com.todaysday.planner.domain.model.ProductivityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Checks the questions the RuleEngine answers. (13 October 2026 is a Tuesday.) */
class RuleEngineTest {

    private val tuesday10 = LocalDateTime.of(2026, 10, 13, 10, 0)
    private val wednesday10 = LocalDateTime.of(2026, 10, 14, 10, 0)

    private fun engine(vararg rules: TimeRule) = RuleEngine(rules.toList())

    private fun RuleEngine.normalAt(time: LocalDateTime) =
        productivityAt(time, ProductivityState.FULL, Level.NORMAL)

    @Test
    fun productivity_is_normal_without_any_rules() {
        assertEquals(1.0, engine().normalAt(tuesday10), 1e-9)
    }

    @Test
    fun weekday_rule_only_applies_on_that_weekday() {
        val engine = engine(ProductivityRule(multiplier = 0.5, weekdays = setOf(DayOfWeek.TUESDAY)))

        assertEquals(0.5, engine.normalAt(tuesday10), 1e-9)
        assertEquals(1.0, engine.normalAt(wednesday10), 1e-9)
        // At 50% productivity one hour of normal work takes two hours.
        assertEquals(120.0, 60 / engine.normalAt(tuesday10), 1e-9)
    }

    @Test
    fun time_window_includes_the_start_and_excludes_the_end() {
        val engine = engine(
            ProductivityRule(multiplier = 0.7, from = LocalTime.of(14, 0), until = LocalTime.of(16, 0))
        )
        fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 10, 13, hour, minute)

        assertEquals(0.7, engine.normalAt(at(14, 0)), 1e-9)
        assertEquals(0.7, engine.normalAt(at(15, 59)), 1e-9)
        assertEquals(1.0, engine.normalAt(at(16, 0)), 1e-9)
        assertEquals(1.0, engine.normalAt(at(13, 59)), 1e-9)
    }

    @Test
    fun window_can_continue_past_midnight() {
        val engine = engine(
            ProductivityRule(multiplier = 0.6, from = LocalTime.of(22, 0), until = LocalTime.of(2, 0))
        )
        fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 10, 13, hour, minute)

        assertEquals(0.6, engine.normalAt(at(23, 30)), 1e-9)
        assertEquals(0.6, engine.normalAt(at(1, 0)), 1e-9)
        assertEquals(1.0, engine.normalAt(at(12, 0)), 1e-9)
        assertEquals(1.0, engine.normalAt(at(2, 0)), 1e-9)
    }

    @Test
    fun overlapping_rules_are_multiplied() {
        val engine = engine(
            ProductivityRule(multiplier = 0.5, weekdays = setOf(DayOfWeek.TUESDAY)),
            ProductivityRule(multiplier = 0.8)
        )

        assertEquals(0.4, engine.normalAt(tuesday10), 1e-9)
        assertEquals(0.8, engine.normalAt(wednesday10), 1e-9)
    }

    @Test
    fun effort_rule_only_applies_to_that_effort() {
        val engine = engine(ProductivityRule(multiplier = 0.5, onlyForEffort = Level.HIGH))

        assertEquals(0.5, engine.productivityAt(tuesday10, ProductivityState.FULL, Level.HIGH), 1e-9)
        assertEquals(1.0, engine.productivityAt(tuesday10, ProductivityState.FULL, Level.LOW), 1e-9)
        assertEquals(1.0, engine.productivityAt(tuesday10, ProductivityState.FULL, Level.NORMAL), 1e-9)
    }

    @Test
    fun half_state_uses_the_built_in_value_and_can_be_changed() {
        assertEquals(0.5, engine().productivityAt(tuesday10, ProductivityState.HALF, Level.NORMAL), 1e-9)

        val custom = engine(HalfStateRule(multiplier = 0.7))
        assertEquals(0.7, custom.productivityAt(tuesday10, ProductivityState.HALF, Level.NORMAL), 1e-9)
        assertEquals(1.0, custom.productivityAt(tuesday10, ProductivityState.FULL, Level.NORMAL), 1e-9)
    }

    @Test
    fun half_state_and_rules_combine() {
        val engine = engine(ProductivityRule(multiplier = 0.5, weekdays = setOf(DayOfWeek.TUESDAY)))

        assertEquals(0.25, engine.productivityAt(tuesday10, ProductivityState.HALF, Level.NORMAL), 1e-9)
    }

    @Test
    fun off_state_means_nothing_gets_done() {
        val engine = engine(ProductivityRule(multiplier = 1.5))

        assertEquals(0.0, engine.productivityAt(tuesday10, ProductivityState.OFF, Level.NORMAL), 1e-9)
    }

    @Test
    fun disabled_rules_are_ignored() {
        val disabled = ProductivityRule(multiplier = 0.5, enabled = false)

        assertEquals(1.0, engine(disabled).normalAt(tuesday10), 1e-9)
        assertEquals(1.0, engine(disabled.withBase(enabled = false)).normalAt(tuesday10), 1e-9)
        assertEquals(0.5, engine(disabled.withBase(enabled = true)).normalAt(tuesday10), 1e-9)
    }

    @Test
    fun productivity_is_kept_within_limits() {
        assertEquals(RuleEngine.MIN_PRODUCTIVITY, engine(ProductivityRule(multiplier = 0.0)).normalAt(tuesday10), 1e-9)
        assertEquals(RuleEngine.MAX_PRODUCTIVITY, engine(ProductivityRule(multiplier = 10.0)).normalAt(tuesday10), 1e-9)
        assertEquals(RuleEngine.MIN_PRODUCTIVITY, engine(HalfStateRule(multiplier = 0.0))
            .productivityAt(tuesday10, ProductivityState.HALF, Level.NORMAL), 1e-9)
    }

    @Test
    fun missing_rules_fall_back_to_the_built_in_values() {
        val engine = engine()

        assertEquals(510, engine.sleepLengthMinutes())
        assertEquals(LocalTime.of(7, 0), engine.defaultWakeUpTime())
        assertEquals(45, engine.morningRoutineMinutes())
        assertEquals(45, engine.eveningRoutineMinutes())
        assertEquals(120, engine.freeTimeMinutes())
        assertEquals(60, engine.wasteMinutes())
        assertEquals(0.5, engine.halfStateMultiplier(), 1e-9)
    }

    @Test
    fun rules_change_the_settings() {
        val engine = engine(
            SleepLengthRule(minutes = 420),
            WakeUpTimeRule(time = LocalTime.of(6, 30)),
            MorningRoutineRule(minutes = 20),
            EveningRoutineRule(minutes = 30),
            DailyFreeTimeRule(minutes = 90),
            DailyWasteRule(minutes = 15)
        )

        assertEquals(420, engine.sleepLengthMinutes())
        assertEquals(LocalTime.of(6, 30), engine.defaultWakeUpTime())
        assertEquals(20, engine.morningRoutineMinutes())
        assertEquals(30, engine.eveningRoutineMinutes())
        assertEquals(90, engine.freeTimeMinutes())
        assertEquals(15, engine.wasteMinutes())
    }

    @Test
    fun lowest_id_wins_when_there_are_several_of_a_single_kind() {
        val engine = engine(
            SleepLengthRule(id = 5, minutes = 480),
            SleepLengthRule(id = 2, minutes = 400)
        )

        assertEquals(400, engine.sleepLengthMinutes())
    }

    @Test
    fun low_effort_is_preferred_in_the_half_state_and_in_windows() {
        val engine = engine(
            LowProductivityWindowRule(
                weekdays = setOf(DayOfWeek.TUESDAY),
                from = LocalTime.of(14, 0), until = LocalTime.of(16, 0)
            )
        )
        val tuesday15 = LocalDateTime.of(2026, 10, 13, 15, 0)
        val wednesday15 = LocalDateTime.of(2026, 10, 14, 15, 0)

        assertTrue(engine.prefersLowEffort(tuesday15, ProductivityState.FULL))
        assertFalse(engine.prefersLowEffort(tuesday10, ProductivityState.FULL))
        assertFalse(engine.prefersLowEffort(wednesday15, ProductivityState.FULL))
        assertTrue(engine.prefersLowEffort(wednesday15, ProductivityState.HALF))
        assertFalse(engine().prefersLowEffort(tuesday15, ProductivityState.FULL))
    }

    @Test
    fun a_slow_productivity_rule_alone_does_not_prefer_low_effort() {
        val engine = engine(ProductivityRule(multiplier = 0.5))

        assertFalse(engine.prefersLowEffort(tuesday10, ProductivityState.FULL))
    }

    @Test
    fun meals_use_their_own_rule_or_the_built_in_one() {
        val engine = engine(
            MealRule(
                meal = MealKind.LUNCH, minutes = 60,
                earliestStart = LocalTime.of(11, 30), latestStart = LocalTime.of(13, 0)
            )
        )

        assertEquals(60, engine.meal(MealKind.LUNCH).minutes)
        assertEquals(LocalTime.of(11, 30), engine.meal(MealKind.LUNCH).earliestStart)
        // Dinner has no rule, so the built-in one is used.
        assertEquals(45, engine.meal(MealKind.DINNER).minutes)
    }
}