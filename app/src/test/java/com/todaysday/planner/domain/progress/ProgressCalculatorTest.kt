package com.todaysday.planner.domain.progress

import com.todaysday.planner.domain.model.Event
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.Task
import com.todaysday.planner.domain.tree.EntryTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Checks the progress rules one situation at a time. */
class ProgressCalculatorTest {

    // Builds a calculator from a list of entries and the logged minutes (entry id -> minutes).
    private fun calculator(
        entries: List<com.todaysday.planner.domain.model.Entry>,
        logged: Map<Long, Int> = emptyMap()
    ) = ProgressCalculator(EntryTree(entries), logged)

    private val done = Instant.EPOCH // any date works as "checked off"

    @Test
    fun time_goal_shows_the_remaining_time() {
        val goal = Goal(id = 1, name = "Study", kind = GoalKind.TIME, timeNeededMinutes = 600)
        val p = calculator(listOf(goal), mapOf(1L to 150)).progressOf(1)!!

        assertEquals(600, p.neededMinutes)
        assertEquals(150, p.doneMinutes)
        assertEquals(450, p.leftMinutes)
        assertEquals(0.25f, p.fraction, 0.0001f)
        assertTrue(p.hasBar)
    }

    @Test
    fun logged_time_beyond_what_is_needed_is_capped() {
        val goal = Goal(id = 1, name = "Study", kind = GoalKind.TIME, timeNeededMinutes = 60)
        val p = calculator(listOf(goal), mapOf(1L to 200)).progressOf(1)!!

        assertEquals(60, p.doneMinutes)
        assertEquals(0, p.leftMinutes)
        assertEquals(1f, p.fraction, 0.0001f)
    }

    @Test
    fun achievement_goal_with_a_used_up_estimate_has_nothing_left_but_is_not_complete() {
        val goal = Goal(id = 1, name = "Build a plane", kind = GoalKind.ACHIEVEMENT, timeNeededMinutes = 120)
        val p = calculator(listOf(goal), mapOf(1L to 200)).progressOf(1)!!

        assertEquals(0, p.leftMinutes)
        assertFalse(p.isComplete)
    }

    @Test
    fun task_progress_uses_its_own_time() {
        val task = Task(id = 1, name = "Buy milk", timeNeededMinutes = 10)
        val p = calculator(listOf(task), mapOf(1L to 4)).progressOf(1)!!

        assertEquals(6, p.leftMinutes)
    }

    @Test
    fun checked_off_entry_is_fully_done_even_if_nothing_was_logged() {
        val goal = Goal(id = 1, name = "Study", kind = GoalKind.TIME, timeNeededMinutes = 60, completedAt = done)
        val p = calculator(listOf(goal)).progressOf(1)!!

        assertEquals(60, p.doneMinutes)
        assertEquals(0, p.leftMinutes)
        assertEquals(1f, p.fraction, 0.0001f)
        assertTrue(p.isComplete)
    }

    @Test
    fun number_goal_is_measured_in_units() {
        val goal = Goal(
            id = 1, name = "Learn words", kind = GoalKind.NUMBER, timeNeededMinutes = 2000,
            numberTarget = 1000, minutesPerUnit = 2, numberDone = 400
        )
        val p = calculator(listOf(goal)).progressOf(1)!!

        assertEquals(2000, p.neededMinutes)
        assertEquals(800, p.doneMinutes)
        assertEquals(1200, p.leftMinutes)
        assertEquals(1000, p.unitsTotal)
        assertEquals(400, p.unitsDone)
        assertEquals(600, p.unitsLeft)
        assertEquals(0.4f, p.fraction, 0.0001f)
    }

    @Test
    fun event_needs_as_much_time_as_it_lasts() {
        val event = Event(
            id = 1, name = "Serbian meet",
            start = Instant.ofEpochSecond(0), end = Instant.ofEpochSecond(3600)
        )
        val p = calculator(listOf(event)).progressOf(1)!!

        assertEquals(60, p.neededMinutes)
    }

    @Test
    fun child_that_counts_is_added_to_the_parent() {
        val parent = Goal(id = 1, name = "Parent", kind = GoalKind.TIME, timeNeededMinutes = 60)
        val child = Task(id = 2, parentId = 1, name = "Child", timeNeededMinutes = 30, countsTowardParent = true)
        val p = calculator(listOf(parent, child), mapOf(1L to 30, 2L to 30)).progressOf(1)!!

        assertEquals(90, p.neededMinutes)
        assertEquals(60, p.doneMinutes)
        assertEquals(30, p.leftMinutes)
    }

    @Test
    fun independent_child_is_not_added_to_the_parent() {
        val parent = Goal(id = 1, name = "Parent", kind = GoalKind.TIME, timeNeededMinutes = 60)
        val child = Task(id = 2, parentId = 1, name = "Child", timeNeededMinutes = 30, countsTowardParent = false)
        val p = calculator(listOf(parent, child), mapOf(1L to 30, 2L to 30)).progressOf(1)!!

        assertEquals(60, p.neededMinutes)
        assertEquals(30, p.doneMinutes)
    }

    @Test
    fun grandchildren_roll_up_through_children_that_count() {
        val top = Goal(id = 1, name = "Top", kind = GoalKind.TIME, timeNeededMinutes = 60)
        val middle = Goal(id = 2, parentId = 1, name = "Middle", kind = GoalKind.TIME, timeNeededMinutes = 60)
        val bottom = Task(id = 3, parentId = 2, name = "Bottom", timeNeededMinutes = 30)
        val p = calculator(listOf(top, middle, bottom), mapOf(3L to 30)).progressOf(1)!!

        assertEquals(150, p.neededMinutes)
        assertEquals(30, p.doneMinutes)
    }

    @Test
    fun independent_middle_entry_stops_the_roll_up() {
        val top = Goal(id = 1, name = "Top", kind = GoalKind.TIME, timeNeededMinutes = 60)
        val middle = Goal(
            id = 2, parentId = 1, name = "Middle", kind = GoalKind.TIME,
            timeNeededMinutes = 60, countsTowardParent = false
        )
        val bottom = Task(id = 3, parentId = 2, name = "Bottom", timeNeededMinutes = 30)
        val calculator = calculator(listOf(top, middle, bottom), mapOf(3L to 30))

        // The task still counts toward the middle goal ...
        assertEquals(90, calculator.progressOf(2)!!.neededMinutes)
        // ... but the top goal does not see any of it.
        assertEquals(60, calculator.progressOf(1)!!.neededMinutes)
        assertEquals(0, calculator.progressOf(1)!!.doneMinutes)
    }

    @Test
    fun repeating_entry_adds_done_time_to_the_parent_but_no_needed_time() {
        val parent = Goal(id = 1, name = "Parent", kind = GoalKind.TIME, timeNeededMinutes = 60)
        val repeating = Repeating(id = 2, parentId = 1, name = "Study 5 min")
        val p = calculator(listOf(parent, repeating), mapOf(2L to 20)).progressOf(1)!!

        assertEquals(60, p.neededMinutes)
        assertEquals(20, p.doneMinutes)
        assertEquals(40, p.leftMinutes)
    }

    @Test
    fun checked_off_child_counts_as_fully_done() {
        val parent = Goal(id = 1, name = "Parent", kind = GoalKind.TIME, timeNeededMinutes = 60)
        val child = Task(id = 2, parentId = 1, name = "Child", timeNeededMinutes = 30, completedAt = done)
        val p = calculator(listOf(parent, child)).progressOf(1)!!

        assertEquals(90, p.neededMinutes)
        assertEquals(30, p.doneMinutes)
    }

    @Test
    fun repeating_entry_has_no_progress_bar_of_its_own() {
        val repeating = Repeating(id = 1, name = "Study 5 min")
        val p = calculator(listOf(repeating), mapOf(1L to 20)).progressOf(1)!!

        assertEquals(0, p.neededMinutes)
        assertFalse(p.hasBar)
    }

    @Test
    fun unknown_id_gives_no_progress() {
        val task = Task(id = 1, name = "Buy milk")
        val calculator = calculator(listOf(task))

        assertNotNull(calculator.progressOf(1))
        assertNull(calculator.progressOf(99))
    }
}