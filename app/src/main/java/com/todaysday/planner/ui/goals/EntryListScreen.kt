package com.todaysday.planner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import com.todaysday.planner.ui.common.ProgressBarWithText

/**
 * Draws the list under the subtabs: a path line, the open goal, and the sections of cards.
 *
 * Tapping a card:
 * - goal: opens it (and its pencil icon edits it)
 * - task: edits it
 * - event, repeating: nothing yet (their forms arrive in M6)
 */
@Composable
fun EntryListScreen(
    state: GoalsUiState,
    onGoalClick: (Long) -> Unit,
    onEdit: (Entry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            // The big bottom padding keeps the last card from hiding behind the + button.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.breadcrumb.isNotEmpty()) {
                item(key = "breadcrumb") {
                    Text(
                        text = state.breadcrumb.joinToString(" › "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            state.openGoalRow?.let { row ->
                item(key = "open-goal") {
                    EntryCard(row = row, onClick = null, onEdit = { onEdit(row.entry) })
                }
            }

            state.sections.forEach { section ->
                val title = section.title
                if (title != null) {
                    item(key = "header-$title") {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                items(section.rows, key = { it.entry.id }) { row ->
                    val entry = row.entry
                    when (entry) {
                        is Goal -> EntryCard(
                            row = row,
                            onClick = { onGoalClick(entry.id) },
                            onEdit = { onEdit(entry) }
                        )
                        else -> EntryCard(row = row, onClick = { onEdit(entry) }, onEdit = null)
                    }
                }
            }

            if (state.isEmpty) {
                item(key = "empty") { EmptyMessage(state) }
            }
        }
    }
}

@Composable
private fun EmptyMessage(state: GoalsUiState) {
    val text = when {
        state.openGoalId != null -> "Nothing inside this goal yet. Tap + to add something."
        state.selectedTab == EntryType.GOAL -> "No goals yet. Tap + to add one."
        state.selectedTab == EntryType.TASK -> "No tasks yet. Tap + to add one."
        state.selectedTab == EntryType.EVENT -> "No events yet. Tap + to add one."
        else -> "No repeating entries yet. Tap + to add one."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    )
}

/** One entry as a card. A null onClick means the card can't be tapped; a null onEdit hides the pencil. */
@Composable
private fun EntryCard(
    row: EntryRow,
    onClick: (() -> Unit)?,
    onEdit: (() -> Unit)?
) {
    if (onClick != null) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            EntryCardContent(row, onEdit)
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            EntryCardContent(row, onEdit)
        }
    }
}

@Composable
private fun EntryCardContent(row: EntryRow, onEdit: (() -> Unit)?) {
    val entry = row.entry
    val isDone = entry.completedAt != null

    Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // A small lock before the name: this entry can't be started yet.
                    if (row.isBlocked) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Blocked",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                if (row.detail.isNotEmpty()) {
                    Text(
                        text = row.detail,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                row.subtitle?.let { path ->
                    Text(
                        text = path,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (row.isBlocked) {
                    Text(
                        text = "Waiting for: " + row.waitingFor.joinToString(", "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }

            // Goals show how many things are inside them, and an arrow: "tap to open".
            if (entry is Goal) {
                if (row.childCount > 0) {
                    Text(
                        text = row.childCount.toString(),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open"
                )
            }
        }

        if (row.showProgress) {
            ProgressBarWithText(
                fraction = row.fraction,
                text = row.progressText,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}