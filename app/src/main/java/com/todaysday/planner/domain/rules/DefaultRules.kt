package com.todaysday.planner.domain.rules

import java.time.LocalTime

/**
 * The built-in values. They are used in two ways:
 * 1. at the first start, the rules in `initial()` are created for you to see and change
 * 2. if you delete (or disable) a rule, the planner falls back to these values
 */
object DefaultRules {
    const val SLEEP_MINUTES = 510              // 8 h 30 min
    val WAKE_UP_TIME: LocalTime = LocalTime.of(7, 0)
    const val MORNING_ROUTINE_MINUTES = 45
    const val EVENING_ROUTINE_MINUTES = 45
    const val FREE_TIME_MINUTES = 120
    const val WASTE_MINUTES = 60
    const val HALF_STATE_MULTIPLIER = 0.5      // 50%

    /** The built-in rule for a meal. */
    fun meal(kind: MealKind): MealRule = when (kind) {
        MealKind.LUNCH -> MealRule(
            meal = MealKind.LUNCH, minutes = 30,
            earliestStart = LocalTime.of(12, 0), latestStart = LocalTime.of(14, 0)
        )
        MealKind.DINNER -> MealRule(
            meal = MealKind.DINNER, minutes = 45,
            earliestStart = LocalTime.of(17, 30), latestStart = LocalTime.of(19, 30)
        )
    }

    /** The rules created at the first start (id 0 means the database gives them real ids). */
    fun initial(): List<TimeRule> = listOf(
        SleepLengthRule(),
        WakeUpTimeRule(),
        MorningRoutineRule(),
        EveningRoutineRule(),
        meal(MealKind.LUNCH),
        meal(MealKind.DINNER),
        DailyFreeTimeRule(),
        DailyWasteRule(),
        HalfStateRule()
    )
}