package com.todaysday.planner.ui.goals

import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.Event
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.RepeatType
import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/** Checks what the Goals screen would show for different lists of entries. */
class GoalsStateBuilderTest {

    // ---------- Small helpers that create entries (createdAt is fixed so order is predictable) ----------

    private fun goal(
        id: Long, name: String, parent: Long? = null, kind: GoalKind = GoalKind.TIME,
        minutes: Int = 60, due: LocalDate? = null, done: Boolean = false
    ) = Goal(
        id = id, parentId = parent, name = name, kind = kind, timeNeededMinutes = minutes,
        dueDate = due, completedAt = if (done) Instant.EPOCH else null,
        createdAt = Instant.ofEpochSecond(id)
    )

    private fun task(id: Long, name: String, parent: Long? = null, done: Boolean = false) = Task(
        id = id, parentId = parent, name = name,
        completedAt = if (done) Instant.EPOCH else null,
        createdAt = Instant.ofEpochSecond(id)
    )

    private fun event(id: Long, name: String, parent: Long? = null) = Event(
        id = id, parentId = parent, name = name,
        start = Instant.parse("2026-10-17T18:00:00Z"),
        end = Instant.parse("2026-10-17T19:30:00Z"),
        createdAt = Instant.ofEpochSecond(id)
    )

    private fun repeating(id: Long, name: String, parent: Long? = null) = Repeating(
        id = id, parentId = parent, name = name,
        repeatType = RepeatType.WEEKDAYS,
        repeatWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        repeatAnchorDate = LocalDate.of(2026, 10, 5),
        sessionMinutes = 5,
        createdAt = Instant.ofEpochSecond(id)
    )

    // The tree used in most tests:
    //   1 Learn Serbian (goal)
    //   |- 2 Book (goal), 3 Find tutor (task), 4 Serbian meet (event), 5 Study 5 min (repeating)
    //   6 Buy milk (task, top level)
    //   7 Build a plane (goal, top level)
    private fun standardEntries(): List<Entry> = listOf(
        goal(1, "Learn Serbian"),
        goal(2, "Book", parent = 1),
        task(3, "Find tutor", parent = 1),
        event(4, "Serbian meet", parent = 1),
        repeating(5, "Study 5 min", parent = 1),
        task(6, "Buy milk"),
        goal(7, "Build a plane")
    )

    // Builds the state with a fixed time zone and language so the results are the same everywhere.
    private fun build(
        entries: List<Entry>,
        tab: EntryType = EntryType.GOAL,
        open: Long? = null,
        logged: Map<Long, Int> = emptyMap()
    ) = buildGoalsUiState(
        entries = entries, loggedMinutes = logged, selectedTab = tab, openGoalId = open,
        zone = ZoneOffset.UTC, locale = Locale.ENGLISH
    )

    private fun GoalsUiState.names(section: Int = 0) = sections[section].rows.map { it.entry.name }

    // ---------- Goals subtab ----------

    @Test
    fun goals_tab_shows_only_top_level_goals() {
        val state = build(standardEntries())

        assertEquals(listOf("Learn Serbian", "Build a plane"), state.names())
        assertNull(state.openGoalRow)
    }

    @Test
    fun opening_a_goal_groups_its_children_by_kind() {
        val state = build(standardEntries(), open = 1)

        assertEquals(listOf("Subgoals", "Tasks", "Events", "Repeating"), state.sections.map { it.title })
        assertEquals(listOf("Book"), state.names(0))
        assertEquals(listOf("Find tutor"), state.names(1))
        assertEquals(listOf("Serbian meet"), state.names(2))
        assertEquals(listOf("Study 5 min"), state.names(3))
        assertEquals(1L, state.openGoalRow!!.entry.id)
    }

    @Test
    fun empty_groups_are_left_out() {
        val state = build(standardEntries(), open = 2) // "Book" has nothing inside

        assertTrue(state.sections.isEmpty())
        assertTrue(state.isEmpty)
        assertNotNull(state.openGoalRow)
    }

    @Test
    fun opening_a_goal_gives_breadcrumb_and_parent() {
        val inside = build(standardEntries(), open = 2)
        assertEquals(listOf("Learn Serbian", "Book"), inside.breadcrumb)
        assertEquals(1L, inside.parentOfOpenGoalId)

        val topGoal = build(standardEntries(), open = 1)
        assertNull(topGoal.parentOfOpenGoalId)
    }

    @Test
    fun opening_something_that_is_not_a_goal_shows_the_top_level() {
        val notAGoal = build(standardEntries(), open = 3) // a task
        val missing = build(standardEntries(), open = 99)

        assertNull(notAGoal.openGoalId)
        assertNull(missing.openGoalId)
        assertEquals(listOf("Learn Serbian", "Build a plane"), notAGoal.names())
    }

    @Test
    fun goal_cards_count_what_is_directly_inside_them() {
        val state = build(standardEntries())

        assertEquals(4, state.sections.single().rows[0].childCount) // Learn Serbian
        assertEquals(0, state.sections.single().rows[1].childCount) // Build a plane
    }

    // ---------- Other subtabs ----------

    @Test
    fun other_tabs_list_all_entries_of_their_kind_with_their_path() {
        val entries = standardEntries() + task(8, "Read chapter 1", parent = 2)
        val rows = build(entries, tab = EntryType.TASK).sections.single().rows

        assertEquals(listOf("Find tutor", "Buy milk", "Read chapter 1"), rows.map { it.entry.name })
        assertEquals("Learn Serbian", rows[0].subtitle)
        assertNull(rows[1].subtitle)
        assertEquals("Learn Serbian › Book", rows[2].subtitle)
    }

    @Test
    fun finished_entries_come_last() {
        val entries = listOf(task(1, "Done already", done = true), task(2, "Still to do"))
        val state = build(entries, tab = EntryType.TASK)

        assertEquals(listOf("Still to do", "Done already"), state.names())
    }

    // ---------- Progress text ----------

    @Test
    fun time_goal_shows_the_time_left() {
        val entries = listOf(goal(1, "Study", minutes = 600))

        val untouched = build(entries).sections.single().rows.single()
        assertEquals("10 h left", untouched.progressText)

        val started = build(entries, logged = mapOf(1L to 150)).sections.single().rows.single()
        assertEquals("7 h 30 min left", started.progressText)
        assertEquals(0.25f, started.fraction, 0.0001f)
        assertTrue(started.showProgress)
    }

    @Test
    fun finished_entry_says_done() {
        val row = build(listOf(goal(1, "Study", done = true))).sections.single().rows.single()

        assertEquals("Done", row.progressText)
        assertEquals(1f, row.fraction, 0.0001f)
    }

    @Test
    fun number_goal_shows_how_many_are_left() {
        val words = Goal(
            id = 1, name = "Words", kind = GoalKind.NUMBER, timeNeededMinutes = 2000,
            numberTarget = 1000, minutesPerUnit = 2, numberDone = 400,
            createdAt = Instant.ofEpochSecond(1)
        )
        val row = build(listOf(words)).sections.single().rows.single()

        assertEquals("600 of 1000 left (20 h)", row.progressText)
    }

    @Test
    fun events_and_repeating_entries_have_no_progress_bar() {
        val entries = standardEntries()
        val event = build(entries, tab = EntryType.EVENT).sections.single().rows.single()
        val repeating = build(entries, tab = EntryType.REPEATING).sections.single().rows.single()

        assertFalse(event.showProgress)
        assertEquals("", event.progressText)
        assertFalse(repeating.showProgress)
        assertEquals("", repeating.progressText)
    }

    // ---------- Detail line ----------

    @Test
    fun goal_detail_shows_kind_and_due_date() {
        val entries = listOf(goal(1, "Plane", kind = GoalKind.ACHIEVEMENT, due = LocalDate.of(2026, 12, 24)))
        val row = build(entries).sections.single().rows.single()

        assertEquals("Achievement goal · due 2026-12-24", row.detail)
    }

    @Test
    fun event_detail_shows_the_start_time() {
        val row = build(standardEntries(), tab = EntryType.EVENT).sections.single().rows.single()

        assertEquals("Event Sat 17 Oct, 18:00", row.detail)
    }

    @Test
    fun repeating_detail_shows_the_days_and_length() {
        val row = build(standardEntries(), tab = EntryType.REPEATING).sections.single().rows.single()

        assertEquals("Repeats on Mon, Wed · 5 min", row.detail)
    }

    // ---------- Misc ----------

    @Test
    fun state_knows_the_total_number_of_entries() {
        val state = build(standardEntries())

        assertEquals(7, state.totalEntries)
        assertFalse(state.isLoading)
    }
}