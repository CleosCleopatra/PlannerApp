package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Checks the rules of the goal form: what counts as valid, and what gets saved. */
class GoalFormStateTest {

    private val now = Instant.ofEpochSecond(100)

    // A form that is valid as it stands: a time goal of 10 hours.
    private fun validTimeGoal() = GoalFormState(
        name = "Learn Serbian", kind = GoalKind.TIME, timeMinutes = 600, isLoaded = true
    )

    private fun validNumberGoal() = GoalFormState(
        name = "Words", kind = GoalKind.NUMBER, numberTarget = 1000, minutesPerUnit = 2, isLoaded = true
    )

    @Test
    fun name_is_required() {
        assertNull(validTimeGoal().errors().name)
        assertNotNull(validTimeGoal().copy(name = "").errors().name)
        assertNotNull(validTimeGoal().copy(name = "   ").errors().name)
    }

    @Test
    fun time_goal_needs_a_time() {
        assertNotNull(validTimeGoal().copy(timeMinutes = null).errors().time)
        assertNotNull(validTimeGoal().copy(timeMinutes = 0).errors().time)
        assertNull(validTimeGoal().errors().time)
    }

    @Test
    fun achievement_goal_needs_an_estimate() {
        val achievement = validTimeGoal().copy(kind = GoalKind.ACHIEVEMENT)

        assertNull(achievement.errors().time)
        assertNotNull(achievement.copy(timeMinutes = null).errors().time)
    }

    @Test
    fun number_goal_needs_a_total_and_a_time_each() {
        val errors = validNumberGoal().copy(numberTarget = null, minutesPerUnit = 0).errors()

        assertNotNull(errors.numberTarget)
        assertNotNull(errors.minutesPerUnit)
        assertNull(errors.time) // the plain time field is not used for number goals
    }

    @Test
    fun number_goal_time_is_worked_out_from_the_two_fields() {
        assertEquals(2000, validNumberGoal().totalMinutes)
        assertEquals(2000, validNumberGoal().toGoal(now)!!.timeNeededMinutes)
    }

    @Test
    fun number_done_cannot_be_more_than_the_total() {
        assertNotNull(validNumberGoal().copy(numberDone = 1001).errors().numberDone)
        assertNull(validNumberGoal().copy(numberDone = 1000).errors().numberDone)
    }

    @Test
    fun invalid_form_gives_no_goal() {
        assertNull(validTimeGoal().copy(name = "").toGoal(now))
    }

    @Test
    fun valid_form_builds_a_goal_with_trimmed_text() {
        val goal = validTimeGoal().copy(
            name = "  Learn Serbian ", description = " daily ", priority = Level.HIGH,
            effort = Level.LOW, dueDate = LocalDate.of(2026, 12, 24), countsTowardParent = false
        ).toGoal(now)!!

        assertEquals("Learn Serbian", goal.name)
        assertEquals("daily", goal.description)
        assertEquals(Level.HIGH, goal.priority)
        assertEquals(Level.LOW, goal.effort)
        assertEquals(LocalDate.of(2026, 12, 24), goal.dueDate)
        assertEquals(false, goal.countsTowardParent)
        assertEquals(600, goal.timeNeededMinutes)
        assertEquals(now, goal.createdAt)
    }

    @Test
    fun new_goal_goes_into_the_given_parent() {
        val form = GoalFormState.forNew(parentId = 4, parentName = "Learn Serbian")
            .copy(name = "Book", timeMinutes = 60)

        assertEquals(4L, form.toGoal(now)!!.parentId)
        assertTrue(form.countsTowardParent) // counts toward the parent by default
    }

    @Test
    fun editing_keeps_what_the_form_does_not_show() {
        val original = Goal(
            id = 5, name = "Old", parentId = 9, placeId = 3, tagIds = setOf(1L, 2L),
            createdAt = Instant.ofEpochSecond(7), timeNeededMinutes = 60
        )
        val saved = GoalFormState.forGoal(original, parentName = "Parent")
            .copy(name = " New ")
            .toGoal(now)!!

        assertEquals(5L, saved.id)
        assertEquals("New", saved.name)
        assertEquals(9L, saved.parentId)
        assertEquals(3L, saved.placeId)
        assertEquals(setOf(1L, 2L), saved.tagIds)
        assertEquals(Instant.ofEpochSecond(7), saved.createdAt)
    }

    @Test
    fun marking_done_sets_the_finish_time_and_unmarking_clears_it() {
        val done = validTimeGoal().copy(isDone = true).toGoal(now)!!
        assertEquals(now, done.completedAt)

        // An already finished goal keeps its original finish time.
        val earlier = Goal(id = 1, name = "x", completedAt = Instant.ofEpochSecond(50))
        val stillDone = GoalFormState.forGoal(earlier, null).toGoal(now)!!
        assertEquals(Instant.ofEpochSecond(50), stillDone.completedAt)

        val notDone = GoalFormState.forGoal(earlier, null).copy(isDone = false).toGoal(now)!!
        assertNull(notDone.completedAt)
    }

    @Test
    fun switching_from_number_goal_to_time_goal_clears_the_number_fields() {
        val numberGoal = Goal(
            id = 1, name = "Words", kind = GoalKind.NUMBER, timeNeededMinutes = 2000,
            numberTarget = 1000, minutesPerUnit = 2, numberDone = 400
        )
        val saved = GoalFormState.forGoal(numberGoal, null)
            .copy(kind = GoalKind.TIME, timeMinutes = 120)
            .toGoal(now)!!

        assertEquals(GoalKind.TIME, saved.kind)
        assertEquals(120, saved.timeNeededMinutes)
        assertNull(saved.numberTarget)
        assertNull(saved.minutesPerUnit)
        assertEquals(0, saved.numberDone)
    }

    @Test
    fun editing_a_number_goal_keeps_its_progress() {
        val numberGoal = Goal(
            id = 1, name = "Words", kind = GoalKind.NUMBER, timeNeededMinutes = 2000,
            numberTarget = 1000, minutesPerUnit = 2, numberDone = 400
        )
        val saved = GoalFormState.forGoal(numberGoal, null).toGoal(now)!!

        assertEquals(400, saved.numberDone)
        assertEquals(1000, saved.numberTarget)
    }
}