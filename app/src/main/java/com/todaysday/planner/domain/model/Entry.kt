package com.todaysday.planner.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Anything that can appear in the tree: a goal, a task, an event or a repeating entry.
 *
 * A "sealed interface" means: these four kinds are the ONLY ones that exist. That lets
 * the compiler warn us if we forget to handle one of them in a `when` expression.
 *
 * Fields that every kind shares are listed here; each kind adds its own below.
 * id = 0 means "not saved in the database yet".
 */
sealed interface Entry {
    val id: Long
    val parentId: Long?            // the entry this one is "part of" (null = top level)
    val name: String
    val description: String
    val priority: Level
    val effort: Level
    val dueDate: LocalDate?
    val placeId: Long?             // null means "Anywhere"
    val tagIds: Set<Long>
    val countsTowardParent: Boolean // true: time counts toward the parent. false: independent
    val completedAt: Instant?      // null = not done yet
    val createdAt: Instant
}

data class Goal(
    override val id: Long = 0,
    override val parentId: Long? = null,
    override val name: String,
    override val description: String = "",
    override val priority: Level = Level.NORMAL,
    override val effort: Level = Level.NORMAL,
    override val dueDate: LocalDate? = null,
    override val placeId: Long? = null,
    override val tagIds: Set<Long> = emptySet(),
    override val countsTowardParent: Boolean = true,
    override val completedAt: Instant? = null,
    override val createdAt: Instant = Instant.now(),
    val kind: GoalKind = GoalKind.ACHIEVEMENT,
    // TIME goal: the exact time needed. Other kinds: an estimate.
    val timeNeededMinutes: Int = 60,
    // NUMBER goals only:
    val numberTarget: Int? = null,   // how many in total
    val minutesPerUnit: Int? = null, // how long each one takes (an estimate)
    val numberDone: Int = 0          // how many are done so far
) : Entry

data class Task(
    override val id: Long = 0,
    override val parentId: Long? = null,
    override val name: String,
    override val description: String = "",
    override val priority: Level = Level.NORMAL,
    override val effort: Level = Level.NORMAL,
    override val dueDate: LocalDate? = null,
    override val placeId: Long? = null,
    override val tagIds: Set<Long> = emptySet(),
    override val countsTowardParent: Boolean = true,
    override val completedAt: Instant? = null,
    override val createdAt: Instant = Instant.now(),
    // A quick task only needs a name; by default it takes ten minutes.
    val timeNeededMinutes: Int = 10
) : Entry

data class Event(
    override val id: Long = 0,
    override val parentId: Long? = null,
    override val name: String,
    override val description: String = "",
    override val priority: Level = Level.NORMAL,
    override val effort: Level = Level.NORMAL,
    override val dueDate: LocalDate? = null,
    override val placeId: Long? = null,
    override val tagIds: Set<Long> = emptySet(),
    override val countsTowardParent: Boolean = true,
    override val completedAt: Instant? = null,
    override val createdAt: Instant = Instant.now(),
    val start: Instant,
    val end: Instant,
    // Set for events imported from a calendar, so a re-import doesn't create duplicates.
    val externalId: String? = null
) : Entry

data class Repeating(
    override val id: Long = 0,
    override val parentId: Long? = null,
    override val name: String,
    override val description: String = "",
    override val priority: Level = Level.NORMAL,
    override val effort: Level = Level.NORMAL,
    override val dueDate: LocalDate? = null,
    override val placeId: Long? = null,
    override val tagIds: Set<Long> = emptySet(),
    override val countsTowardParent: Boolean = true,
    override val completedAt: Instant? = null,
    override val createdAt: Instant = Instant.now(),
    val repeatType: RepeatType = RepeatType.WEEKDAYS,
    val repeatWeekdays: Set<DayOfWeek> = emptySet(),
    val repeatN: Int = 1,                              // the N in "every N days/weeks"
    val repeatAnchorDate: LocalDate = LocalDate.now(), // the day counting starts from
    val sessionMinutes: Int = 5                        // how long each occurrence takes
) : Entry