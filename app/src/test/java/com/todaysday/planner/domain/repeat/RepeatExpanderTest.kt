package com.todaysday.planner.domain.repeat

import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Checks the three repeat patterns, including month and year boundaries.
 * Handy to know: 5 October 2026 is a Monday.
 */
class RepeatExpanderTest {

    private fun repeating(
        type: RepeatType,
        start: LocalDate,
        days: Set<DayOfWeek> = emptySet(),
        n: Int = 1,
        done: Boolean = false
    ) = Repeating(
        name = "Study", repeatType = type, repeatWeekdays = days, repeatN = n,
        repeatAnchorDate = start, completedAt = if (done) Instant.EPOCH else null
    )

    private fun date(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    @Test
    fun weekdays_pattern_hits_the_chosen_days() {
        val entry = repeating(
            RepeatType.WEEKDAYS, start = date(2026, 10, 5),
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )

        val dates = RepeatExpander.datesBetween(entry, date(2026, 10, 5), date(2026, 10, 18))

        assertEquals(
            listOf(date(2026, 10, 5), date(2026, 10, 7), date(2026, 10, 12), date(2026, 10, 14)),
            dates
        )
    }

    @Test
    fun nothing_happens_before_the_start_date() {
        val entry = repeating(
            RepeatType.WEEKDAYS, start = date(2026, 10, 7), // a Wednesday
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )

        assertFalse(RepeatExpander.occursOn(entry, date(2026, 10, 5))) // the Monday before
        assertTrue(RepeatExpander.occursOn(entry, date(2026, 10, 7)))
    }

    @Test
    fun every_n_days_counts_from_the_start_date() {
        val entry = repeating(RepeatType.EVERY_N_DAYS, start = date(2026, 10, 1), n = 2)

        assertEquals(
            listOf(date(2026, 10, 1), date(2026, 10, 3), date(2026, 10, 5), date(2026, 10, 7)),
            RepeatExpander.datesBetween(entry, date(2026, 10, 1), date(2026, 10, 8))
        )
        assertFalse(RepeatExpander.occursOn(entry, date(2026, 10, 2)))
    }

    @Test
    fun every_n_days_works_across_a_month_boundary() {
        val entry = repeating(RepeatType.EVERY_N_DAYS, start = date(2026, 10, 29), n = 3)

        assertEquals(
            listOf(date(2026, 10, 29), date(2026, 11, 1), date(2026, 11, 4)),
            RepeatExpander.datesBetween(entry, date(2026, 10, 29), date(2026, 11, 5))
        )
    }

    @Test
    fun every_n_days_works_across_a_year_boundary() {
        val entry = repeating(RepeatType.EVERY_N_DAYS, start = date(2026, 12, 30), n = 2)

        assertEquals(
            listOf(date(2026, 12, 30), date(2027, 1, 1), date(2027, 1, 3)),
            RepeatExpander.datesBetween(entry, date(2026, 12, 30), date(2027, 1, 4))
        )
    }

    @Test
    fun weekdays_every_n_weeks_skips_the_weeks_in_between() {
        val entry = repeating(
            RepeatType.WEEKDAYS_EVERY_N_WEEKS, start = date(2026, 10, 7), // a Wednesday
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), n = 2
        )

        assertEquals(
            listOf(
                date(2026, 10, 7),                      // the first week (Monday 5th is before the start)
                date(2026, 10, 19), date(2026, 10, 21), // two weeks later
                date(2026, 11, 2), date(2026, 11, 4)    // two weeks after that
            ),
            RepeatExpander.datesBetween(entry, date(2026, 10, 5), date(2026, 11, 4))
        )
    }

    @Test
    fun weeks_start_on_monday() {
        // The start date is a Sunday: that Sunday belongs to the week that began on Monday 5 October.
        val entry = repeating(
            RepeatType.WEEKDAYS_EVERY_N_WEEKS, start = date(2026, 10, 11),
            days = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), n = 2
        )

        assertEquals(
            listOf(date(2026, 10, 11), date(2026, 10, 19), date(2026, 10, 25)),
            RepeatExpander.datesBetween(entry, date(2026, 10, 11), date(2026, 10, 26))
        )
    }

    @Test
    fun weeks_pattern_works_across_a_year_boundary() {
        val entry = repeating(
            RepeatType.WEEKDAYS_EVERY_N_WEEKS, start = date(2026, 12, 28), // a Monday
            days = setOf(DayOfWeek.MONDAY), n = 2
        )

        assertEquals(
            listOf(date(2026, 12, 28), date(2027, 1, 11), date(2027, 1, 25)),
            RepeatExpander.datesBetween(entry, date(2026, 12, 28), date(2027, 1, 31))
        )
    }

    @Test
    fun no_weekdays_means_it_never_happens() {
        val weekly = repeating(RepeatType.WEEKDAYS, start = date(2026, 10, 5))
        val everyOtherWeek = repeating(RepeatType.WEEKDAYS_EVERY_N_WEEKS, start = date(2026, 10, 5), n = 2)

        assertEquals(emptyList<LocalDate>(), RepeatExpander.datesBetween(weekly, date(2026, 10, 5), date(2026, 12, 31)))
        assertEquals(emptyList<LocalDate>(), RepeatExpander.datesBetween(everyOtherWeek, date(2026, 10, 5), date(2026, 12, 31)))
    }

    @Test
    fun a_number_below_one_counts_as_one() {
        val entry = repeating(RepeatType.EVERY_N_DAYS, start = date(2026, 10, 1), n = 0)

        assertEquals(3, RepeatExpander.datesBetween(entry, date(2026, 10, 1), date(2026, 10, 3)).size)
    }

    @Test
    fun finished_entry_has_no_dates() {
        val entry = repeating(
            RepeatType.WEEKDAYS, start = date(2026, 10, 5),
            days = setOf(DayOfWeek.MONDAY), done = true
        )

        assertFalse(RepeatExpander.occursOn(entry, date(2026, 10, 5)))
        assertNull(RepeatExpander.nextOccurrence(entry, date(2026, 10, 5)))
    }

    @Test
    fun next_occurrence_finds_the_next_matching_day() {
        val entry = repeating(
            RepeatType.WEEKDAYS, start = date(2026, 10, 5),
            days = setOf(DayOfWeek.FRIDAY)
        )

        // 16 October 2026 is a Friday: it counts when it is the day we start looking from.
        assertEquals(date(2026, 10, 16), RepeatExpander.nextOccurrence(entry, date(2026, 10, 16)))
        assertEquals(date(2026, 10, 23), RepeatExpander.nextOccurrence(entry, date(2026, 10, 17)))
    }

    @Test
    fun reversed_range_gives_no_dates() {
        val entry = repeating(RepeatType.EVERY_N_DAYS, start = date(2026, 10, 1), n = 1)

        assertEquals(emptyList<LocalDate>(), RepeatExpander.datesBetween(entry, date(2026, 10, 5), date(2026, 10, 1)))
    }
}