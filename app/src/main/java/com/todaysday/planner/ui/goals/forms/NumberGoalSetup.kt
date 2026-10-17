package com.todaysday.planner.ui.goals.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.todaysday.planner.domain.progress.formatMinutes
import com.todaysday.planner.ui.common.IntField

/**
 * The extra questions for a NUMBER goal, instead of the plain "time needed" field:
 * how many, and how long each one takes. The total time is worked out from those two.
 */
@Composable
fun NumberGoalSetup(
    state: GoalFormState,
    errors: GoalFormErrors,
    onChange: ((GoalFormState) -> GoalFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IntField(
            label = "How many do you want to complete?",
            value = state.numberTarget,
            onValueChange = { v -> onChange { it.copy(numberTarget = v) } },
            errorText = errors.numberTarget,
            maxDigits = 5
        )
        IntField(
            label = "Minutes each one takes (estimate)",
            value = state.minutesPerUnit,
            onValueChange = { v -> onChange { it.copy(minutesPerUnit = v) } },
            errorText = errors.minutesPerUnit,
            maxDigits = 4
        )
        state.totalMinutes?.let { total ->
            Text(
                text = "Total time: ${formatMinutes(total)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        // Until the work history exists (M13), you can set the progress by hand.
        if (state.isEditing) {
            IntField(
                label = "How many are done so far?",
                value = state.numberDone,
                onValueChange = { v -> onChange { it.copy(numberDone = v) } },
                errorText = errors.numberDone,
                maxDigits = 5
            )
        }
    }
}