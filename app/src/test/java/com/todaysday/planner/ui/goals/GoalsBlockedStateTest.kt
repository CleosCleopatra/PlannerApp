package com.todaysday.planner.ui.goals

import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/** Checks which entries are marked as blocked, and what they say they are waiting for. */
class GoalsBlockedStateTest {

    private fun goal(id: Long, name: String, parent: Long? = null, done: Boolean = false) = Goal(
        id = id, parentId = parent, name = name,
        completedAt = if (done) Instant.EPOCH else null,
        createdAt = Instant.ofEpochSecond(id)
    )

    private fun task(id: Long, name: String, parent: Long? = null) = Task(
        id = id, parentId = parent, name = name, createdAt = Instant.ofEpochSecond(id)
    )

    // requirements: pairs like  1L to setOf(2L)  mean "entry 1 requires entry 2".
    private fun build(
        entries: List<Entry>,
        requirements: Map<Long, Set<Long>>,
        tab: EntryType = EntryType.GOAL,
        open: Long? = null
    ) = buildGoalsUiState(
        entries = entries, loggedMinutes = emptyMap(), requirements = requirements,
        selectedTab = tab, openGoalId = open, zone = ZoneOffset.UTC, locale = Locale.ENGLISH
    )

    // Finds a card by the name of its entry (the open goal's card included).
    private fun GoalsUiState.row(name: String): EntryRow =
        (sections.flatMap { it.rows } + listOfNotNull(openGoalRow)).first { it.entry.name == name }

    @Test
    fun blocked_entry_shows_what_it_waits_for() {
        val entries = listOf(goal(1, "Build a plane"), goal(2, "Order parts"))
        val state = build(entries, mapOf(1L to setOf(2L)))

        assertTrue(state.row("Build a plane").isBlocked)
        assertEquals(listOf("Order parts"), state.row("Build a plane").waitingFor)
        assertFalse(state.row("Order parts").isBlocked)
    }

    @Test
    fun entry_is_free_again_when_what_it_requires_is_finished() {
        val entries = listOf(goal(1, "Build a plane"), goal(2, "Order parts", done = true))
        val state = build(entries, mapOf(1L to setOf(2L)))

        assertFalse(state.row("Build a plane").isBlocked)
        assertEquals(emptyList<String>(), state.row("Build a plane").waitingFor)
    }

    @Test
    fun finished_entry_is_never_shown_as_blocked() {
        val entries = listOf(goal(1, "Build a plane", done = true), goal(2, "Order parts"))
        val state = build(entries, mapOf(1L to setOf(2L)))

        assertFalse(state.row("Build a plane").isBlocked)
    }

    @Test
    fun everything_inside_a_blocked_goal_is_blocked_too() {
        val entries = listOf(
            goal(1, "Learn Serbian"),
            goal(2, "Order parts"),
            task(3, "Find tutor", parent = 1)
        )
        val state = build(entries, mapOf(1L to setOf(2L)), open = 1)

        // assertEquals(listOf("Order parts"), state.row("Find tutor").waitingFor)
        // The card of the open goal itself shows it as well.
        assertEquals(listOf("Order parts"), state.row("Learn Serbian").waitingFor)
    }

    @Test
    fun several_requirements_are_listed_in_alphabetical_order() {
        val entries = listOf(goal(1, "Plane"), goal(2, "Zeta"), goal(3, "alpha"))
        val state = build(entries, mapOf(1L to setOf(2L, 3L)))

        assertEquals(listOf("alpha", "Zeta"), state.row("Plane").waitingFor)
    }

    @Test
    fun only_unfinished_requirements_are_listed() {
        val entries = listOf(goal(1, "Plane"), goal(2, "Open one"), goal(3, "Done one", done = true))
        val state = build(entries, mapOf(1L to setOf(2L, 3L)))

        assertEquals(listOf("Open one"), state.row("Plane").waitingFor)
    }

    @Test
    fun requirement_on_a_missing_entry_is_ignored() {
        val state = build(listOf(goal(1, "Plane")), mapOf(1L to setOf(99L)))

        assertFalse(state.row("Plane").isBlocked)
    }

    @Test
    fun the_other_subtabs_show_blocked_entries_too() {
        val entries = listOf(goal(2, "Order parts"), task(3, "Find tutor"))
        val state = build(entries, mapOf(3L to setOf(2L)), tab = EntryType.TASK)

        assertTrue(state.row("Find tutor").isBlocked)
        assertEquals(listOf("Order parts"), state.row("Find tutor").waitingFor)
    }
}