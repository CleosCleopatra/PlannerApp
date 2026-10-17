package com.todaysday.planner.ui.goals

import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType

/** Everything needed to draw one entry on the screen (a "card"). */
data class EntryRow(
    val entry: Entry,
    val detail: String,         // e.g. "Time goal · due 2026-12-24"
    val subtitle: String?,      // e.g. the path "Learn Serbian › Finishing my book"
    val showProgress: Boolean,  // draw a progress bar?
    val fraction: Float,        // 0.0 to 1.0, how much of the bar is filled
    val progressText: String,   // e.g. "3 h 20 min left"
    val childCount: Int,        // how many entries are directly inside it
    val waitingFor: List<String> = emptyList() // names of unfinished entries it waits for
) {
    /** True while something this entry requires (itself or through a parent goal) is unfinished. */
    val isBlocked: Boolean
        get() = waitingFor.isNotEmpty()
}

/** A group of cards with an optional heading, e.g. "Tasks". */
data class EntrySection(
    val title: String?,
    val rows: List<EntryRow>
)

/** Everything the Goals screen needs to know in order to draw itself. */
data class GoalsUiState(
    val isLoading: Boolean = true,
    val selectedTab: EntryType = EntryType.GOAL,
    val openGoalId: Long? = null,          // the goal we are "inside", or null at the top level
    val parentOfOpenGoalId: Long? = null,  // where the Back button goes
    val breadcrumb: List<String> = emptyList(),
    val openGoalRow: EntryRow? = null,     // the card of the open goal, shown at the top
    val sections: List<EntrySection> = emptyList(),
    val totalEntries: Int = 0              // how many entries exist in total
) {
    /** True when there is nothing to list. */
    val isEmpty: Boolean
        get() = sections.all { it.rows.isEmpty() }
}