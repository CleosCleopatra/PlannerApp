package com.todaysday.planner.domain.progress

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun zero_minutes() = assertEquals("0 min", formatMinutes(0))

    @Test
    fun less_than_an_hour() = assertEquals("45 min", formatMinutes(45))

    @Test
    fun whole_hours() = assertEquals("1 h", formatMinutes(60))

    @Test
    fun hours_and_minutes() = assertEquals("3 h 20 min", formatMinutes(200))

    @Test
    fun negative_time_is_shown_as_zero() = assertEquals("0 min", formatMinutes(-5))
}