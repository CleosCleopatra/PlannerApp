package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Checks the rules of the event form. */
class EventFormStateTest {

    private val now = Instant.ofEpochSecond(100)
    private val utc = ZoneOffset.UTC

    // A valid form: 18:00 to 19:30 on 17 October 2026.
    private fun validForm() = EventFormState(
        name = "Serbian meet",
        start = LocalDateTime.of(2026, 10, 17, 18, 0),
        end = LocalDateTime.of(2026, 10, 17, 19, 30),
        isLoaded = true
    )

    @Test
    fun name_is_required() {
        assertNull(validForm().errors().name)
        assertNotNull(validForm().copy(name = "  ").errors().name)
    }

    @Test
    fun event_must_end_after_it_starts() {
        val sameTime = validForm().copy(end = LocalDateTime.of(2026, 10, 17, 18, 0))
        val earlier = validForm().copy(end = LocalDateTime.of(2026, 10, 17, 17, 0))

        assertNotNull(sameTime.errors().time)
        assertNotNull(earlier.errors().time)
        assertNull(validForm().errors().time)
    }

    @Test
    fun invalid_form_gives_no_event() {
        assertNull(validForm().copy(name = "").toEvent(utc, now))
    }

    @Test
    fun changing_the_start_keeps_the_length_of_the_event() {
        val moved = validForm().withStart(LocalDateTime.of(2026, 10, 20, 9, 0))

        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 0), moved.start)
        assertEquals(LocalDateTime.of(2026, 10, 20, 10, 30), moved.end) // still 1 h 30 min
    }

    @Test
    fun new_event_starts_at_the_next_full_hour_and_lasts_an_hour() {
        val form = EventFormState.forNew(null, null, now = LocalDateTime.of(2026, 10, 17, 17, 30))

        assertEquals(LocalDateTime.of(2026, 10, 17, 18, 0), form.start)
        assertEquals(LocalDateTime.of(2026, 10, 17, 19, 0), form.end)
        assertTrue(form.isLoaded)
    }

    @Test
    fun times_are_turned_into_exact_moments_using_the_time_zone() {
        val inUtc = validForm().toEvent(utc, now)!!
        assertEquals(Instant.parse("2026-10-17T18:00:00Z"), inUtc.start)
        assertEquals(Instant.parse("2026-10-17T19:30:00Z"), inUtc.end)

        // The same wall-clock time two hours east of UTC is two hours earlier in UTC.
        val inPlusTwo = validForm().toEvent(ZoneOffset.ofHours(2), now)!!
        assertEquals(Instant.parse("2026-10-17T16:00:00Z"), inPlusTwo.start)
    }

    @Test
    fun loading_an_event_shows_its_times_in_the_given_zone() {
        val event = Event(
            id = 1, name = "x",
            start = Instant.parse("2026-10-17T18:00:00Z"),
            end = Instant.parse("2026-10-17T19:00:00Z")
        )

        assertEquals(LocalDateTime.of(2026, 10, 17, 18, 0), EventFormState.forEvent(event, null, utc).start)
        assertEquals(
            LocalDateTime.of(2026, 10, 17, 20, 0),
            EventFormState.forEvent(event, null, ZoneOffset.ofHours(2)).start
        )
    }

    @Test
    fun editing_keeps_what_the_form_does_not_show() {
        val original = Event(
            id = 5, name = "Old", parentId = 9, externalId = "calendar-1",
            completedAt = Instant.ofEpochSecond(50), createdAt = Instant.ofEpochSecond(7),
            start = Instant.parse("2026-10-17T18:00:00Z"), end = Instant.parse("2026-10-17T19:00:00Z")
        )
        val saved = EventFormState.forEvent(original, "Parent", utc)
            .copy(name = " New ")
            .toEvent(utc, now)!!

        assertEquals(5L, saved.id)
        assertEquals("New", saved.name)
        assertEquals("calendar-1", saved.externalId)
        assertEquals(Instant.ofEpochSecond(7), saved.createdAt)
        assertEquals(Instant.ofEpochSecond(50), saved.completedAt)
    }

    @Test
    fun new_event_goes_into_the_given_goal_with_its_tags_and_place() {
        val event = EventFormState.forNew(parentId = 4, parentName = "Learn Serbian")
            .copy(
                name = "Serbian meet", tagIds = setOf(1L), placeId = 2,
                start = LocalDateTime.of(2026, 10, 17, 18, 0),
                end = LocalDateTime.of(2026, 10, 17, 19, 0)
            )
            .toEvent(utc, now)!!

        assertEquals(4L, event.parentId)
        assertEquals(setOf(1L), event.tagIds)
        assertEquals(2L, event.placeId)
        assertTrue(event.countsTowardParent)
    }
}