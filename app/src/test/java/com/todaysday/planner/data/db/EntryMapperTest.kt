package com.todaysday.planner.data.db

import com.todaysday.planner.data.db.entities.TagEntity
import com.todaysday.planner.data.db.relations.EntryWithTags
import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.Event
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.Level
import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.RepeatType
import com.todaysday.planner.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Checks that turning an entry into a database row and back gives exactly the same entry.
 * If a field were forgotten in the mapper, one of these tests would fail.
 */
class EntryMapperTest {

    // Whole milliseconds only, because the database stores milliseconds.
    private val created = Instant.ofEpochMilli(1_700_000_000_000)

    // Convert to a row and back, pretending the given tags are attached.
    private fun roundTrip(entry: Entry, tags: List<TagEntity> = emptyList()): Entry =
        EntryWithTags(entry = entry.toEntity(), tags = tags).toDomain()

    @Test
    fun goal_survives_round_trip() {
        val goal = Goal(
            id = 1, parentId = 2, name = "Learn 1000 words", description = "Serbian",
            priority = Level.HIGH, effort = Level.LOW, dueDate = LocalDate.of(2026, 12, 24),
            placeId = 3, countsTowardParent = false, createdAt = created,
            kind = GoalKind.NUMBER, timeNeededMinutes = 500,
            numberTarget = 1000, minutesPerUnit = 2, numberDone = 40
        )
        assertEquals(goal, roundTrip(goal))
    }

    @Test
    fun task_survives_round_trip() {
        val task = Task(id = 4, name = "Buy milk", timeNeededMinutes = 15, createdAt = created)
        assertEquals(task, roundTrip(task))
    }

    @Test
    fun event_survives_round_trip() {
        val event = Event(
            id = 5, name = "Serbian meet", createdAt = created,
            start = Instant.ofEpochMilli(1_700_100_000_000),
            end = Instant.ofEpochMilli(1_700_103_600_000),
            externalId = "calendar-123"
        )
        assertEquals(event, roundTrip(event))
    }

    @Test
    fun repeating_survives_round_trip() {
        val repeating = Repeating(
            id = 6, name = "Study 5 min Serbian", createdAt = created,
            repeatType = RepeatType.WEEKDAYS_EVERY_N_WEEKS,
            repeatWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY),
            repeatN = 2, repeatAnchorDate = LocalDate.of(2026, 10, 5), sessionMinutes = 5
        )
        assertEquals(repeating, roundTrip(repeating))
    }

    @Test
    fun tag_ids_come_from_the_attached_tags() {
        val task = Task(id = 7, name = "Practice", createdAt = created)
        val loaded = roundTrip(task, listOf(TagEntity(id = 5, name = "Serbian")))
        assertEquals(task.copy(tagIds = setOf(5L)), loaded)
    }

    @Test
    fun weekday_mask_round_trip() {
        val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)
        assertEquals(2 + 16, days.toMask())
        assertEquals(days, days.toMask().toWeekdays())
    }
}