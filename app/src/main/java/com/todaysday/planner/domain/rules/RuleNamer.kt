package com.todaysday.planner.domain.rules

import com.todaysday.planner.domain.progress.formatMinutes
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Gives every rule a name that clearly says what it does, for example
 * "Tuesdays, 14:00–16:00: 70% productivity". The name is made from the settings, so it
 * always matches them. A name typed by the user wins.
 */
object RuleNamer {

    /** The name to show in the list: the user's own name, or the generated one. */
    fun displayName(rule: TimeRule): String =
        rule.customName?.takeIf { it.isNotBlank() } ?: generate(rule)

    /** The name made from the settings only. */
    fun generate(rule: TimeRule): String = when (rule) {
        is SleepLengthRule -> "Sleep length: ${formatMinutes(rule.minutes)}"
        is WakeUpTimeRule -> "Default wake-up time: ${rule.time}"
        is MorningRoutineRule -> "Morning routine: ${formatMinutes(rule.minutes)}"
        is EveningRoutineRule -> "Evening routine: ${formatMinutes(rule.minutes)}"
        is MealRule -> {
            val name = if (rule.meal == MealKind.LUNCH) "Lunch" else "Dinner"
            "$name: ${formatMinutes(rule.minutes)}, starts between ${rule.earliestStart} and ${rule.latestStart}"
        }
        is DailyFreeTimeRule -> "Free time: ${formatMinutes(rule.minutes)} per day"
        is DailyWasteRule -> "Wasted time: ${formatMinutes(rule.minutes)} per day"
        is HalfStateRule -> "Half state: ${percent(rule.multiplier)}% productivity"
        is ProductivityRule -> {
            val effort = rule.onlyForEffort
                ?.let { " (${it.name.lowercase()}-effort entries only)" }
                ?: ""
            "${whenText(rule.weekdays, rule.from, rule.until)}: ${percent(rule.multiplier)}% productivity$effort"
        }
        is LowProductivityWindowRule ->
            "Prefer low-effort entries: ${whenText(rule.weekdays, rule.from, rule.until)}"
    }

    private fun percent(multiplier: Double): Int = (multiplier * 100).roundToInt()

    /** "Tuesdays", "Every day, 14:00–16:00", "Mon, Wed" ... */
    private fun whenText(weekdays: Set<DayOfWeek>, from: LocalTime?, until: LocalTime?): String {
        val days = daysText(weekdays)
        return if (from != null && until != null && from != until) "$days, $from–$until" else days
    }

    private fun daysText(weekdays: Set<DayOfWeek>): String {
        val weekdaysMondayToFriday = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
        return when {
            weekdays.isEmpty() || weekdays.size == 7 -> "Every day"
            weekdays == weekdaysMondayToFriday -> "Monday to Friday"
            weekdays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
            weekdays.size == 1 -> weekdays.first().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "s"
            else -> weekdays.sortedBy { it.value }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
        }
    }
}