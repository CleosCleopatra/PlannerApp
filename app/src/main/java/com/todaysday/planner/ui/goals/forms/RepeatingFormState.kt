package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.RepeatType
import com.todaysday.planner.domain.repeat.RepeatExpander
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** The error messages of the repeating form. null means "this field is fine". */
data class RepeatingFormErrors(
    val name: String? = null,
    val days: String? = null,
    val interval: String? = null,
    val time: String? = null
) {
    val hasErrors: Boolean
        get() = name != null || days != null || interval != null || time != null
}

/**
 * Everything typed into the repeating form.
 *
 * Which fields matter depends on the pattern:
 * - WEEKDAYS:               weekdays
 * - EVERY_N_DAYS:           everyN
 * - WEEKDAYS_EVERY_N_WEEKS: weekdays and everyN
 * Fields that don't matter for the chosen pattern are cleared when saving.
 */
data class RepeatingFormState(
    val original: Repeating? = null,
    val parentId: Long? = null,
    val parentName: String? = null,
    val name: String = "",
    val description: String = "",
    val repeatType: RepeatType = RepeatType.WEEKDAYS,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val everyN: Int? = 1,
    // Only a placeholder: forNew and forRepeating set the real date.
    val startDate: LocalDate = LocalDate.of(2000, 1, 1),
    val sessionMinutes: Int? = 5,      // how long each occurrence takes
    val countsTowardParent: Boolean = true,
    val tagIds: Set<Long> = emptySet(),
    val placeId: Long? = null,         // null = Anywhere
    val isDone: Boolean = false,       // "stopped": no more repeats
    val isLoaded: Boolean = false,
    val showErrors: Boolean = false
) {
    val isEditing: Boolean get() = original != null

    /** Does the chosen pattern use the weekday buttons? */
    val usesWeekdays: Boolean get() = repeatType != RepeatType.EVERY_N_DAYS

    /** Does the chosen pattern use the "every N" box? */
    val usesInterval: Boolean get() = repeatType != RepeatType.WEEKDAYS

    fun errors() = RepeatingFormErrors(
        name = if (name.isBlank()) "Enter a name" else null,
        days = if (usesWeekdays && weekdays.isEmpty()) "Choose at least one day" else null,
        interval = if (usesInterval && (everyN ?: 0) < 1) "Enter 1 or more" else null,
        time = if ((sessionMinutes ?: 0) <= 0) "Enter how long each time takes" else null
    )

    /** The pattern with the unused fields cleared, or null while the pattern is incomplete. */
    private fun patternOrNull(): Repeating? {
        val e = errors()
        if (e.days != null || e.interval != null) return null
        return Repeating(
            name = "",
            repeatType = repeatType,
            repeatWeekdays = if (usesWeekdays) weekdays else emptySet(),
            repeatN = if (usesInterval) (everyN ?: 1) else 1,
            repeatAnchorDate = startDate
        )
    }

    /**
     * The next dates on which this would happen, from [today] on. Empty while the pattern
     * is incomplete. Used for the "Next: ..." line under the form.
     */
    fun upcomingDates(today: LocalDate, count: Int = 3): List<LocalDate> {
        val pattern = patternOrNull() ?: return emptyList()
        val dates = mutableListOf<LocalDate>()
        var from = today
        while (dates.size < count) {
            val next = RepeatExpander.nextOccurrence(pattern, from) ?: break
            dates += next
            from = next.plusDays(1)
        }
        return dates
    }

    /** The finished Repeating, or null if the form has errors. */
    fun toRepeating(now: Instant = Instant.now()): Repeating? {
        if (errors().hasErrors) return null

        // When editing we start from the original, so things the form doesn't show
        // (id, creation time, ...) are kept as they were.
        val base = original ?: Repeating(name = name, createdAt = now)

        return base.copy(
            parentId = parentId,
            name = name.trim(),
            description = description.trim(),
            countsTowardParent = countsTowardParent,
            tagIds = tagIds,
            placeId = placeId,
            completedAt = if (isDone) (original?.completedAt ?: now) else null,
            repeatType = repeatType,
            repeatWeekdays = if (usesWeekdays) weekdays else emptySet(),
            repeatN = if (usesInterval) (everyN ?: 1) else 1,
            repeatAnchorDate = startDate,
            sessionMinutes = sessionMinutes ?: base.sessionMinutes
        )
    }

    companion object {
        /** An empty form: starts today, takes 5 minutes each time. */
        fun forNew(
            parentId: Long?,
            parentName: String?,
            today: LocalDate = LocalDate.now()
        ) = RepeatingFormState(
            parentId = parentId,
            parentName = parentName,
            startDate = today,
            isLoaded = true
        )

        /** A form filled in from an existing repeating entry. */
        fun forRepeating(entry: Repeating, parentName: String?) = RepeatingFormState(
            original = entry,
            parentId = entry.parentId,
            parentName = parentName,
            name = entry.name,
            description = entry.description,
            repeatType = entry.repeatType,
            weekdays = entry.repeatWeekdays,
            everyN = entry.repeatN,
            startDate = entry.repeatAnchorDate,
            sessionMinutes = entry.sessionMinutes,
            countsTowardParent = entry.countsTowardParent,
            tagIds = entry.tagIds,
            placeId = entry.placeId,
            isDone = entry.completedAt != null,
            isLoaded = true
        )
    }
}