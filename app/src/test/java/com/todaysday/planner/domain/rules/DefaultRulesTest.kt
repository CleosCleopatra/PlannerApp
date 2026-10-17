package com.todaysday.planner.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DefaultRulesTest {

    @Test
    fun the_first_start_creates_each_basic_rule_once() {
        val rules = DefaultRules.initial()

        assertEquals(9, rules.size)
        assertEquals(1, rules.filterIsInstance<SleepLengthRule>().size)
        assertEquals(1, rules.filterIsInstance<WakeUpTimeRule>().size)
        assertEquals(1, rules.filterIsInstance<MorningRoutineRule>().size)
        assertEquals(1, rules.filterIsInstance<EveningRoutineRule>().size)
        assertEquals(1, rules.filterIsInstance<DailyFreeTimeRule>().size)
        assertEquals(1, rules.filterIsInstance<DailyWasteRule>().size)
        assertEquals(1, rules.filterIsInstance<HalfStateRule>().size)
        assertEquals(setOf(MealKind.LUNCH, MealKind.DINNER), rules.filterIsInstance<MealRule>().map { it.meal }.toSet())
        // They are new: no id yet, and switched on.
        assertTrue(rules.all { it.id == 0L && it.enabled && it.customName == null })
    }

    @Test
    fun the_built_in_values() {
        val rules = DefaultRules.initial()

        assertEquals(510, rules.filterIsInstance<SleepLengthRule>().single().minutes)
        assertEquals(LocalTime.of(7, 0), rules.filterIsInstance<WakeUpTimeRule>().single().time)
        assertEquals(0.5, rules.filterIsInstance<HalfStateRule>().single().multiplier, 1e-9)
    }

    @Test
    fun every_rule_has_a_readable_name() {
        val everyKind = DefaultRules.initial() + ProductivityRule() + LowProductivityWindowRule()

        for (rule in everyKind) {
            assertTrue(RuleNamer.displayName(rule).isNotBlank())
        }
    }

    @Test
    fun changing_the_base_fields_keeps_everything_else() {
        val everyKind = DefaultRules.initial() + ProductivityRule() + LowProductivityWindowRule()

        for (rule in everyKind) {
            val changed = rule.withBase(id = 7, enabled = false, customName = "Mine")

            assertEquals(7L, changed.id)
            assertEquals(false, changed.enabled)
            assertEquals("Mine", changed.customName)
            // Changing the three fields back must give exactly the original rule again.
            assertEquals(rule, changed.withBase(id = rule.id, enabled = rule.enabled, customName = rule.customName))
        }
    }
}