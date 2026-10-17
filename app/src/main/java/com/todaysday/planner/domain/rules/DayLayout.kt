package com.todaysday.planner.domain.rules

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One meal in a day: how long it takes, and between which times it may start.
 * The scheduler decides the exact time later.
 */
data class MealSlot(
    val kind: MealKind,
    val minutes: Int,
    val earliestStart: LocalDateTime,
    val latestStart: LocalDateTime
)

/**
 * The fixed shape of one day, worked out from the time rules:
 * wake up -> morning routine -> (the day) -> evening routine -> bedtime.
 *
 * Meals, free time and wasted time are not placed at exact times here. The scheduler places
 * them later; here we only know how much time they take.
 */
data class DayLayout(
    val date: LocalDate,
    val wakeUp: LocalDateTime,
    val morningRoutineMinutes: Int,
    val bedTime: LocalDateTime,
    val eveningRoutineMinutes: Int,
    val meals: List<MealSlot>,
    val freeTimeMinutes: Int,
    val wasteMinutes: Int
) {
    val morningRoutineEnd: LocalDateTime
        get() = wakeUp.plusMinutes(morningRoutineMinutes.toLong())

    val eveningRoutineStart: LocalDateTime
        get() = bedTime.minusMinutes(eveningRoutineMinutes.toLong())

    /** Minutes between waking up and bedtime. */
    val awakeMinutes: Int
        get() = Duration.between(wakeUp, bedTime).toMinutes().toInt().coerceAtLeast(0)

    val mealMinutes: Int
        get() = meals.sumOf { it.minutes }

    /**
     * Minutes left for work: the time you are awake minus the routines, meals, free time and
     * wasted time. These are plain clock minutes: events, repeating entries and how
     * productive you are at different times are taken into account later.
     */
    val workCapacityMinutes: Int
        get() = (awakeMinutes - morningRoutineMinutes - eveningRoutineMinutes -
                mealMinutes - freeTimeMinutes - wasteMinutes).coerceAtLeast(0)
}