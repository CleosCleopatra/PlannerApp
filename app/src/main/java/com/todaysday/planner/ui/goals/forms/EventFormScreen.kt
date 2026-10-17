package com.todaysday.planner.ui.goals.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todaysday.planner.PlannerApp
import com.todaysday.planner.ui.common.ConfirmDialog
import com.todaysday.planner.ui.common.DateField
import com.todaysday.planner.ui.common.TimeField
import java.time.LocalDateTime

/**
 * The screen for creating or editing an event.
 * @param entryId  the event to edit, or null for a new event
 * @param parentId the goal a new event goes into, or null for the top level
 * @param onClose  called after saving, deleting or cancelling
 */
@Composable
fun EventFormScreen(
    entryId: Long?,
    parentId: Long?,
    onClose: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PlannerApp
    val viewModel: EventFormViewModel = viewModel(
        factory = EventFormViewModel.factory(
            app.container.entryRepository,
            app.container.tagPlaceRepository,
            entryId,
            parentId
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickerData by viewModel.pickerData.collectAsStateWithLifecycle()

    EventFormContent(
        state = state,
        pickerData = pickerData,
        onChange = viewModel::change,
        onCreateTag = viewModel::createTag,
        onCreatePlace = viewModel::createPlace,
        onSave = { viewModel.save(onClose) },
        onDelete = { viewModel.delete(onClose) },
        onCancel = onClose
    )
}

@Composable
private fun EventFormContent(
    state: EventFormState,
    pickerData: FormPickerData,
    onChange: ((EventFormState) -> EventFormState) -> Unit,
    onCreateTag: (String) -> Unit,
    onCreatePlace: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    FormScaffold(
        title = if (state.isEditing) "Edit event" else "New event",
        onCancel = onCancel,
        onSave = onSave
    ) {
        if (!state.isLoaded) {
            CircularProgressIndicator()
        } else {
            val errors = if (state.showErrors) state.errors() else EventFormErrors()

            val parentName = state.parentId?.let { id ->
                pickerData.entries.firstOrNull { it.id == id }?.name ?: state.parentName
            }

            NameField(
                value = state.name,
                onValueChange = { v -> onChange { it.copy(name = v) } },
                error = errors.name
            )
            DescriptionField(
                value = state.description,
                onValueChange = { v -> onChange { it.copy(description = v) } }
            )

            // ---------- Start ----------
            Text("Starts", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateField(
                    label = "Date",
                    date = state.start.toLocalDate(),
                    onDateChange = { date ->
                        if (date != null) {
                            onChange { it.withStart(LocalDateTime.of(date, it.start.toLocalTime())) }
                        }
                    },
                    allowClear = false,
                    modifier = Modifier.weight(3f)
                )
                TimeField(
                    label = "Time",
                    time = state.start.toLocalTime(),
                    onTimeChange = { time ->
                        onChange { it.withStart(LocalDateTime.of(it.start.toLocalDate(), time)) }
                    },
                    modifier = Modifier.weight(2f)
                )
            }

            // ---------- End ----------
            Text("Ends", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateField(
                    label = "Date",
                    date = state.end.toLocalDate(),
                    onDateChange = { date ->
                        if (date != null) {
                            onChange { it.copy(end = LocalDateTime.of(date, it.end.toLocalTime())) }
                        }
                    },
                    allowClear = false,
                    modifier = Modifier.weight(3f)
                )
                TimeField(
                    label = "Time",
                    time = state.end.toLocalTime(),
                    onTimeChange = { time ->
                        onChange { it.copy(end = LocalDateTime.of(it.end.toLocalDate(), time)) }
                    },
                    modifier = Modifier.weight(2f)
                )
            }
            errors.time?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PickerSections(
                entryId = state.original?.id,
                data = pickerData,
                parentId = state.parentId,
                parentName = parentName,
                tagIds = state.tagIds,
                placeId = state.placeId,
                requiredIds = emptySet(),
                onParentChange = { id -> onChange { it.copy(parentId = id) } },
                onTagsChange = { ids -> onChange { it.copy(tagIds = ids) } },
                onPlaceChange = { id -> onChange { it.copy(placeId = id) } },
                onRequiredChange = {},
                onCreateTag = onCreateTag,
                onCreatePlace = onCreatePlace,
                showRequires = false
            )

            CountsTowardParentSwitch(
                parentName = parentName,
                countsTowardParent = state.countsTowardParent,
                onCountsTowardParentChange = { on -> onChange { it.copy(countsTowardParent = on) } }
            )

            if (state.isEditing) {
                DeleteButton(onDeleteClick = { showDeleteDialog = true })
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete event?",
            message = "\"${state.name}\" will be deleted.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}