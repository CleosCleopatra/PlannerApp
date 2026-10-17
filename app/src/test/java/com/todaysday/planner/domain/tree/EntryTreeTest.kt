package com.todaysday.planner.domain.tree

import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * The tree used in most tests:
 *
 *   1 Goal
 *   |- 2 Goal (subgoal)
 *   |  `- 3 Task
 *   `- 4 Task
 *   5 Task (top level)
 */
class EntryTreeTest {

    private fun goal(id: Long, parent: Long? = null, completed: Boolean = false) = Goal(
        id = id, parentId = parent, name = "Goal $id",
        completedAt = if (completed) Instant.EPOCH else null
    )

    private fun task(id: Long, parent: Long? = null) =
        Task(id = id, parentId = parent, name = "Task $id")

    private fun standardEntries(completedIds: Set<Long> = emptySet()): List<Entry> = listOf(
        goal(1, completed = 1L in completedIds),
        goal(2, parent = 1, completed = 2L in completedIds),
        task(3, parent = 2),
        task(4, parent = 1),
        task(5)
    )

    private fun List<Entry>.ids() = map { it.id }

    @Test
    fun roots_are_the_entries_without_a_parent() {
        val tree = EntryTree(standardEntries())
        assertEquals(listOf(1L, 5L), tree.roots().ids())
    }

    @Test
    fun children_keep_the_order_of_the_original_list() {
        val tree = EntryTree(standardEntries())
        assertEquals(listOf(2L, 4L), tree.childrenOf(1).ids())
        assertTrue(tree.hasChildren(1))
        assertFalse(tree.hasChildren(4))
    }

    @Test
    fun unknown_id_has_no_children_and_is_not_found() {
        val tree = EntryTree(standardEntries())
        assertEquals(emptyList<Entry>(), tree.childrenOf(99))
        assertNull(tree[99])
    }

    @Test
    fun path_goes_from_the_top_down_to_the_entry() {
        val tree = EntryTree(standardEntries())
        assertEquals(listOf(1L, 2L, 3L), tree.pathTo(3).ids())
    }

    @Test
    fun depth_counts_how_far_down_an_entry_is() {
        val tree = EntryTree(standardEntries())
        assertEquals(0, tree.depthOf(1))
        assertEquals(1, tree.depthOf(2))
        assertEquals(2, tree.depthOf(3))
    }

    @Test
    fun descendants_list_parents_before_their_children() {
        val tree = EntryTree(standardEntries())
        assertEquals(listOf(2L, 3L, 4L), tree.descendantsOf(1).ids())
    }

    @Test
    fun an_entry_cannot_become_part_of_itself_or_of_something_below_it() {
        val tree = EntryTree(standardEntries())
        assertTrue(tree.wouldCreateCycle(entryId = 1, newParentId = 1))
        assertTrue(tree.wouldCreateCycle(entryId = 1, newParentId = 2))
        assertTrue(tree.wouldCreateCycle(entryId = 1, newParentId = 3))
    }

    @Test
    fun valid_moves_are_not_cycles() {
        val tree = EntryTree(standardEntries())
        assertFalse(tree.wouldCreateCycle(entryId = 3, newParentId = 1))
        assertFalse(tree.wouldCreateCycle(entryId = 2, newParentId = null))
        assertFalse(tree.wouldCreateCycle(entryId = 2, newParentId = 5))
    }

    @Test
    fun entry_with_a_missing_parent_is_treated_as_top_level() {
        val tree = EntryTree(listOf(task(9, parent = 99)))
        assertEquals(listOf(9L), tree.roots().ids())
    }

    @Test
    fun finished_entries_are_hidden_and_their_children_move_up() {
        // Goal 2 is done: its task 3 should now appear directly under goal 1.
        val tree = EntryTree(standardEntries(completedIds = setOf(2L))).withoutCompleted()
        assertNull(tree[2])
        assertEquals(listOf(1L, 5L), tree.roots().ids())
        assertEquals(listOf(3L, 4L), tree.childrenOf(1).ids())
    }

    @Test
    fun children_of_a_chain_of_finished_entries_become_top_level() {
        // Goals 1 and 2 are both done, so tasks 3 and 4 have no unfinished ancestor.
        val tree = EntryTree(standardEntries(completedIds = setOf(1L, 2L))).withoutCompleted()
        assertEquals(listOf(3L, 4L, 5L), tree.roots().ids())
    }
}