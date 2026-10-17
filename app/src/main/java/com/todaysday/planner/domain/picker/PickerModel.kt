package com.todaysday.planner.domain.picker

import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.entryType
import com.todaysday.planner.domain.tree.EntryTree

/**
 * What a picker menu should show.
 *
 * @param allowedTypes which kinds can appear (for example only goals when choosing "part of")
 * @param hideCompleted finished entries don't appear (this is what you want almost everywhere)
 * @param hideBlocked  entries that can't be started yet don't appear
 * @param disabledIds  entries that are shown but can't be chosen (for example to prevent loops)
 */
data class PickerOptions(
    val allowedTypes: Set<EntryType> = EntryType.entries.toSet(),
    val hideCompleted: Boolean = true,
    val hideBlocked: Boolean = false,
    val disabledIds: Set<Long> = emptySet()
)

/** One row of the picker. */
data class PickerItem(
    val entry: Entry,
    val selectable: Boolean,   // false = greyed out, can't be chosen
    val hasChildren: Boolean   // true = there is something to open inside it
)

/**
 * The menu logic: start at the top-level entries and click your way down into subgoals.
 * Plain Kotlin, so it can be tested without a phone.
 *
 * @param entries    ALL entries
 * @param blockedIds the ids that can't be started yet (from DependencyGraph.blockedIds())
 */
class PickerModel(
    entries: List<Entry>,
    private val blockedIds: Set<Long> = emptySet(),
    private val options: PickerOptions = PickerOptions()
) {
    // When finished entries are hidden, their unfinished children move up (see EntryTree).
    private val tree: EntryTree = EntryTree(entries).let {
        if (options.hideCompleted) it.withoutCompleted() else it
    }

    private val byName: Comparator<Entry> = compareBy { it.name.lowercase() }

    private fun isVisible(entry: Entry): Boolean =
        entry.entryType in options.allowedTypes &&
                !(options.hideBlocked && entry.id in blockedIds)

    private fun visibleChildren(openId: Long?): List<Entry> {
        val children = if (openId == null) tree.roots() else tree.childrenOf(openId)
        return children.filter { isVisible(it) }.sortedWith(byName)
    }

    /** The rows to show: the top level when [openId] is null, otherwise what is inside that goal. */
    fun itemsIn(openId: Long?): List<PickerItem> = visibleChildren(openId).map { entry ->
        PickerItem(
            entry = entry,
            selectable = entry.id !in options.disabledIds,
            hasChildren = visibleChildren(entry.id).isNotEmpty()
        )
    }

    /** The path from the top down to the open goal, for the line at the top of the menu. */
    fun pathTo(openId: Long?): List<Entry> =
        if (openId == null) emptyList() else tree.pathTo(openId)

    /** Where the Back button goes from the open goal (null = the top level). */
    fun parentOf(openId: Long?): Long? =
        if (openId == null) null else tree.pathTo(openId).dropLast(1).lastOrNull()?.id
}