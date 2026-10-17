package com.todaysday.planner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the default values of the entry classes.
 * This is a plain "unit test": it runs on your computer in a second, no phone needed.
 */
class EntryDefaultsTest {

    @Test
    fun task_takes_ten_minutes_by_default() {
        assertEquals(10, Task(name = "Buy milk").timeNeededMinutes)
    }

    @Test
    fun entry_without_place_means_anywhere() {
        assertNull(Task(name = "Buy milk").placeId)
    }

    @Test
    fun subentries_count_toward_parent_by_default() {
        assertTrue(Goal(name = "Learn Serbian").countsTowardParent)
    }

    @Test
    fun repeating_entry_takes_five_minutes_by_default() {
        assertEquals(5, Repeating(name = "Study Serbian").sessionMinutes)
    }
}