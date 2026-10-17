package com.todaysday.planner.domain.model

// The four kinds of entries in the "tree" on the Goals tab.
enum class EntryType { GOAL, TASK, EVENT, REPEATING }

// The three kinds of goal:
// TIME        = spend a certain amount of time (e.g. 10 hours studying Serbian)
// ACHIEVEMENT = achieve something (e.g. build a plane)
// NUMBER      = do a number of things (e.g. learn 1000 Serbian words)
enum class GoalKind { TIME, ACHIEVEMENT, NUMBER }

// Used for both priority and effort.
enum class Level { LOW, NORMAL, HIGH }

// How a repeating entry repeats.
enum class RepeatType {
    WEEKDAYS,                // on selected weekdays
    EVERY_N_DAYS,            // every N days
    WEEKDAYS_EVERY_N_WEEKS   // on selected weekdays, every N weeks
}