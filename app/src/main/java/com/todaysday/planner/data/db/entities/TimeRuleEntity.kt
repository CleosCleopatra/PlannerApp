package com.todaysday.planner.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.todaysday.planner.domain.model.Level
import com.todaysday.planner.domain.rules.MealKind

/** The kinds of rule, as saved in the database. */
enum class TimeRuleType {
    SLEEP_LENGTH, WAKE_UP_TIME, MORNING_ROUTINE, EVENING_ROUTINE, MEAL,
    FREE_TIME, WASTE, HALF_STATE, PRODUCTIVITY, LOW_EFFORT_WINDOW
}

/**
 * The "time_rules" table. All kinds of rule share this one table and the same columns.
 * Which columns a rule uses depends on its type (the others stay empty):
 *
 * - SLEEP_LENGTH, MORNING_ROUTINE, EVENING_ROUTINE, FREE_TIME, WASTE: minutes
 * - WAKE_UP_TIME:       timeA = the time
 * - MEAL:               meal, minutes, timeA = earliest start, timeB = latest start
 * - HALF_STATE:         multiplier
 * - PRODUCTIVITY:       multiplier, weekdaysMask, timeA = from, timeB = until, effort
 * - LOW_EFFORT_WINDOW:  weekdaysMask, timeA = from, timeB = until
 *
 * Times are saved as text like "14:30". weekdaysMask works like for repeating entries
 * (Monday = 1, Tuesday = 2, Wednesday = 4, ...; 0 = every day).
 */
@Entity(
    tableName = "time_rules",
    indices = [Index("type")]
)
data class TimeRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TimeRuleType,
    val enabled: Boolean = true,
    val customName: String? = null,
    val minutes: Int? = null,
    val multiplier: Double? = null,
    val timeA: String? = null,
    val timeB: String? = null,
    val weekdaysMask: Int = 0,
    val meal: MealKind? = null,
    val effort: Level? = null
)