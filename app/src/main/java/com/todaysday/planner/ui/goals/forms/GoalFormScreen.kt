package com.todaysday.planner.ui.goals.forms

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todaysday.planner.PlannerApp
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.ui.common.ChoiceChips
import com.todaysday.planner.ui.common.ConfirmDialog
import com.todaysday.planner.ui.common.DateField
import com.todaysday.planner.ui.common.DurationField

private val kindOptions = listOf(
    GoalKind.TIME to "Time",
    GoalKind.ACHIEVEMENT to "Achievement",
    GoalKind.NUMBER to "Number"
)

private fun kindExplanation(kind: GoalKind) = when (kind) {
    GoalKind.TIME -> "Spend a certain amount of time, e.g. 10 hours studying Serbian. The time is exact."
    GoalKind.ACHIEVEMENT -> "Achieve something, e.g. build a plane. The time is an estimate."
    GoalKind.NUMBER -> "Do a number of things, e.g. learn 1000 Serbian words. The total time is an estimate."
}

/**
 * The screen for creating or editing a goal.
 * @param entryId  the goal to edit, or null for a new goal
 * @param parentId the goal a new goal goes into, or null for the top level
 * @param onClose  called after saving, deleting or cancelling
 */
@Composable
fun GoalFormScreen(
    entryId: Long?,
    parentId: Long?,
    onClose: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PlannerApp
    val viewModel: GoalFormViewModel = viewModel(
        factory = GoalFormViewModel.factory(
            app.container.entryRepository,
            app.container.tagPlaceRepository,
            entryId,
            parentId
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickerData by viewModel.pickerData.collectAsStateWithLifecycle()

    GoalFormContent(
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
private fun GoalFormContent(
    state: GoalFormState,
    pickerData: FormPickerData,
    onChange: ((GoalFormState) -> GoalFormState) -> Unit,
    onCreateTag: (String) -> Unit,
    onCreatePlace: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    FormScaffold(
        title = if (state.isEditing) "Edit goal" else "New goal",
        onCancel = onCancel,
        onSave = onSave
    ) {
        if (!state.isLoaded) {
            CircularProgressIndicator()
        } else {
            // Errors only appear after the user has pressed Save once.
            val errors = if (state.showErrors) state.errors() else GoalFormErrors()

            // The name of the parent goal: the live name if we have it, else the loaded one.
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
            DescriptionField(
                value = state.description,
                onValueChange = { v -> onChange { it.copy(description = v) } }
            )

            Text("Type of goal", style = MaterialTheme.typography.titleSmall)
            ChoiceChips(
                options = kindOptions,
                selected = state.kind,
                onSelected = { kind -> onChange { it.copy(kind = kind) } }
            )
            Text(
                text = kindExplanation(state.kind),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.kind == GoalKind.NUMBER) {
                NumberGoalSetup(state = state, errors = errors, onChange = onChange)
            } else {
                DurationField(
                    label = if (state.kind == GoalKind.TIME) "Time needed (exact)" else "Time needed (estimate)",
                    minutes = state.timeMinutes,
                    onMinutesChange = { m -> onChange { it.copy(timeMinutes = m) } },
                    errorText = errors.time
                )
            }

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
            title = "Delete goal?",
            message = "\"${state.name}\" will be deleted. Anything inside it is kept and moves to the top level.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}