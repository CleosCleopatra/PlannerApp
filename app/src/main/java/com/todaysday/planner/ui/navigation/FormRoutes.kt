package com.todaysday.planner.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task

/**
 * The "addresses" of the form screens. Each form gets two values in its address:
 * entryId (the entry to edit, or NONE for a new one) and parentId (the goal to put it in, or NONE).
 */
object FormRoutes {
    const val NONE = -1L
    const val ENTRY_ID = "entryId"
    const val PARENT_ID = "parentId"

    // The patterns used when the NavHost is set up.
    const val GOAL_FORM = "goal_form?entryId={entryId}&parentId={parentId}"
    const val TASK_FORM = "task_form?entryId={entryId}&parentId={parentId}"

    val arguments = listOf(
        navArgument(ENTRY_ID) { type = NavType.LongType; defaultValue = NONE },
        navArgument(PARENT_ID) { type = NavType.LongType; defaultValue = NONE }
    )

    /** The address for creating a new goal or task. Events and repeating entries come in M6. */
    fun create(type: EntryType, parentId: Long?): String {
        val parent = parentId ?: NONE
        return when (type) {
            EntryType.GOAL -> "goal_form?entryId=$NONE&parentId=$parent"
            EntryType.TASK -> "task_form?entryId=$NONE&parentId=$parent"
            else -> throw IllegalArgumentException("No form for $type yet")
        }
    }

    /** The address for editing an entry, or null for kinds that have no form yet. */
    fun edit(entry: Entry): String? = when (entry) {
        is Goal -> "goal_form?entryId=${entry.id}&parentId=$NONE"
        is Task -> "task_form?entryId=${entry.id}&parentId=$NONE"
        else -> null
    }
}

/** Reads one of the id values from the address; null if it was NONE. */
fun NavBackStackEntry.optionalId(key: String): Long? =
    arguments?.getLong(key, FormRoutes.NONE)?.takeIf { it != FormRoutes.NONE }