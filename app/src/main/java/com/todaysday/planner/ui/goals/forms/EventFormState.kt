package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Event
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** The error messages of the event form. null means "this field is fine". */
data class EventFormErrors(
    val name: String? = null,
    val time: String? = null
) {
    val hasErrors: Boolean get() = name != null || time != null
}

/**
 * Everything typed into the event form.
 *
 * Start and end are kept as local date-and-time ("18:00 on 17 October", as on the clock on
 * the wall). They are turned into exact moments (Instant) only when saving, using the time zone.
 */
data class EventFormState(
    val original: Event? = null,
    val parentId: Long? = null,
    val parentName: String? = null,
    val name: String = "",
    val description: String = "",
    // These two are only placeholders: forNew and forEvent set the real values.
    val start: LocalDateTime = LocalDateTime.of(2000, 1, 1, 9, 0),
    val end: LocalDateTime = LocalDateTime.of(2000, 1, 1, 10, 0),
    val countsTowardParent: Boolean = true,
    val tagIds: Set<Long> = emptySet(),
    val placeId: Long? = null,          // null = Anywhere
    val isLoaded: Boolean = false,
    val showErrors: Boolean = false
) {
    val isEditing: Boolean get() = original != null

    /** Moves the start and moves the end with it, so the event keeps its length. */
    fun withStart(newStart: LocalDateTime): EventFormState {
        val length = Duration.between(start, end)
        val keptLength = if (length.isNegative) Duration.ofHours(1) else length
        return copy(start = newStart, end = newStart.plus(keptLength))
    }

    fun errors() = EventFormErrors(
        name = if (name.isBlank()) "Enter a name" else null,
        time = if (!end.isAfter(start)) "The event must end after it starts" else null
    )

    /** The finished Event, or null if the form has errors. */
    fun toEvent(
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now()
    ): Event? {
        if (errors().hasErrors) return null

        val startMoment = start.atZone(zone).toInstant()
        val endMoment = end.atZone(zone).toInstant()

        // When editing we start from the original, so things the form doesn't show
        // (id, creation time, calendar id, ...) are kept as they were.
        val base = original ?: Event(name = name, start = startMoment, end = endMoment, createdAt = now)

        return base.copy(
            parentId = parentId,
            name = name.trim(),
            description = description.trim(),
            countsTowardParent = countsTowardParent,
            tagIds = tagIds,
            placeId = placeId,
            start = startMoment,
            end = endMoment
        )
    }

    companion object {
        /** An empty form: starts at the next full hour and lasts one hour. */
        fun forNew(
            parentId: Long?,
            parentName: String?,
            now: LocalDateTime = LocalDateTime.now()
        ): EventFormState {
            val start = now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
            return EventFormState(
                parentId = parentId,
                parentName = parentName,
                start = start,
                end = start.plusHours(1),
                isLoaded = true
            )
        }

        /** A form filled in from an existing event, shown in the given time zone. */
        fun forEvent(
            event: Event,
            parentName: String?,
            zone: ZoneId = ZoneId.systemDefault()
        ) = EventFormState(
            original = event,
            parentId = event.parentId,
            parentName = parentName,
            name = event.name,
            description = event.description,
            start = LocalDateTime.ofInstant(event.start, zone),
            end = LocalDateTime.ofInstant(event.end, zone),
            countsTowardParent = event.countsTowardParent,
            tagIds = event.tagIds,
            placeId = event.placeId,
            isLoaded = true
        )
    }
}