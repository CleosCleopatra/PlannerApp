package com.todaysday.planner.domain.picker

import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import com.todaysday.planner.domain.tree.DependencyGraph
import com.todaysday.planner.domain.tree.EntryTree
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks which entries are greyed out in the pickers. */
class PickerRulesTest {

    // 1 (goal) contains 3 (goal), which contains 4 (task). 2 is a separate goal.
    private val entries = listOf(
        Goal(id = 1, name = "One"),
        Goal(id = 2, name = "Two"),
        Goal(id = 3, parentId = 1, name = "Three"),
        Task(id = 4, parentId = 3, name = "Four")
    )
    private val tree = EntryTree(entries)

    @Test
    fun goal_cannot_be_placed_inside_itself_or_its_contents() {
        assertEquals(setOf(1L, 3L, 4L), PickerRules.disabledParents(tree, entryId = 1))
        assertEquals(setOf(2L), PickerRules.disabledParents(tree, entryId = 2))
    }

    @Test
    fun new_entry_has_nothing_greyed_out() {
        assertEquals(emptySet<Long>(), PickerRules.disabledParents(tree, entryId = null))

        val graph = DependencyGraph(tree, mapOf(1L to setOf(2L)))
        assertEquals(emptySet<Long>(), PickerRules.disabledRequirements(graph, tree, entryId = null))
    }

    @Test
    fun entry_cannot_require_what_would_end_up_waiting_for_it() {
        // 1 requires 2, so 2 must not require 1 (and obviously not itself).
        val graph = DependencyGraph(tree, mapOf(1L to setOf(2L)))

        assertEquals(setOf(1L, 2L), PickerRules.disabledRequirements(graph, tree, entryId = 2))
    }
}