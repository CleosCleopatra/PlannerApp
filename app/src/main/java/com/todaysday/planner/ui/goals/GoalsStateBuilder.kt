package com.todaysday.planner.ui.goals

import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.Event
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.RepeatType
import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.Task
import com.todaysday.planner.domain.progress.Progress
import com.todaysday.planner.domain.progress.ProgressCalculator
import com.todaysday.planner.domain.progress.formatMinutes
import com.todaysday.planner.domain.tree.DependencyGraph
import com.todaysday.planner.domain.tree.EntryTree
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/*
 * This file turns "the list of all entries" into "what the Goals screen shows".
 * It is plain Kotlin (no Android), so we can test it quickly on the computer.
 */

// Order inside a list: unfinished before finished, then events by start time, then by due
// date (no due date last), then by when the entry was created, then by name.
private val entryOrder: Comparator<Entry> =
    compareBy<Entry> { it.completedAt != null }
        .thenBy { (it as? Event)?.start ?: Instant.MAX }
        .thenBy { it.dueDate ?: LocalDate.MAX }
        .thenBy { it.createdAt }
        .thenBy { it.name.lowercase() }

/**
 * Builds the complete screen state.
 *
 * @param entries       all entries (finished ones too)
 * @param loggedMinutes minutes worked per entry id (empty until the work history exists, M13)
 * @param requirements  "requires completion of": entry id -> the ids it requires
 * @param selectedTab   which subtab is open
 * @param openGoalId    the goal we are inside (Goals subtab only), or null for the top level
 * @param zone, locale  used to write dates; tests set them so results are the same everywhere
 */
fun buildGoalsUiState(
    entries: List<Entry>,
    loggedMinutes: Map<Long, Int>,
    selectedTab: EntryType,
    openGoalId: Long?,
    requirements: Map<Long, Set<Long>> = emptyMap(),
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): GoalsUiState {
    val tree = EntryTree(entries)
    val calculator = ProgressCalculator(tree, loggedMinutes)
    val graph = DependencyGraph(tree, requirements)

    // The names of the unfinished entries this entry has to wait for, in alphabetical order.
    // A finished entry waits for nothing. Requirements on missing entries are ignored.
    fun waitingFor(entry: Entry): List<String> {
        if (entry.completedAt != null) return emptyList()
        return graph.waitsFor(entry.id)
            .mapNotNull { tree[it] }
            .filter { it.completedAt == null }
            .map { it.name }
            .sortedBy { it.lowercase() }
    }

    // Makes the card data for one entry.
    fun rowOf(entry: Entry, subtitle: String? = null): EntryRow {
        val progress = calculator.progressOf(entry)
        return EntryRow(
            entry = entry,
            detail = detailText(entry, zone, locale),
            subtitle = subtitle,
            showProgress = entry is Goal || entry is Task,
            fraction = progress.fraction,
            progressText = progressText(entry, progress),
            childCount = tree.childrenOf(entry.id).size,
            waitingFor = waitingFor(entry)
        )
    }

    // "Learn Serbian › Finishing my book": the names above an entry, or null at the top level.
    fun parentPath(entry: Entry): String? {
        val names = tree.pathTo(entry.id).dropLast(1).map { it.name }
        return if (names.isEmpty()) null else names.joinToString(" › ")
    }

    // A heading with the sorted cards of one kind.
    fun section(title: String, items: List<Entry>) =
        EntrySection(title, items.sortedWith(entryOrder).map { rowOf(it) })

    // Only a goal can be opened. If the id is missing or isn't a goal we show the top level.
    val openEntry = if (openGoalId != null) tree[openGoalId] else null
    val openGoal = openEntry as? Goal

    var openGoalRow: EntryRow? = null
    var breadcrumb = emptyList<String>()
    var parentOfOpenGoalId: Long? = null
    val sections: List<EntrySection>

    if (selectedTab == EntryType.GOAL) {
        if (openGoal == null) {
            // Top level of the Goals subtab: goals that are not part of another goal.
            val topGoals = tree.roots().filterIsInstance<Goal>()
            sections = listOf(EntrySection(null, topGoals.sortedWith(entryOrder).map { rowOf(it) }))
        } else {
            // Inside a goal: show what is in it, grouped by kind. Empty groups are left out.
            val path = tree.pathTo(openGoal.id)
            breadcrumb = path.map { it.name }
            parentOfOpenGoalId = path.dropLast(1).lastOrNull()?.id
            openGoalRow = rowOf(openGoal)

            val children = tree.childrenOf(openGoal.id)
            sections = listOf(
                section("Subgoals", children.filterIsInstance<Goal>()),
                section("Tasks", children.filterIsInstance<Task>()),
                section("Events", children.filterIsInstance<Event>()),
                section("Repeating", children.filterIsInstance<Repeating>())
            ).filter { it.rows.isNotEmpty() }
        }
    } else {
        // Tasks, Events and Repeating subtabs: a flat list of every entry of that kind.
        val ofThisType: List<Entry> = when (selectedTab) {
            EntryType.TASK -> entries.filterIsInstance<Task>()
            EntryType.EVENT -> entries.filterIsInstance<Event>()
            EntryType.REPEATING -> entries.filterIsInstance<Repeating>()
            EntryType.GOAL -> emptyList() // can't happen here, the Goals subtab is handled above
        }
        sections = listOf(
            EntrySection(null, ofThisType.sortedWith(entryOrder).map { rowOf(it, parentPath(it)) })
        )
    }

    return GoalsUiState(
        isLoading = false,
        selectedTab = selectedTab,
        openGoalId = openGoal?.id,
        parentOfOpenGoalId = parentOfOpenGoalId,
        breadcrumb = breadcrumb,
        openGoalRow = openGoalRow,
        sections = sections,
        totalEntries = entries.size
    )
}

/** The small text under the progress bar: "3 h 20 min left", "600 of 1000 left (20 h)", "Done". */
internal fun progressText(entry: Entry, progress: Progress): String = when {
    entry is Event || entry is Repeating -> ""
    progress.isComplete -> "Done"
    progress.unitsLeft != null && progress.unitsTotal != null ->
        "${progress.unitsLeft} of ${progress.unitsTotal} left (${formatMinutes(progress.leftMinutes)})"
    else -> "${formatMinutes(progress.leftMinutes)} left"
}

/** The grey line under the name: what kind of entry it is, when it happens, its due date. */
internal fun detailText(entry: Entry, zone: ZoneId, locale: Locale): String {
    val parts = mutableListOf<String>()

    when (entry) {
        is Goal -> parts += when (entry.kind) {
            GoalKind.TIME -> "Time goal"
            GoalKind.ACHIEVEMENT -> "Achievement goal"
            GoalKind.NUMBER -> "Number goal"
        }

        is Task -> parts += "Task"

        is Event -> {
            val format = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", locale).withZone(zone)
            parts += "Event " + format.format(entry.start)
        }

        is Repeating -> {
            parts += repeatText(entry, locale)
            parts += "${entry.sessionMinutes} min"
        }
    }

    entry.dueDate?.let { parts += "due $it" }
    return parts.joinToString(" · ")
}

private fun repeatText(entry: Repeating, locale: Locale): String {
    val days = entry.repeatWeekdays
    val everyDay = days.size == 7
    val dayNames = days.sortedBy { it.value }
        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }

    return when (entry.repeatType) {
        RepeatType.WEEKDAYS ->
            if (everyDay) "Repeats every day" else "Repeats on $dayNames"

        RepeatType.EVERY_N_DAYS ->
            if (entry.repeatN == 1) "Repeats every day" else "Repeats every ${entry.repeatN} days"

        RepeatType.WEEKDAYS_EVERY_N_WEEKS ->
            if (entry.repeatN == 1) "Repeats on $dayNames every week"
            else "Repeats on $dayNames every ${entry.repeatN} weeks"
    }
}