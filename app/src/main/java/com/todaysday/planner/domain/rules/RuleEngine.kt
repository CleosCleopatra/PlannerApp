package com.todaysday.planner.domain.rules

import com.todaysday.planner.domain.model.Level
import com.todaysday.planner.domain.model.ProductivityState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Is [time] inside the window from [from] up to (not including) [until]?
 * - a window needs both ends. If one is missing, or both are equal, it means "the whole day"
 * - if [from] is later than [until] the window continues past midnight (22:00 to 02:00)
 */
fun timeInWindow(time: LocalTime, from: LocalTime?, until: LocalTime?): Boolean {
    if (from == null || until == null || from == until) return true
    return if (from.isBefore(until)) {
        !time.isBefore(from) && time.isBefore(until)
    } else {
        !time.isBefore(from) || time.isBefore(until)
    }
}

/**
 * Answers questions about your days, based on the time rules. Everything else in the app
 * (the scheduler, the planning screen) asks the RuleEngine and never reads the rules directly.
 *
 * Which rules count:
 * - Disabled rules are ignored, as if they did not exist.
 * - For settings with one value (sleep length, routines, meals, free time, wasted time,
 *   half state, wake-up time) the first enabled rule is used. If there is none, the built-in
 *   value from DefaultRules is used.
 * - Productivity rules can be many; all that fit a moment are multiplied together.
 *
 * It is plain Kotlin with no clock inside: the date and time are always passed in.
 */
class RuleEngine(rules: List<TimeRule>) {

    // Only enabled rules, lowest id first, so "the first one" is the same every time.
    private val active: List<TimeRule> = rules.filter { it.enabled }.sortedBy { it.id }

    private inline fun <reified T : TimeRule> firstOf(): T? =
        active.filterIsInstance<T>().firstOrNull()

    // ---------- Single settings ----------

    fun sleepLengthMinutes(): Int =
        (firstOf<SleepLengthRule>()?.minutes ?: DefaultRules.SLEEP_MINUTES).coerceAtLeast(0)

    fun defaultWakeUpTime(): LocalTime =
        firstOf<WakeUpTimeRule>()?.time ?: DefaultRules.WAKE_UP_TIME

    fun morningRoutineMinutes(): Int =
        (firstOf<MorningRoutineRule>()?.minutes ?: DefaultRules.MORNING_ROUTINE_MINUTES).coerceAtLeast(0)

    fun eveningRoutineMinutes(): Int =
        (firstOf<EveningRoutineRule>()?.minutes ?: DefaultRules.EVENING_ROUTINE_MINUTES).coerceAtLeast(0)

    fun freeTimeMinutes(): Int =
        (firstOf<DailyFreeTimeRule>()?.minutes ?: DefaultRules.FREE_TIME_MINUTES).coerceAtLeast(0)

    fun wasteMinutes(): Int =
        (firstOf<DailyWasteRule>()?.minutes ?: DefaultRules.WASTE_MINUTES).coerceAtLeast(0)

    /** The rule for a meal: the user's own, or the built-in one. */
    fun meal(kind: MealKind): MealRule =
        active.filterIsInstance<MealRule>().firstOrNull { it.meal == kind }
            ?: DefaultRules.meal(kind)

    /** How productive the "Half" state is, as a number: 0.5 means 50%. */
    fun halfStateMultiplier(): Double =
        (firstOf<HalfStateRule>()?.multiplier ?: DefaultRules.HALF_STATE_MULTIPLIER)
            .coerceIn(MIN_PRODUCTIVITY, MAX_PRODUCTIVITY)

    // ---------- Productivity ----------

    /**
     * How productive you are at [dateTime], as a number: 1.0 is normal, 0.5 means that
     * one hour of work gets half an hour of work done.
     *
     * - OFF gives 0.0 (nothing gets done)
     * - HALF multiplies by the half-state value
     * - every productivity rule that fits (weekday, time window, effort) is multiplied in
     * The result is kept between 5% and 200%.
     * The weekday is the weekday of [dateTime] itself, even for a window that crosses midnight.
     */
    fun productivityAt(dateTime: LocalDateTime, state: ProductivityState, effort: Level): Double {
        if (state == ProductivityState.OFF) return 0.0

        var productivity = 1.0
        if (state == ProductivityState.HALF) productivity *= halfStateMultiplier()

        for (rule in active.filterIsInstance<ProductivityRule>()) {
            val fitsEffort = rule.onlyForEffort == null || rule.onlyForEffort == effort
            if (fitsEffort &&
                weekdayFits(rule.weekdays, dateTime) &&
                timeInWindow(dateTime.toLocalTime(), rule.from, rule.until)
            ) {
                productivity *= rule.multiplier
            }
        }
        return productivity.coerceIn(MIN_PRODUCTIVITY, MAX_PRODUCTIVITY)
    }

    /**
     * Should entries with LOW effort come first at [dateTime]? True in the Half state, and
     * inside a "prefer low-effort entries" window.
     */
    fun prefersLowEffort(dateTime: LocalDateTime, state: ProductivityState): Boolean {
        if (state == ProductivityState.HALF) return true
        return active.filterIsInstance<LowProductivityWindowRule>().any { rule ->
            weekdayFits(rule.weekdays, dateTime) &&
                    timeInWindow(dateTime.toLocalTime(), rule.from, rule.until)
        }
    }

    private fun weekdayFits(weekdays: Set<DayOfWeek>, dateTime: LocalDateTime): Boolean =
        weekdays.isEmpty() || dateTime.dayOfWeek in weekdays

    // ---------- The shape of a day ----------

    /**
     * The fixed shape of [date].
     *
     * @param wakeUp when the day really starts. After "Going to bed" this is the moment you
     *               pressed it plus the sleep length. null = the default wake-up time.
     *
     * Bedtime is the default wake-up time of the NEXT day minus the sleep length. With the
     * built-in values: wake up 07:00, bedtime 22:30.
     */
    fun dayLayout(date: LocalDate, wakeUp: LocalDateTime? = null): DayLayout {
        val wake = wakeUp ?: date.atTime(defaultWakeUpTime())
        val nextDefaultWake = date.plusDays(1).atTime(defaultWakeUpTime())
        val bedTime = nextDefaultWake.minusMinutes(sleepLengthMinutes().toLong())

        val meals = MealKind.entries.map { kind ->
            val rule = meal(kind)
            val earliest = date.atTime(rule.earliestStart)
            val latest = date.atTime(rule.latestStart)
            MealSlot(
                kind = kind,
                minutes = rule.minutes.coerceAtLeast(0),
                earliestStart = earliest,
                latestStart = if (latest.isBefore(earliest)) earliest else latest
            )
        }

        return DayLayout(
            date = date,
            wakeUp = wake,
            morningRoutineMinutes = morningRoutineMinutes(),
            bedTime = bedTime,
            eveningRoutineMinutes = eveningRoutineMinutes(),
            meals = meals,
            freeTimeMinutes = freeTimeMinutes(),
            wasteMinutes = wasteMinutes()
        )
    }

    companion object {
        const val MIN_PRODUCTIVITY = 0.05   // 5%: never zero, so "time needed" stays finite
        const val MAX_PRODUCTIVITY = 2.0    // 200%
    }
}