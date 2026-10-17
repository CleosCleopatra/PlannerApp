package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Checks the rules of the task form. */
class TaskFormStateTest {

    private val now = Instant.ofEpochSecond(100)

    @Test
    fun a_name_alone_is_enough_and_takes_ten_minutes() {
        val task = TaskFormState.forNew(null, null).copy(name = "Buy milk").toTask(now)!!

        assertEquals("Buy milk", task.name)
        assertEquals(10, task.timeNeededMinutes)
        assertNull(task.parentId)
    }

    @Test
    fun name_is_required() {
        assertNotNull(TaskFormState.forNew(null, null).errors().name)
        assertNotNull(TaskFormState.forNew(null, null).copy(name = "  ").errors().name)
        assertNull(TaskFormState.forNew(null, null).toTask(now))
    }

    @Test
    fun time_must_be_more_than_zero() {
        val form = TaskFormState.forNew(null, null).copy(name = "x")

        assertNotNull(form.copy(timeMinutes = 0).errors().time)
        assertNotNull(form.copy(timeMinutes = null).errors().time)
        assertNull(form.copy(timeMinutes = 5).errors().time)
    }

    @Test
    fun extras_are_closed_for_new_tasks_and_open_when_editing() {
        assertFalse(TaskFormState.forNew(null, null).showExtras)
        assertTrue(TaskFormState.forTask(Task(id = 1, name = "x"), null).showExtras)
    }

    @Test
    fun new_task_goes_into_the_given_goal() {
        val task = TaskFormState.forNew(parentId = 7, parentName = "Learn Serbian")
            .copy(name = "Find tutor")
            .toTask(now)!!

        assertEquals(7L, task.parentId)
        assertTrue(task.countsTowardParent)
    }

    @Test
    fun editing_keeps_what_the_form_does_not_show() {
        val original = Task(
            id = 5, name = "Old", parentId = 9, placeId = 3, tagIds = setOf(1L),
            createdAt = Instant.ofEpochSecond(7), timeNeededMinutes = 15
        )
        val saved = TaskFormState.forTask(original, "Parent")
            .copy(name = " New ", timeMinutes = 20)
            .toTask(now)!!

        assertEquals(5L, saved.id)
        assertEquals("New", saved.name)
        assertEquals(20, saved.timeNeededMinutes)
        assertEquals(9L, saved.parentId)
        assertEquals(3L, saved.placeId)
        assertEquals(setOf(1L), saved.tagIds)
        assertEquals(Instant.ofEpochSecond(7), saved.createdAt)
    }

    @Test
    fun marking_done_sets_the_finish_time_and_unmarking_clears_it() {
        val done = TaskFormState.forNew(null, null).copy(name = "x", isDone = true).toTask(now)!!
        assertEquals(now, done.completedAt)

        val finished = Task(id = 1, name = "x", completedAt = Instant.ofEpochSecond(50))
        val reopened = TaskFormState.forTask(finished, null).copy(isDone = false).toTask(now)!!
        assertNull(reopened.completedAt)
    }
}