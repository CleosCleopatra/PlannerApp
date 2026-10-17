package com.todaysday.planner.domain.repeat

import com.todaysday.planner.domain.model.RepeatType
import com.todaysday.planner.domain.model.Repeating
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Turns a repeat pattern into actual dates: "does it happen on this day?".
 *
 * The three patterns:
 * - WEEKDAYS:               on the chosen weekdays, every week
 * - EVERY_N_DAYS:           every N days, counting from the start date
 * - WEEKDAYS_EVERY_N_WEEKS: on the chosen weekdays, but only every N-th week.
 *                           The week of the start date is week 0 (weeks start on Monday).
 *
 * Nothing happens before the start date (repeatAnchorDate), and a finished entry
 * has no more dates. A number below 1 counts as 1.
 *
 * Plain Kotlin and no database, so it is easy to test.
 */
object RepeatExpander {

    // How far into the future nextOccurrence looks before giving up.
    private const val SEARCH_DAYS = 2000L

    /** Does this repeating entry happen on [date]? */
    fun occursOn(entry: Repeating, date: LocalDate): Boolean {
        if (entry.completedAt != null) return false
        if (date.isBefore(entry.repeatAnchorDate)) return false

        val interval = entry.repeatN.coerceAtLeast(1)

        return when (entry.repeatType) {
            RepeatType.WEEKDAYS ->
                date.dayOfWeek in entry.repeatWeekdays

            RepeatType.EVERY_N_DAYS -> {
                val daysSinceStart = ChronoUnit.DAYS.between(entry.repeatAnchorDate, date)
                daysSinceStart % interval == 0L
            }

            RepeatType.WEEKDAYS_EVERY_N_WEEKS -> {
                if (date.dayOfWeek !in entry.repeatWeekdays) {
                    false
                } else {
                    val weeksSinceStart = ChronoUnit.WEEKS.between(
                        mondayOf(entry.repeatAnchorDate),
                        mondayOf(date)
                    )
                    weeksSinceStart % interval == 0L
                }
            }
        }
    }

    /** All the dates from [from] to [to] (both included) on which the entry happens. */
    fun datesBetween(entry: Repeating, from: LocalDate, to: LocalDate): List<LocalDate> {
        if (to.isBefore(from)) return emptyList()

        val dates = mutableListOf<LocalDate>()
        var day = from
        while (!day.isAfter(to)) {
            if (occursOn(entry, day)) dates += day
            day = day.plusDays(1)
        }
        return dates
    }

    /** The first date on or after [from] on which it happens, or null if there is none. */
    fun nextOccurrence(entry: Repeating, from: LocalDate): LocalDate? {
        for (offset in 0..SEARCH_DAYS) {
            val day = from.plusDays(offset)
            if (occursOn(entry, day)) return day
        }
        return null
    }

    // The Monday of the week that contains this date.
    private fun mondayOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}