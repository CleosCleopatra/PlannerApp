package com.todaysday.planner.ui.goals.forms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todaysday.planner.PlannerApp
import com.todaysday.planner.domain.model.RepeatType
import com.todaysday.planner.ui.common.ChoiceChips
import com.todaysday.planner.ui.common.ConfirmDialog
import com.todaysday.planner.ui.common.DateField
import com.todaysday.planner.ui.common.DurationField
import com.todaysday.planner.ui.common.IntField
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val typeOptions = listOf(
    RepeatType.WEEKDAYS to "Weekdays",
    RepeatType.EVERY_N_DAYS to "Every N days",
    RepeatType.WEEKDAYS_EVERY_N_WEEKS to "Every N weeks"
)

private fun typeExplanation(type: RepeatType) = when (type) {
    RepeatType.WEEKDAYS -> "On the days you choose, every week."
    RepeatType.EVERY_N_DAYS -> "Every 2nd, 3rd, ... day, counting from the start date."
    RepeatType.WEEKDAYS_EVERY_N_WEEKS -> "On the days you choose, but only every N-th week."
}

/**
 * The screen for creating or editing a repeating entry.
 * @param entryId  the entry to edit, or null for a new one
 * @param parentId the goal a new entry goes into, or null for the top level
 * @param onClose  called after saving, deleting or cancelling
 */
@Composable
fun RepeatingFormScreen(
    entryId: Long?,
    parentId: Long?,
    onClose: () -> Unit
) {
    val app = LocalContext.current.applicationContext as PlannerApp
    val viewModel: RepeatingFormViewModel = viewModel(
        factory = RepeatingFormViewModel.factory(
            app.container.entryRepository,
            app.container.tagPlaceRepository,
            entryId,
            parentId
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickerData by viewModel.pickerData.collectAsStateWithLifecycle()

    RepeatingFormContent(
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
private fun RepeatingFormContent(
    state: RepeatingFormState,
    pickerData: FormPickerData,
    onChange: ((RepeatingFormState) -> RepeatingFormState) -> Unit,
    onCreateTag: (String) -> Unit,
    onCreatePlace: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    FormScaffold(
        title = if (state.isEditing) "Edit repeating" else "New repeating",
        onCancel = onCancel,
        onSave = onSave
    ) {
        if (!state.isLoaded) {
            CircularProgressIndicator()
        } else {
            val errors = if (state.showErrors) state.errors() else RepeatingFormErrors()

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

            // ---------- The repeat pattern ----------
            Text("Repeats", style = MaterialTheme.typography.titleSmall)
            ChoiceChips(
                options = typeOptions,
                selected = state.repeatType,
                onSelected = { type -> onChange { it.copy(repeatType = type) } }
            )
            Text(
                text = typeExplanation(state.repeatType),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.usesWeekdays) {
                WeekdayPicker(
                    selected = state.weekdays,
                    onChange = { days -> onChange { it.copy(weekdays = days) } }
                )
                errors.days?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (state.usesInterval) {
                IntField(
                    label = if (state.repeatType == RepeatType.EVERY_N_DAYS) {
                        "Every how many days?"
                    } else {
                        "Every how many weeks?"
                    },
                    value = state.everyN,
                    onValueChange = { n -> onChange { it.copy(everyN = n) } },
                    errorText = errors.interval,
                    maxDigits = 3
                )
            }

            DateField(
                label = "Starts on",
                date = state.startDate,
                onDateChange = { date -> if (date != null) onChange { it.copy(startDate = date) } },
                allowClear = false
            )

            DurationField(
                label = "How long each time takes",
                minutes = state.sessionMinutes,
                onMinutesChange = { m -> onChange { it.copy(sessionMinutes = m) } },
                errorText = errors.time
            )

            // A preview, so you can see right away if the pattern does what you meant.
            val upcoming = state.upcomingDates(LocalDate.now())
            if (upcoming.isNotEmpty()) {
                val format = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
                Text(
                    text = "Next: " + upcoming.joinToString(", ") { it.format(format) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
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
                DoneAndDeleteSection(
                    isDone = state.isDone,
                    onDoneChange = { done -> onChange { it.copy(isDone = done) } },
                    onDeleteClick = { showDeleteDialog = true },
                    doneLabel = "Stopped (no more repeats)"
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete repeating entry?",
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

/**
 * Seven round buttons, Monday to Sunday. Tap a day to switch it on or off.
 * A line underneath spells out the choice, because single letters can be ambiguous.
 */
@Composable
private fun WeekdayPicker(
    selected: Set<DayOfWeek>,
    onChange: (Set<DayOfWeek>) -> Unit
) {
    val locale = Locale.getDefault()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DayOfWeek.values().forEach { day ->
            val isSelected = day in selected
            val fullName = day.getDisplayName(TextStyle.FULL, locale)

            Surface(
                onClick = { onChange(if (isSelected) selected - day else selected + day) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .semantics { contentDescription = fullName }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    Text(
        text = if (selected.isEmpty()) {
            "No days selected"
            "No days selected"
        } else {
            "Selected: " + selected.sortedBy { it.value }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}