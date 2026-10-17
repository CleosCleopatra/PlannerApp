package com.todaysday.planner.ui.goals.forms

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todaysday.planner.PlannerApp
import com.todaysday.planner.ui.common.ConfirmDialog
import com.todaysday.planner.ui.common.DateField
import com.todaysday.planner.ui.common.DurationField

/** The screen for creating or editing a task. Same parameters as GoalFormScreen. */
@Composable
fun TaskFormScreen(
    entryId: Long?,
    parentId: Long?,
    onClose: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PlannerApp
    val viewModel: TaskFormViewModel = viewModel(
        factory = TaskFormViewModel.factory(
            app.container.entryRepository,
            app.container.tagPlaceRepository,
            entryId,
            parentId
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickerData by viewModel.pickerData.collectAsStateWithLifecycle()

    TaskFormContent(
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
private fun TaskFormContent(
    state: TaskFormState,
    pickerData: FormPickerData,
    onChange: ((TaskFormState) -> TaskFormState) -> Unit,
    onCreateTag: (String) -> Unit,
    onCreatePlace: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    FormScaffold(
        title = if (state.isEditing) "Edit task" else "New task",
        onCancel = onCancel,
        onSave = onSave
    ) {
        if (!state.isLoaded) {
            CircularProgressIndicator()
        } else {
            val errors = if (state.showErrors) state.errors() else TaskFormErrors()

            val parentName = state.parentId?.let { id ->
                pickerData.entries.firstOrNull { it.id == id }?.name ?: state.parentName
            }

            state.saveError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            NameField(
                value = state.name,
                onValueChange = { v -> onChange { it.copy(name = v) } },
                error = errors.name
            )

            // A new task only needs a name. Everything else is behind "More options".
            if (!state.isEditing) {
                if (!state.showExtras && parentName != null) {
                    Text("Part of: $parentName", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = "Takes 10 minutes unless you change it under More options.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { onChange { it.copy(showExtras = !it.showExtras) } }) {
                    Text(if (state.showExtras) "Fewer options" else "More options")
                }
            }

            if (state.showExtras) {
                DescriptionField(
                    value = state.description,
                    onValueChange = { v -> onChange { it.copy(description = v) } }
                )
                DurationField(
                    label = "Time needed",
                    minutes = state.timeMinutes,
                    onMinutesChange = { m -> onChange { it.copy(timeMinutes = m) } },
                    errorText = errors.time
                )
                LevelChips(
                    label = "Priority",
                    selected = state.priority,
                    onSelected = { level -> onChange { it.copy(priority = level) } }
                )
                LevelChips(
                    label = "Effort",
                    selected = state.effort,
                    onSelected = { level -> onChange { it.copy(effort = level) } }
                )
                DateField(
                    label = "Due date",
                    date = state.dueDate,
                    onDateChange = { d -> onChange { it.copy(dueDate = d) } }
                )
                PickerSections(
                    entryId = state.original?.id,
                    data = pickerData,
                    parentId = state.parentId,
                    parentName = parentName,
                    tagIds = state.tagIds,
                    placeId = state.placeId,
                    requiredIds = state.requiredIds,
                    onParentChange = { id -> onChange { it.copy(parentId = id) } },
                    onTagsChange = { ids -> onChange { it.copy(tagIds = ids) } },
                    onPlaceChange = { id -> onChange { it.copy(placeId = id) } },
                    onRequiredChange = { ids -> onChange { it.copy(requiredIds = ids) } },
                    onCreateTag = onCreateTag,
                    onCreatePlace = onCreatePlace
                )
                CountsTowardParentSwitch(
                    parentName = parentName,
                    countsTowardParent = state.countsTowardParent,
                    onCountsTowardParentChange = { on -> onChange { it.copy(countsTowardParent = on) } }
                )
            }

            if (state.isEditing) {
                DoneAndDeleteSection(
                    isDone = state.isDone,
                    onDoneChange = { done -> onChange { it.copy(isDone = done) } },
                    onDeleteClick = { showDeleteDialog = true }
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete task?",
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