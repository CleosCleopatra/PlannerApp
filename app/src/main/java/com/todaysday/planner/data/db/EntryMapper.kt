package com.todaysday.planner.data.db

import com.todaysday.planner.data.db.entities.EntryEntity
import com.todaysday.planner.data.db.entities.PlaceEntity
import com.todaysday.planner.data.db.entities.TagEntity
import com.todaysday.planner.data.db.relations.EntryWithTags
import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.Event
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.Place
import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.RepeatType
import com.todaysday.planner.domain.model.Tag
import com.todaysday.planner.domain.model.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/*
 * Mappers translate between the two worlds:
 *   database rows (EntryEntity: flat, numbers and nulls)  <->  domain classes (Goal, Task, ...)
 * The rest of the app only works with the domain classes.
 */

// ---------- Weekdays <-> a single number ----------
// Monday is bit 1, Tuesday bit 2, Wednesday bit 4, ... so {Monday, Wednesday} is 1 + 4 = 5.

fun Set<DayOfWeek>.toMask(): Int =
    fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

fun Int.toWeekdays(): Set<DayOfWeek> =
    DayOfWeek.values().filter { day -> this and (1 shl (day.value - 1)) != 0 }.toSet()

// ---------- Database -> domain ----------

fun EntryWithTags.toDomain(): Entry {
    val e = entry
    val tagIds = tags.map { it.id }.toSet()
    val due = e.dueDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    val created = Instant.ofEpochMilli(e.createdAtMillis)
    val completed = e.completedAtMillis?.let { Instant.ofEpochMilli(it) }

    return when (e.type) {
        EntryType.GOAL -> Goal(
            id = e.id, parentId = e.parentId, name = e.name, description = e.description,
            priority = e.priority, effort = e.effort, dueDate = due, placeId = e.placeId,
            tagIds = tagIds, countsTowardParent = e.countsTowardParent,
            completedAt = completed, createdAt = created,
            kind = e.goalKind ?: GoalKind.ACHIEVEMENT,
            timeNeededMinutes = e.timeNeededMinutes ?: 60,
            numberTarget = e.numberTarget, minutesPerUnit = e.minutesPerUnit,
            numberDone = e.numberDone
        )

        EntryType.TASK -> Task(
            id = e.id, parentId = e.parentId, name = e.name, description = e.description,
            priority = e.priority, effort = e.effort, dueDate = due, placeId = e.placeId,
            tagIds = tagIds, countsTowardParent = e.countsTowardParent,
            completedAt = completed, createdAt = created,
            timeNeededMinutes = e.timeNeededMinutes ?: 10
        )

        EntryType.EVENT -> Event(
            id = e.id, parentId = e.parentId, name = e.name, description = e.description,
            priority = e.priority, effort = e.effort, dueDate = due, placeId = e.placeId,
            tagIds = tagIds, countsTowardParent = e.countsTowardParent,
            completedAt = completed, createdAt = created,
            start = Instant.ofEpochMilli(e.eventStartMillis ?: e.createdAtMillis),
            end = Instant.ofEpochMilli(e.eventEndMillis ?: e.createdAtMillis),
            externalId = e.externalId
        )

        EntryType.REPEATING -> Repeating(
            id = e.id, parentId = e.parentId, name = e.name, description = e.description,
            priority = e.priority, effort = e.effort, dueDate = due, placeId = e.placeId,
            tagIds = tagIds, countsTowardParent = e.countsTowardParent,
            completedAt = completed, createdAt = created,
            repeatType = e.repeatType ?: RepeatType.WEEKDAYS,
            repeatWeekdays = e.repeatWeekdaysMask.toWeekdays(),
            repeatN = e.repeatN,
            repeatAnchorDate = LocalDate.ofEpochDay(
                e.repeatAnchorEpochDay ?: LocalDate.now().toEpochDay()
            ),
            sessionMinutes = e.sessionMinutes ?: 5
        )
    }
}

// ---------- Domain -> database ----------
// Note: tags are NOT part of the row. They are saved separately with
// EntryDao.insertWithTags(entry.toEntity(), entry.tagIds).

fun Entry.toEntity(): EntryEntity {
    // First the columns every kind shares ...
    val base = EntryEntity(
        id = id,
        type = when (this) {
            is Goal -> EntryType.GOAL
            is Task -> EntryType.TASK
            is Event -> EntryType.EVENT
            is Repeating -> EntryType.REPEATING
        },
        parentId = parentId,
        name = name,
        description = description,
        priority = priority,
        effort = effort,
        dueDateEpochDay = dueDate?.toEpochDay(),
        placeId = placeId,
        countsTowardParent = countsTowardParent,
        createdAtMillis = createdAt.toEpochMilli(),
        completedAtMillis = completedAt?.toEpochMilli()
    )

    // ... then the columns that only this kind uses.
    return when (this) {
        is Goal -> base.copy(
            goalKind = kind,
            timeNeededMinutes = timeNeededMinutes,
            numberTarget = numberTarget,
            minutesPerUnit = minutesPerUnit,
            numberDone = numberDone
        )

        is Task -> base.copy(timeNeededMinutes = timeNeededMinutes)

        is Event -> base.copy(
            eventStartMillis = start.toEpochMilli(),
            eventEndMillis = end.toEpochMilli(),
            externalId = externalId
        )

        is Repeating -> base.copy(
            repeatType = repeatType,
            repeatWeekdaysMask = repeatWeekdays.toMask(),
            repeatN = repeatN,
            repeatAnchorEpochDay = repeatAnchorDate.toEpochDay(),
            sessionMinutes = sessionMinutes
        )
    }
}

// ---------- Tags and places ----------

fun TagEntity.toDomain() = Tag(id = id, name = name)
fun Tag.toEntity() = TagEntity(id = id, name = name)

fun PlaceEntity.toDomain() = Place(id = id, name = name)
fun Place.toEntity() = PlaceEntity(id = id, name = name)