package com.todaysday.planner.ui.goals.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.picker.PickerModel
import com.todaysday.planner.domain.picker.PickerOptions
import com.todaysday.planner.domain.picker.PickerRules
import com.todaysday.planner.domain.tree.DependencyGraph
import com.todaysday.planner.domain.tree.EntryTree
import com.todaysday.planner.ui.common.EntryPickerDialog
import com.todaysday.planner.ui.common.PlacePickerDialog
import com.todaysday.planner.ui.common.TagPickerDialog

// Which pop-up is open right now.
private enum class OpenPicker { NONE, PARENT, TAGS, PLACE, REQUIRES }

/**
 * The four "organise" rows of a form: Part of, Tags, Place and Requires completion of.
 * Each shows the current choice and a button that opens the matching pop-up.
 * Both the goal form and the task form use this.
 *
 * @param entryId the entry being edited, or null for a new one (needed to grey out loops)
 */
@Composable
fun PickerSections(
    entryId: Long?,
    data: FormPickerData,
    parentId: Long?,
    parentName: String?,
    tagIds: Set<Long>,
    placeId: Long?,
    requiredIds: Set<Long>,
    onParentChange: (Long?) -> Unit,
    onTagsChange: (Set<Long>) -> Unit,
    onPlaceChange: (Long?) -> Unit,
    onRequiredChange: (Set<Long>) -> Unit,
    onCreateTag: (String) -> Unit,
    onCreatePlace: (String) -> Unit,
    showRequires: Boolean = true
) {
    var open by rememberSaveable { mutableStateOf(OpenPicker.NONE) }

    // The texts shown in the rows.
    val tagNames = data.tags.filter { it.id in tagIds }.map { it.name }
    val placeName = data.places.firstOrNull { it.id == placeId }?.name
    val requiredNames = data.entries.filter { it.id in requiredIds }.map { it.name }

    PickerRow("Part of", parentName ?: "None (top level)", "Change") { open = OpenPicker.PARENT }
    PickerRow("Tags", tagNames.joinToString(", ").ifEmpty { "None" }, "Choose") { open = OpenPicker.TAGS }
    PickerRow("Place", placeName ?: "Anywhere", "Change") { open = OpenPicker.PLACE }
    if (showRequires) {
        PickerRow(
            "Requires completion of",
            requiredNames.joinToString(", ").ifEmpty { "Nothing" },
            "Choose"
        ) { open = OpenPicker.REQUIRES }
    }

    // ---------- The pop-ups ----------

    when (open) {
        OpenPicker.NONE -> Unit

        OpenPicker.PARENT -> {
            val model = remember(data, entryId, requiredIds) {
                val tree = EntryTree(data.entries)
                // Try the requirements as they are chosen in the form right now.
                val requirements = if (entryId != null) {
                    data.requirements + (entryId to requiredIds)
                } else {
                    data.requirements
                }
                PickerModel(
                    entries = data.entries,
                    options = PickerOptions(
                        allowedTypes = setOf(EntryType.GOAL), // only goals can contain things
                        hideCompleted = true,
                        disabledIds = PickerRules.disabledParents(tree, entryId, requirements)
                    )
                )
            }
            EntryPickerDialog(
                title = "Part of",
                model = model,
                selectedIds = setOfNotNull(parentId),
                multiSelect = false,
                onSelect = { id ->
                    onParentChange(id)
                    open = OpenPicker.NONE
                },
                onDismiss = { open = OpenPicker.NONE },
                noneLabel = "No parent (top level)",
                onNone = {
                    onParentChange(null)
                    open = OpenPicker.NONE
                }
            )
        }

        OpenPicker.TAGS -> TagPickerDialog(
            tags = data.tags,
            selectedIds = tagIds,
            onToggle = { id -> onTagsChange(if (id in tagIds) tagIds - id else tagIds + id) },
            onCreate = onCreateTag,
            onDismiss = { open = OpenPicker.NONE }
        )

        OpenPicker.PLACE -> PlacePickerDialog(
            places = data.places,
            selectedId = placeId,
            onSelect = { id ->
                onPlaceChange(id)
                open = OpenPicker.NONE
            },
            onCreate = { name ->
                onCreatePlace(name)
                open = OpenPicker.NONE
            },
            onDismiss = { open = OpenPicker.NONE }
        )

        OpenPicker.REQUIRES -> {
            val model = remember(data, entryId, requiredIds) {
                val tree = EntryTree(data.entries)
                val requirements = if (entryId != null) {
                    data.requirements + (entryId to requiredIds)
                } else {
                    data.requirements
                }
                val graph = DependencyGraph(tree, requirements)
                PickerModel(
                    entries = data.entries,
                    options = PickerOptions(
                        allowedTypes = setOf(EntryType.GOAL, EntryType.TASK),
                        hideCompleted = true,
                        disabledIds = PickerRules.disabledRequirements(graph, tree, entryId)
                    )
                )
            }
            EntryPickerDialog(
                title = "Requires completion of",
                model = model,
                selectedIds = requiredIds,
                multiSelect = true,
                onSelect = { id ->
                    onRequiredChange(if (id in requiredIds) requiredIds - id else requiredIds + id)
                },
                onDismiss = { open = OpenPicker.NONE }
            )
        }
    }
}

/** One row: a title with the current choice under it, and a button to change it. */
@Composable
private fun PickerRow(
    title: String,
    summary: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onClick) { Text(buttonText) }
    }
}