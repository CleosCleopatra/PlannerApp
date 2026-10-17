package com.todaysday.planner.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.entryType
import com.todaysday.planner.domain.picker.PickerModel

private fun kindLabel(type: EntryType) = when (type) {
    EntryType.GOAL -> "Goal"
    EntryType.TASK -> "Task"
    EntryType.EVENT -> "Event"
    EntryType.REPEATING -> "Repeating"
}

/**
 * A pop-up menu for choosing entries. It starts at the top level; the arrow on a row opens
 * what is inside it, and the button at the top goes back up one level.
 *
 * What is shown (finished entries, which kinds, greyed-out rows) is decided by the PickerModel.
 *
 * @param multiSelect  true: tick several (the pop-up stays open, tap Done when finished)
 *                     false: tapping a row chooses it (the caller closes the pop-up)
 * @param onSelect     called when a row that can be chosen is tapped
 * @param noneLabel    if set, a first row with this text (e.g. "No parent") is shown at the top level
 */
@Composable
fun EntryPickerDialog(
    title: String,
    model: PickerModel,
    selectedIds: Set<Long>,
    multiSelect: Boolean,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    noneLabel: String? = null,
    onNone: () -> Unit = {}
) {
    // Which goal we are inside (null = the top level). Kept when the screen is rotated.
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val items = model.itemsIn(openId)
    val path = model.pathTo(openId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (openId != null) {
                    TextButton(onClick = { openId = model.parentOf(openId) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(path.joinToString(" › ") { it.name })
                    }
                }

                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    if (noneLabel != null && openId == null) {
                        item(key = "none") {
                            ChoiceRow(
                                label = noneLabel,
                                supporting = null,
                                selected = selectedIds.isEmpty(),
                                selectable = true,
                                multiSelect = false,
                                onClick = onNone,
                                onOpen = null
                            )
                        }
                    }

                    items(items, key = { it.entry.id }) { item ->
                        ChoiceRow(
                            label = item.entry.name,
                            supporting = if (item.selectable) {
                                kindLabel(item.entry.entryType)
                            } else {
                                "Not possible here"
                            },
                            selected = item.entry.id in selectedIds,
                            selectable = item.selectable,
                            multiSelect = multiSelect,
                            onClick = { onSelect(item.entry.id) },
                            onOpen = if (item.hasChildren) ({ openId = item.entry.id }) else null
                        )
                    }

                    if (items.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = "Nothing to choose here.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (multiSelect) "Done" else "Close") }
        }
    )
}

/** One row: a tick box or radio button, the name, and an arrow if there is something inside. */
@Composable
private fun ChoiceRow(
    label: String,
    supporting: String?,
    selected: Boolean,
    selectable: Boolean,
    multiSelect: Boolean,
    onClick: () -> Unit,
    onOpen: (() -> Unit)?
) {
    val textColor = if (selectable) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = selectable, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (multiSelect) {
            Checkbox(checked = selected, onCheckedChange = null, enabled = selectable)
        } else {
            RadioButton(selected = selected, onClick = null, enabled = selectable)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(text = label, color = textColor, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    text = supporting,
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        if (onOpen != null) {
            IconButton(onClick = onOpen) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open")
            }
        }
    }
}