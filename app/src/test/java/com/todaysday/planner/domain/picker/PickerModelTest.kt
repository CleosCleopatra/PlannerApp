package com.todaysday.planner.domain.picker

import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import com.todaysday.planner.domain.tree.DependencyGraph
import com.todaysday.planner.domain.tree.EntryTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Checks what the picker menus show. */
class PickerModelTest {

    private fun goal(id: Long, name: String, parent: Long? = null, done: Boolean = false) = Goal(
        id = id, parentId = parent, name = name,
        completedAt = if (done) Instant.EPOCH else null
    )

    // Learn Serbian
    //   |- Book (goal), Find tutor (task)
    // Archive (finished)
    //   `- Old chapter (not finished)
    // Build a plane (waits for "Order parts")
    // Order parts
    private val entries = listOf(
        goal(1, "Learn Serbian"),
        goal(2, "Book", parent = 1),
        Task(id = 3, parentId = 1, name = "Find tutor"),
        goal(4, "Archive", done = true),
        goal(5, "Old chapter", parent = 4),
        goal(6, "Build a plane"),
        goal(7, "Order parts")
    )

    private val blocked = DependencyGraph(EntryTree(entries), mapOf(6L to setOf(7L))).blockedIds()

    private fun model(options: PickerOptions = PickerOptions()) = PickerModel(entries, blocked, options)

    private fun List<PickerItem>.names() = map { it.entry.name }

    @Test
    fun finished_entries_are_hidden_and_their_unfinished_children_move_up() {
        val names = model().itemsIn(null).names()

        assertEquals(listOf("Build a plane", "Learn Serbian", "Old chapter", "Order parts"), names)
    }

    @Test
    fun finished_entries_can_be_shown() {
        val names = model(PickerOptions(hideCompleted = false)).itemsIn(null).names()

        assertEquals(listOf("Archive", "Build a plane", "Learn Serbian", "Order parts"), names)
    }

    @Test
    fun goals_open_up_to_show_what_is_inside_in_alphabetical_order() {
        assertEquals(listOf("Book", "Find tutor"), model().itemsIn(1).names())
    }

    @Test
    fun entry_kinds_can_be_filtered() {
        val onlyGoals = PickerOptions(allowedTypes = setOf(EntryType.GOAL))

        assertEquals(listOf("Book"), model(onlyGoals).itemsIn(1).names())
    }

    @Test
    fun items_know_if_there_is_something_to_open() {
        val items = model().itemsIn(null)

        assertTrue(items.first { it.entry.name == "Learn Serbian" }.hasChildren)
        assertFalse(items.first { it.entry.name == "Build a plane" }.hasChildren)
    }

    @Test
    fun blocked_entries_can_be_hidden() {
        assertTrue(model().itemsIn(null).names().contains("Build a plane"))

        val names = model(PickerOptions(hideBlocked = true)).itemsIn(null).names()
        assertEquals(listOf("Learn Serbian", "Old chapter", "Order parts"), names)
    }

    @Test
    fun disabled_entries_are_listed_but_cannot_be_chosen() {
        val items = model(PickerOptions(disabledIds = setOf(2L))).itemsIn(1)

        assertEquals(listOf("Book", "Find tutor"), items.names())
        assertFalse(items.first { it.entry.name == "Book" }.selectable)
        assertTrue(items.first { it.entry.name == "Find tutor" }.selectable)
    }

    @Test
    fun path_and_parent_support_the_back_button() {
        val model = model()

        assertEquals(listOf("Learn Serbian", "Book"), model.pathTo(2).map { it.name })
        assertEquals(1L, model.parentOf(2))
        assertNull(model.parentOf(1))
        // "Old chapter" lost its finished parent, so it is now at the top level.
        assertNull(model.parentOf(5))
    }
}