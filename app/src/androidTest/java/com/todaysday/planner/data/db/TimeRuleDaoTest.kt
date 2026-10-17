package com.todaysday.planner.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.todaysday.planner.data.repository.SettingsRepository
import com.todaysday.planner.data.repository.TimeRuleRepository
import com.todaysday.planner.domain.rules.HalfStateRule
import com.todaysday.planner.domain.rules.MealKind
import com.todaysday.planner.domain.rules.MealRule
import com.todaysday.planner.domain.rules.ProductivityRule
import com.todaysday.planner.domain.rules.SleepLengthRule
import com.todaysday.planner.domain.settings.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Tests saving rules and settings on the phone, against a temporary in-memory database.
 * Your real app data is NOT touched: every test starts with an empty database.
 */
@RunWith(AndroidJUnit4::class)
class TimeRuleDaoTest {

    private lateinit var db: PlannerDatabase
    private lateinit var rules: TimeRuleRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = PlannerDatabase.createInMemory(context)
        rules = TimeRuleRepository(db.timeRuleDao())
        settings = SettingsRepository(db.settingDao())
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    @Test
    fun a_new_rule_is_saved_and_read_back() = runBlocking {
        val id = rules.save(SleepLengthRule(minutes = 400))

        assertEquals(listOf(SleepLengthRule(id = id, minutes = 400)), rules.observeRules().first())
    }

    @Test
    fun saving_a_rule_with_an_id_updates_it() = runBlocking {
        val id = rules.save(HalfStateRule(multiplier = 0.5))

        rules.save(HalfStateRule(id = id, multiplier = 0.7, customName = "Tired"))

        assertEquals(
            listOf(HalfStateRule(id = id, multiplier = 0.7, customName = "Tired")),
            rules.observeRules().first()
        )
    }

    @Test
    fun a_rule_can_be_switched_off_and_on() = runBlocking {
        val id = rules.save(ProductivityRule(multiplier = 0.5, weekdays = setOf(DayOfWeek.TUESDAY)))

        rules.setEnabled(id, false)
        assertFalse(rules.observeRules().first().single().enabled)

        rules.setEnabled(id, true)
        assertTrue(rules.observeRules().first().single().enabled)
    }

    @Test
    fun a_rule_can_be_deleted() = runBlocking {
        val keep = rules.save(SleepLengthRule())
        val remove = rules.save(HalfStateRule())

        rules.delete(remove)

        assertEquals(listOf(keep), rules.observeRules().first().map { it.id })
    }

    @Test
    fun the_engine_follows_the_saved_rules() = runBlocking {
        rules.save(SleepLengthRule(minutes = 420))
        rules.save(
            MealRule(
                meal = MealKind.LUNCH, minutes = 60,
                earliestStart = LocalTime.of(11, 30), latestStart = LocalTime.of(13, 0)
            )
        )

        val engine = rules.observeEngine().first()

        assertEquals(420, engine.sleepLengthMinutes())
        assertEquals(60, engine.meal(MealKind.LUNCH).minutes)
    }

    @Test
    fun the_default_rules_are_created_only_the_first_time() = runBlocking {
        assertTrue(rules.seedDefaultsIfNeeded())
        assertEquals(9, rules.observeRules().first().size)

        // A second start does nothing.
        assertFalse(rules.seedDefaultsIfNeeded())
        assertEquals(9, rules.observeRules().first().size)
    }

    @Test
    fun deleted_default_rules_stay_deleted() = runBlocking {
        rules.seedDefaultsIfNeeded()
        rules.observeRules().first().forEach { rules.delete(it.id) }

        assertFalse(rules.seedDefaultsIfNeeded())
        assertEquals(0, rules.observeRules().first().size)
    }

    @Test
    fun settings_start_as_the_defaults_and_can_be_changed() = runBlocking {
        assertEquals(AppSettings(), settings.observeSettings().first())

        val changed = AppSettings(
            minSessionMinutes = 10, maxSessionMinutes = 90, dayRollover = LocalTime.of(5, 0)
        )
        settings.update(changed)

        assertEquals(changed, settings.observeSettings().first())
    }

    @Test
    fun senseless_settings_are_fixed_when_saved() = runBlocking {
        settings.update(AppSettings(minSessionMinutes = 60, maxSessionMinutes = 30))

        val saved = settings.observeSettings().first()
        assertEquals(60, saved.minSessionMinutes)
        assertEquals(60, saved.maxSessionMinutes)
    }

    @Test
    fun the_seeding_flag_does_not_disturb_the_settings() = runBlocking {
        rules.seedDefaultsIfNeeded()

        assertEquals(AppSettings(), settings.observeSettings().first())
    }
}