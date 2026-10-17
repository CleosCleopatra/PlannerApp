package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Level
import com.todaysday.planner.domain.model.Task
import java.time.Instant
import java.time.LocalDate

/** The error messages of the task form. null means "this field is fine". */
data class TaskFormErrors(
    val name: String? = null,
    val time: String? = null
) {
    val hasErrors: Boolean get() = name != null || time != null
}

/**
 * Everything typed into the task form. A task only needs a name: it takes 10 minutes
 * unless you change it under "More options".
 */
data class TaskFormState(
    val original: Task? = null,
    val parentId: Long? = null,
    val parentName: String? = null,
    val name: String = "",
    val description: String = "",
    val timeMinutes: Int? = 10,
    val priority: Level = Level.NORMAL,
    val effort: Level = Level.NORMAL,
    val dueDate: LocalDate? = null,
    val countsTowardParent: Boolean = true,
    val tagIds: Set<Long> = emptySet(),
    val placeId: Long? = null,          // null = Anywhere
    val requiredIds: Set<Long> = emptySet(), // "requires completion of"
    val isDone: Boolean = false,
    val showExtras: Boolean = false,   // is the "More options" section open?
    val isLoaded: Boolean = false,
    val showErrors: Boolean = false,
    val saveError: String? = null      // a problem found when saving (e.g. a loop)
) {
    val isEditing: Boolean get() = original != null

    fun errors() = TaskFormErrors(
        name = if (name.isBlank()) "Enter a name" else null,
        time = if ((timeMinutes ?: 0) <= 0) "Enter how long it will take" else null
    )

    /** The finished Task, or null if the form has errors. */
    fun toTask(now: Instant = Instant.now()): Task? {
        if (errors().hasErrors) return null

        val base = original ?: Task(name = name, createdAt = now)
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
            timeNeededMinutes = timeMinutes ?: base.timeNeededMinutes
        )
    }

    companion object {
        fun forNew(parentId: Long?, parentName: String?) = TaskFormState(
            parentId = parentId,
            parentName = parentName,
            isLoaded = true
        )

        // When editing, all the options are shown right away.
        fun forTask(
            task: Task,
            parentName: String?,
            requiredIds: Set<Long> = emptySet()
        ) = TaskFormState(
            original = task,
            parentId = task.parentId,
            parentName = parentName,
            name = task.name,
            description = task.description,
            timeMinutes = task.timeNeededMinutes,
            priority = task.priority,
            effort = task.effort,
            dueDate = task.dueDate,
            countsTowardParent = task.countsTowardParent,
            tagIds = task.tagIds,
            placeId = task.placeId,
            requiredIds = requiredIds,
            isDone = task.completedAt != null,
            showExtras = true,
            isLoaded = true
        )
    }
}