package com.todaysday.planner.domain.rules

import com.todaysday.planner.domain.model.Level
import java.time.DayOfWeek
import java.time.LocalTime

/** How long you sleep. The day after "Going to bed" starts this many minutes later. */
data class SleepLengthRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val minutes: Int = DefaultRules.SLEEP_MINUTES
) : TimeRule

/** When the day starts if you did not press "Going to bed" the evening before. */
data class WakeUpTimeRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val time: LocalTime = DefaultRules.WAKE_UP_TIME
) : TimeRule

/** Time to get ready in the morning, right after waking up. */
data class MorningRoutineRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val minutes: Int = DefaultRules.MORNING_ROUTINE_MINUTES
) : TimeRule

/** Time to get ready for the night, right before bedtime. */
data class EveningRoutineRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val minutes: Int = DefaultRules.EVENING_ROUTINE_MINUTES
) : TimeRule

/**
 * One meal: how long it takes and between which times it may START.
 * (A meal of 0 minutes is skipped.)
 */
data class MealRule(
    val meal: MealKind,
    val minutes: Int,
    val earliestStart: LocalTime,
    val latestStart: LocalTime,
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null
) : TimeRule

/** Free time every day, preferably at the end of the day. */
data class DailyFreeTimeRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val minutes: Int = DefaultRules.FREE_TIME_MINUTES
) : TimeRule

/** Time that is lost every day without getting anything done. */
data class DailyWasteRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val minutes: Int = DefaultRules.WASTE_MINUTES
) : TimeRule

/** How productive you are in the "Half" state, as a number: 0.5 means 50%. */
data class HalfStateRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val multiplier: Double = DefaultRules.HALF_STATE_MULTIPLIER
) : TimeRule

/**
 * How productive you are at certain times, as a number: 0.5 means 50%, 1.2 means 120%.
 *
 * It applies when ALL of these fit:
 * - weekdays:      empty = every day
 * - from / until:  both empty = the whole day. If "from" is later than "until" the window
 *                  continues past midnight (22:00 to 02:00). The end itself is not included.
 * - onlyForEffort: null = all entries, otherwise only entries with this effort
 * Several rules that fit at the same moment are multiplied.
 */
data class ProductivityRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val multiplier: Double = 0.5,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val from: LocalTime? = null,
    val until: LocalTime? = null,
    val onlyForEffort: Level? = null
) : TimeRule

/**
 * A time window where entries with LOW effort should come first, for example the
 * afternoon slump. Same rules for weekdays and for the window as in ProductivityRule.
 */
data class LowProductivityWindowRule(
    override val id: Long = 0,
    override val enabled: Boolean = true,
    override val customName: String? = null,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val from: LocalTime = LocalTime.of(14, 0),
    val until: LocalTime = LocalTime.of(16, 0)
) : TimeRule

/**
 * Changes the three things every rule has (id, enabled, customName) without needing to
 * know which kind of rule it is. Only the values you pass are changed.
 */
fun TimeRule.withBase(
    id: Long = this.id,
    enabled: Boolean = this.enabled,
    customName: String? = this.customName
): TimeRule = when (this) {
    is SleepLengthRule -> copy(id = id, enabled = enabled, customName = customName)
    is WakeUpTimeRule -> copy(id = id, enabled = enabled, customName = customName)
    is MorningRoutineRule -> copy(id = id, enabled = enabled, customName = customName)
    is EveningRoutineRule -> copy(id = id, enabled = enabled, customName = customName)
    is MealRule -> copy(id = id, enabled = enabled, customName = customName)
    is DailyFreeTimeRule -> copy(id = id, enabled = enabled, customName = customName)
    is DailyWasteRule -> copy(id = id, enabled = enabled, customName = customName)
    is HalfStateRule -> copy(id = id, enabled = enabled, customName = customName)
    is ProductivityRule -> copy(id = id, enabled = enabled, customName = customName)
    is LowProductivityWindowRule -> copy(id = id, enabled = enabled, customName = customName)
}