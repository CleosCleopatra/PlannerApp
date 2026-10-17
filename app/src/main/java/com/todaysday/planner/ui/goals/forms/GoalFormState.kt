package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.Level
import java.time.Instant
import java.time.LocalDate

/** The error messages of the goal form. null means "this field is fine". */
data class GoalFormErrors(
    val name: String? = null,
    val time: String? = null,
    val numberTarget: String? = null,
    val minutesPerUnit: String? = null,
    val numberDone: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(name, time, numberTarget, minutesPerUnit, numberDone).any { it != null }
}

/**
 * Everything the user has typed into the goal form so far.
 * Numbers can be null while a field is still empty.
 *
 * - errors()  checks the input and returns messages
 * - toGoal()  turns valid input into a Goal (or null if something is wrong)
 * Neither knows anything about the screen, so both are easy to test.
 */
data class GoalFormState(
    val original: Goal? = null,        // the goal being edited (null = creating a new one)
    val parentId: Long? = null,
    val parentName: String? = null,
    val name: String = "",
    val description: String = "",
    val kind: GoalKind = GoalKind.ACHIEVEMENT,
    val timeMinutes: Int? = null,
    val numberTarget: Int? = null,
    val minutesPerUnit: Int? = null,
    val numberDone: Int? = 0,
    val priority: Level = Level.NORMAL,
    val effort: Level = Level.NORMAL,
    val dueDate: LocalDate? = null,
    val countsTowardParent: Boolean = true,
    val tagIds: Set<Long> = emptySet(),
    val placeId: Long? = null,          // null = Anywhere
    val requiredIds: Set<Long> = emptySet(), // "requires completion of"
    val isDone: Boolean = false,
    val isLoaded: Boolean = false,     // false while an existing goal is being loaded
    val showErrors: Boolean = false,   // errors are shown only after the first Save attempt
    val saveError: String? = null      // a problem found when saving (e.g. a loop)
) {
    val isEditing: Boolean get() = original != null

    /** The total time. For number goals it is worked out: how many x minutes each. */
    val totalMinutes: Int?
        get() = when (kind) {
            GoalKind.NUMBER -> {
                val target = numberTarget
                val perUnit = minutesPerUnit
                if (target != null && perUnit != null) target * perUnit else null
            }
            else -> timeMinutes
        }

    fun errors(): GoalFormErrors {
        val target = numberTarget
        val perUnit = minutesPerUnit
        val isNumber = kind == GoalKind.NUMBER

        return GoalFormErrors(
            name = if (name.isBlank()) "Enter a name" else null,
            time = if (!isNumber && (timeMinutes ?: 0) <= 0) "Enter how long it will take" else null,
            numberTarget = if (isNumber && (target ?: 0) <= 0) "Enter how many" else null,
            minutesPerUnit = if (isNumber && (perUnit ?: 0) <= 0) "Enter how many minutes each takes" else null,
            numberDone = if (isNumber && target != null && target > 0 && (numberDone ?: 0) > target) {
                "Can't be more than the total"
            } else {
                null
            }
        )
    }

    /** The finished Goal, or null if the form has errors. */
    fun toGoal(now: Instant = Instant.now()): Goal? {
        if (errors().hasErrors) return null

        val isNumber = kind == GoalKind.NUMBER
        // When editing we start from the original, so things the form doesn't show
        // (id, creation time, ...) are kept as they were.
        val base = original ?: Goal(name = name, createdAt = now)

        return base.copy(
            parentId = parentId,
            name = name.trim(),
            description = description.trim(),
            priority = priority,
            effort = effort,
            dueDate = dueDate,
            countsTowardParent = countsTowardParent,
            tagIds = tagIds,
            placeId = placeId,
            completedAt = if (isDone) (original?.completedAt ?: now) else null,
            kind = kind,
            timeNeededMinutes = totalMinutes ?: base.timeNeededMinutes,
            numberTarget = if (isNumber) numberTarget else null,
            minutesPerUnit = if (isNumber) minutesPerUnit else null,
            numberDone = if (isNumber) (numberDone ?: 0) else 0
        )
    }

    companion object {
        /** An empty form for a new goal. */
        fun forNew(parentId: Long?, parentName: String?) = GoalFormState(
            parentId = parentId,
            parentName = parentName,
            isLoaded = true
        )

        /** A form filled in from an existing goal and the entries it requires. */
        fun forGoal(
            goal: Goal,
            parentName: String?,
            requiredIds: Set<Long> = emptySet()
        ) = GoalFormState(
            original = goal,
            parentId = goal.parentId,
            parentName = parentName,
            name = goal.name,
            description = goal.description,
            kind = goal.kind,
            timeMinutes = goal.timeNeededMinutes,
            numberTarget = goal.numberTarget,
            minutesPerUnit = goal.minutesPerUnit,
            numberDone = goal.numberDone,
            priority = goal.priority,
            effort = goal.effort,
            dueDate = goal.dueDate,
            countsTowardParent = goal.countsTowardParent,
            tagIds = goal.tagIds,
            placeId = goal.placeId,
            requiredIds = requiredIds,
            isDone = goal.completedAt != null,
            isLoaded = true
        )
    }
}