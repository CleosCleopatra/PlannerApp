package com.todaysday.planner.domain.tree

import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Checks "requires completion of": who is blocked, and which requirements would cause a loop. */
class DependencyGraphTest {

    private fun goal(id: Long, parent: Long? = null, done: Boolean = false) = Goal(
        id = id, parentId = parent, name = "Goal $id",
        completedAt = if (done) Instant.EPOCH else null
    )

    private fun task(id: Long, parent: Long? = null) =
        Task(id = id, parentId = parent, name = "Task $id")

    // pairs like  2L to setOf(1L)  mean "entry 2 requires entry 1".
    private fun graph(entries: List<Entry>, vararg requirements: Pair<Long, Set<Long>>) =
        DependencyGraph(EntryTree(entries), requirements.toMap())

    @Test
    fun entry_is_blocked_until_what_it_requires_is_complete() {
        val waiting = graph(listOf(goal(1), goal(2)), 2L to setOf(1L))
        assertEquals(setOf(2L), waiting.blockedIds())
        assertTrue(waiting.isBlocked(2))
        assertFalse(waiting.isBlocked(1))

        val finished = graph(listOf(goal(1, done = true), goal(2)), 2L to setOf(1L))
        assertEquals(emptySet<Long>(), finished.blockedIds())
    }

    @Test
    fun everything_inside_a_blocked_goal_is_blocked_too() {
        // Goal 1 (with goal 3 and task 4 inside) requires goal 2.
        val entries = listOf(goal(1), goal(2), goal(3, parent = 1), task(4, parent = 3))
        val graph = graph(entries, 1L to setOf(2L))

        assertEquals(setOf(1L, 3L, 4L), graph.blockedIds())
    }

    @Test
    fun requirement_on_a_missing_entry_is_ignored() {
        val graph = graph(listOf(goal(1)), 1L to setOf(99L))

        assertEquals(emptySet<Long>(), graph.blockedIds())
    }

    @Test
    fun entry_cannot_require_itself() {
        assertTrue(graph(listOf(goal(1))).wouldCreateCycle(1, 1))
    }

    @Test
    fun two_entries_cannot_require_each_other() {
        val graph = graph(listOf(goal(1), goal(2)), 1L to setOf(2L))

        assertTrue(graph.wouldCreateCycle(entryId = 2, requiredId = 1))
        assertFalse(graph.wouldCreateCycle(entryId = 2, requiredId = 3))
    }

    @Test
    fun longer_chain_that_closes_into_a_loop_is_detected() {
        // 1 requires 2, and 2 requires 3. Now 3 must not require 1.
        val graph = graph(listOf(goal(1), goal(2), goal(3)), 1L to setOf(2L), 2L to setOf(3L))

        assertTrue(graph.wouldCreateCycle(entryId = 3, requiredId = 1))
    }

    @Test
    fun shortcut_in_a_chain_is_not_a_loop() {
        val graph = graph(listOf(goal(1), goal(2), goal(3)), 1L to setOf(2L), 2L to setOf(3L))

        assertFalse(graph.wouldCreateCycle(entryId = 1, requiredId = 3))
    }

    @Test
    fun goal_cannot_require_something_inside_itself() {
        val entries = listOf(goal(1), goal(3, parent = 1), task(4, parent = 3))
        val graph = graph(entries)

        assertTrue(graph.wouldCreateCycle(entryId = 1, requiredId = 3))
        assertTrue(graph.wouldCreateCycle(entryId = 1, requiredId = 4))
    }

    @Test
    fun something_inside_a_goal_may_require_that_goal() {
        val graph = graph(listOf(goal(1), goal(3, parent = 1)))

        assertFalse(graph.wouldCreateCycle(entryId = 3, requiredId = 1))
    }

    @Test
    fun a_requirement_inherited_through_a_parent_can_make_a_loop() {
        // Goal 1 requires goal 2, and goal 3 is inside goal 1 so it waits for goal 2 as well.
        // Goal 2 must therefore not require goal 3.
        val entries = listOf(goal(1), goal(2), goal(3, parent = 1))
        val graph = graph(entries, 1L to setOf(2L))

        assertTrue(graph.wouldCreateCycle(entryId = 2, requiredId = 3))
    }
}