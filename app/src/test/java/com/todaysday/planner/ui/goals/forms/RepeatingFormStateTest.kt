package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** Checks the rules of the repeating form. (5 October 2026 is a Monday.) */
class RepeatingFormStateTest {

    private val now = Instant.ofEpochSecond(100)
    private val monday = LocalDate.of(2026, 10, 5)

    // A valid form: Mondays and Wednesdays, starting on Monday 5 October.
    private fun validForm() = RepeatingFormState(
        name = "Study 5 min Serbian",
        repeatType = RepeatType.WEEKDAYS,
        weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        startDate = monday,
        isLoaded = true
    )

    @Test
    fun name_is_required() {
        assertNull(validForm().errors().name)
        assertNotNull(validForm().copy(name = "  ").errors().name)
    }

    @Test
    fun weekday_patterns_need_at_least_one_day() {
        val noDays = validForm().copy(weekdays = emptySet())

        assertNotNull(noDays.errors().days)
        assertNotNull(noDays.copy(repeatType = RepeatType.WEEKDAYS_EVERY_N_WEEKS).errors().days)
        // "Every N days" doesn't use weekdays at all.
        assertNull(noDays.copy(repeatType = RepeatType.EVERY_N_DAYS).errors().days)
    }

    @Test
    fun interval_patterns_need_one_or_more() {
        val zero = validForm().copy(repeatType = RepeatType.EVERY_N_DAYS, everyN = 0)

        assertNotNull(zero.errors().interval)
        assertNotNull(zero.copy(everyN = null).errors().interval)
        assertNotNull(zero.copy(repeatType = RepeatType.WEEKDAYS_EVERY_N_WEEKS).errors().interval)
        assertNull(zero.copy(everyN = 2).errors().interval)
        // The plain weekday pattern doesn't use the number.
        assertNull(zero.copy(repeatType = RepeatType.WEEKDAYS).errors().interval)
    }

    @Test
    fun each_time_must_take_some_time() {
        assertNotNull(validForm().copy(sessionMinutes = 0).errors().time)
        assertNotNull(validForm().copy(sessionMinutes = null).errors().time)
        assertNull(validForm().copy(sessionMinutes = 5).errors().time)
    }

    @Test
    fun invalid_form_gives_no_entry() {
        assertNull(validForm().copy(name = "").toRepeating(now))
    }

    @Test
    fun new_form_starts_today_with_five_minutes() {
        val form = RepeatingFormState.forNew(parentId = null, parentName = null, today = monday)

        assertEquals(monday, form.startDate)
        assertEquals(5, form.sessionMinutes)
        assertEquals(RepeatType.WEEKDAYS, form.repeatType)
        assertEquals(1, form.everyN)
        assertTrue(form.countsTowardParent)
        assertTrue(form.isLoaded)
    }

    @Test
    fun valid_form_builds_an_entry() {
        val entry = validForm().copy(
            name = " Study ", description = " daily ", sessionMinutes = 10,
            tagIds = setOf(1L), placeId = 2, countsTowardParent = false
        ).toRepeating(now)!!

        assertEquals("Study", entry.name)
        assertEquals("daily", entry.description)
        assertEquals(RepeatType.WEEKDAYS, entry.repeatType)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), entry.repeatWeekdays)
        assertEquals(monday, entry.repeatAnchorDate)
        assertEquals(10, entry.sessionMinutes)
        assertEquals(setOf(1L), entry.tagIds)
        assertEquals(2L, entry.placeId)
        assertEquals(false, entry.countsTowardParent)
        assertEquals(now, entry.createdAt)
    }

    @Test
    fun fields_the_pattern_does_not_use_are_cleared_when_saving() {
        // Every 3 days: the weekdays that were ticked earlier are dropped.
        val everyThreeDays = validForm().copy(repeatType = RepeatType.EVERY_N_DAYS, everyN = 3)
            .toRepeating(now)!!
        assertEquals(emptySet<DayOfWeek>(), everyThreeDays.repeatWeekdays)
        assertEquals(3, everyThreeDays.repeatN)

        // Plain weekdays: the number is reset to 1.
        val weekdays = validForm().copy(everyN = 5).toRepeating(now)!!
        assertEquals(1, weekdays.repeatN)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), weekdays.repeatWeekdays)
    }

    @Test
    fun every_n_weeks_keeps_both_the_days_and_the_number() {
        val entry = validForm().copy(repeatType = RepeatType.WEEKDAYS_EVERY_N_WEEKS, everyN = 2)
            .toRepeating(now)!!

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), entry.repeatWeekdays)
        assertEquals(2, entry.repeatN)
    }

    @Test
    fun editing_keeps_what_the_form_does_not_show() {
        val original = Repeating(
            id = 5, name = "Old", parentId = 9, createdAt = Instant.ofEpochSecond(7),
            repeatType = RepeatType.WEEKDAYS, repeatWeekdays = setOf(DayOfWeek.FRIDAY),
            repeatAnchorDate = monday
        )
        val saved = RepeatingFormState.forRepeating(original, "Parent")
            .copy(name = " New ")
            .toRepeating(now)!!

        assertEquals(5L, saved.id)
        assertEquals("New", saved.name)
        assertEquals(9L, saved.parentId)
        assertEquals(Instant.ofEpochSecond(7), saved.createdAt)
        assertEquals(setOf(DayOfWeek.FRIDAY), saved.repeatWeekdays)
    }

    @Test
    fun stopping_sets_the_finish_time_and_restarting_clears_it() {
        val stopped = validForm().copy(isDone = true).toRepeating(now)!!
        assertEquals(now, stopped.completedAt)

        val finished = Repeating(
            id = 1, name = "x", repeatType = RepeatType.WEEKDAYS,
            repeatWeekdays = setOf(DayOfWeek.MONDAY), completedAt = Instant.ofEpochSecond(50)
        )
        val unchanged = RepeatingFormState.forRepeating(finished, null).toRepeating(now)!!
        assertEquals(Instant.ofEpochSecond(50), unchanged.completedAt)

        val restarted = RepeatingFormState.forRepeating(finished, null).copy(isDone = false).toRepeating(now)!!
        assertNull(restarted.completedAt)
    }

    @Test
    fun preview_lists_the_next_dates_of_the_pattern() {
        val wednesday = LocalDate.of(2026, 10, 7)

        assertEquals(
            listOf(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14)),
            validForm().upcomingDates(today = wednesday)
        )
    }

    @Test
    fun preview_works_for_every_n_days() {
        val form = validForm().copy(
            repeatType = RepeatType.EVERY_N_DAYS, everyN = 2, startDate = LocalDate.of(2026, 10, 1)
        )

        assertEquals(
            listOf(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7)),
            form.upcomingDates(today = LocalDate.of(2026, 10, 2))
        )
    }

    @Test
    fun preview_is_empty_while_the_pattern_is_incomplete() {
        assertEquals(emptyList<LocalDate>(), validForm().copy(weekdays = emptySet()).upcomingDates(monday))
    }

    @Test
    fun loading_an_entry_fills_in_the_form() {
        val entry = Repeating(
            id = 3, name = "Walk", repeatType = RepeatType.WEEKDAYS_EVERY_N_WEEKS,
            repeatWeekdays = setOf(DayOfWeek.SATURDAY), repeatN = 2,
            repeatAnchorDate = monday, sessionMinutes = 30, tagIds = setOf(4L), placeId = 6
        )
        val form = RepeatingFormState.forRepeating(entry, "Health")

        assertEquals(RepeatType.WEEKDAYS_EVERY_N_WEEKS, form.repeatType)
        assertEquals(setOf(DayOfWeek.SATURDAY), form.weekdays)
        assertEquals(2, form.everyN)
        assertEquals(30, form.sessionMinutes)
        assertEquals(setOf(4L), form.tagIds)
        assertEquals(6L, form.placeId)
        assertEquals("Health", form.parentName)
    }
}