package com.todaysday.planner.data.db

import com.todaysday.planner.data.db.entities.TimeRuleEntity
import com.todaysday.planner.data.db.entities.TimeRuleType
import com.todaysday.planner.domain.rules.DailyFreeTimeRule
import com.todaysday.planner.domain.rules.DailyWasteRule
import com.todaysday.planner.domain.rules.DefaultRules
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
import java.time.LocalTime

/*
 * Mappers between database rows (TimeRuleEntity: flat columns) and the rule classes
 * (SleepLengthRule, ProductivityRule, ...), like EntryMapper does for entries.
 * The weekday helpers toMask / toWeekdays live in EntryMapper.kt.
 */

// Reads a saved time like "14:30". Unreadable text gives null.
private fun String?.toTimeOrNull(): LocalTime? =
    this?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

// ---------- Database -> rule ----------
// A missing or unreadable column falls back to a safe value, so a damaged row can't crash the app.

fun TimeRuleEntity.toDomain(): TimeRule = when (type) {
    TimeRuleType.SLEEP_LENGTH -> SleepLengthRule(
        id = id, enabled = enabled, customName = customName,
        minutes = minutes ?: DefaultRules.SLEEP_MINUTES
    )

    TimeRuleType.WAKE_UP_TIME -> WakeUpTimeRule(
        id = id, enabled = enabled, customName = customName,
        time = timeA.toTimeOrNull() ?: DefaultRules.WAKE_UP_TIME
    )

    TimeRuleType.MORNING_ROUTINE -> MorningRoutineRule(
        id = id, enabled = enabled, customName = customName,
        minutes = minutes ?: DefaultRules.MORNING_ROUTINE_MINUTES
    )

    TimeRuleType.EVENING_ROUTINE -> EveningRoutineRule(
        id = id, enabled = enabled, customName = customName,
        minutes = minutes ?: DefaultRules.EVENING_ROUTINE_MINUTES
    )

    TimeRuleType.MEAL -> {
        val kind = meal ?: MealKind.LUNCH
        val fallback = DefaultRules.meal(kind)
        MealRule(
            id = id, enabled = enabled, customName = customName,
            meal = kind,
            minutes = minutes ?: fallback.minutes,
            earliestStart = timeA.toTimeOrNull() ?: fallback.earliestStart,
            latestStart = timeB.toTimeOrNull() ?: fallback.latestStart
        )
    }

    TimeRuleType.FREE_TIME -> DailyFreeTimeRule(
        id = id, enabled = enabled, customName = customName,
        minutes = minutes ?: DefaultRules.FREE_TIME_MINUTES
    )

    TimeRuleType.WASTE -> DailyWasteRule(
        id = id, enabled = enabled, customName = customName,
        minutes = minutes ?: DefaultRules.WASTE_MINUTES
    )

    TimeRuleType.HALF_STATE -> HalfStateRule(
        id = id, enabled = enabled, customName = customName,
        multiplier = multiplier ?: DefaultRules.HALF_STATE_MULTIPLIER
    )

    // A productivity rule without a readable percentage has no effect (100%).
    TimeRuleType.PRODUCTIVITY -> ProductivityRule(
        id = id, enabled = enabled, customName = customName,
        multiplier = multiplier ?: 1.0,
        weekdays = weekdaysMask.toWeekdays(),
        from = timeA.toTimeOrNull(),
        until = timeB.toTimeOrNull(),
        onlyForEffort = effort
    )

    TimeRuleType.LOW_EFFORT_WINDOW -> LowProductivityWindowRule(
        id = id, enabled = enabled, customName = customName,
        weekdays = weekdaysMask.toWeekdays(),
        from = timeA.toTimeOrNull() ?: LocalTime.of(14, 0),
        until = timeB.toTimeOrNull() ?: LocalTime.of(16, 0)
    )
}

// ---------- Rule -> database ----------

fun TimeRule.toEntity(): TimeRuleEntity = when (this) {
    is SleepLengthRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.SLEEP_LENGTH, enabled = enabled, customName = customName,
        minutes = minutes
    )

    is WakeUpTimeRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.WAKE_UP_TIME, enabled = enabled, customName = customName,
        timeA = time.toString()
    )

    is MorningRoutineRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.MORNING_ROUTINE, enabled = enabled, customName = customName,
        minutes = minutes
    )

    is EveningRoutineRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.EVENING_ROUTINE, enabled = enabled, customName = customName,
        minutes = minutes
    )

    is MealRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.MEAL, enabled = enabled, customName = customName,
        meal = meal, minutes = minutes,
        timeA = earliestStart.toString(), timeB = latestStart.toString()
    )

    is DailyFreeTimeRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.FREE_TIME, enabled = enabled, customName = customName,
        minutes = minutes
    )

    is DailyWasteRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.WASTE, enabled = enabled, customName = customName,
        minutes = minutes
    )

    is HalfStateRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.HALF_STATE, enabled = enabled, customName = customName,
        multiplier = multiplier
    )

    is ProductivityRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.PRODUCTIVITY, enabled = enabled, customName = customName,
        multiplier = multiplier, weekdaysMask = weekdays.toMask(),
        timeA = from?.toString(), timeB = until?.toString(), effort = onlyForEffort
    )

    is LowProductivityWindowRule -> TimeRuleEntity(
        id = id, type = TimeRuleType.LOW_EFFORT_WINDOW, enabled = enabled, customName = customName,
        weekdaysMask = weekdays.toMask(), timeA = from.toString(), timeB = until.toString()
    )
}