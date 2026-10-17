package com.todaysday.planner.domain.picker

import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import com.todaysday.planner.domain.tree.DependencyGraph
import com.todaysday.planner.domain.tree.EntryTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the loop protection when moving entries and choosing requirements together. */
class PickerRulesCycleTest {

    // 1 is a goal, 2 and 3 are tasks. All three are at the top level.
    private val entries = listOf(
        Goal(id = 1, name = "G"),
        Task(id = 2, name = "E"),
        Task(id = 3, name = "X")
    )
    private val tree = EntryTree(entries)

    // Goal 1 requires task 2.
    private val requirements = mapOf(1L to setOf(2L))

    @Test
    fun moving_under_a_goal_that_waits_for_it_is_a_loop() {
        // Task 2 inside goal 1 would wait for goal 1's requirement, which is task 2 itself.
        assertTrue(
            PickerRules.choicesCreateCycle(
                tree, requirements, entryId = 2, newParentId = 1, requiredIds = emptySet()
            )
        )
    }

    @Test
    fun moving_to_the_top_level_is_fine() {
        assertFalse(
            PickerRules.choicesCreateCycle(
                tree, requirements, entryId = 2, newParentId = null, requiredIds = emptySet()
            )
        )
    }

    @Test
    fun requiring_something_that_waits_for_you_is_a_loop() {
        assertTrue(
            PickerRules.choicesCreateCycle(
                tree, requirements, entryId = 2, newParentId = null, requiredIds = setOf(1L)
            )
        )
    }

    @Test
    fun new_entries_never_make_a_loop() {
        assertFalse(
            PickerRules.choicesCreateCycle(
                tree, requirements, entryId = null, newParentId = 1, requiredIds = setOf(1L, 2L)
            )
        )
    }

    @Test
    fun parent_picker_greys_out_goals_that_would_make_a_loop() {
        // Task 2 can't go inside itself, or inside goal 1 (which waits for it).
        assertEquals(setOf(1L, 2L), PickerRules.disabledParents(tree, entryId = 2, requirements = requirements))
        // Without the requirement only task 2 itself is greyed out.
        assertEquals(setOf(2L), PickerRules.disabledParents(tree, entryId = 2))
    }

    @Test
    fun a_tree_can_be_tried_out_with_a_different_parent() {
        val moved = tree.withParentOf(entryId = 2, newParentId = 1)

        assertEquals(listOf(2L), moved.childrenOf(1).map { it.id })
        assertEquals(listOf(1L, 3L), moved.roots().map { it.id })
        // The original tree is not changed.
        assertEquals(listOf(1L, 2L, 3L), tree.roots().map { it.id })
    }

    @Test
    fun a_circle_of_requirements_is_detected() {
        val circle = DependencyGraph(tree, mapOf(1L to setOf(2L), 2L to setOf(1L)))

        assertTrue(circle.hasCycle())
    }

    @Test
    fun a_plain_chain_of_requirements_is_not_a_circle() {
        val chain = DependencyGraph(tree, mapOf(1L to setOf(2L), 2L to setOf(3L)))

        assertFalse(chain.hasCycle())
    }
}