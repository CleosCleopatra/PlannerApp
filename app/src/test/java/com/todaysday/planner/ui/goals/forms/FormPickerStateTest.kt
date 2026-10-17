package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** Checks that tags, place and requirements flow through the forms correctly. */
class FormPickerStateTest {

    private val now = Instant.ofEpochSecond(100)

    @Test
    fun goal_form_saves_its_tags_and_place() {
        val goal = GoalFormState(
            name = "Learn Serbian", timeMinutes = 60,
            tagIds = setOf(1L, 2L), placeId = 5, isLoaded = true
        ).toGoal(now)!!

        assertEquals(setOf(1L, 2L), goal.tagIds)
        assertEquals(5L, goal.placeId)
    }

    @Test
    fun task_form_saves_its_tags_and_place() {
        val task = TaskFormState(
            name = "Buy milk", tagIds = setOf(3L), placeId = 4, isLoaded = true
        ).toTask(now)!!

        assertEquals(setOf(3L), task.tagIds)
        assertEquals(4L, task.placeId)
    }

    @Test
    fun place_can_be_set_back_to_anywhere() {
        val original = Task(id = 1, name = "x", placeId = 3)
        val saved = TaskFormState.forTask(original, null).copy(placeId = null).toTask(now)!!

        assertNull(saved.placeId)
    }

    @Test
    fun editing_a_goal_loads_its_requirements_into_the_form() {
        val form = GoalFormState.forGoal(Goal(id = 1, name = "x"), parentName = null, requiredIds = setOf(9L))

        assertEquals(setOf(9L), form.requiredIds)
    }

    @Test
    fun editing_a_task_loads_its_requirements_into_the_form() {
        val form = TaskFormState.forTask(Task(id = 1, name = "x"), parentName = null, requiredIds = setOf(7L, 8L))

        assertEquals(setOf(7L, 8L), form.requiredIds)
    }
}