package com.todaysday.planner.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Two small fields, hours and minutes, that together give a number of minutes.
 * Both empty = no value (null). Example: 2 hours and 30 minutes = 150.
 */
@Composable
fun DurationField(
    label: String,
    minutes: Int?,
    onMinutesChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null
) {
    // The text of each box. A zero is shown as an empty box to keep it tidy.
    var hoursText by rememberSaveable {
        mutableStateOf(if (minutes != null && minutes >= 60) (minutes / 60).toString() else "")
    }
    var minutesText by rememberSaveable {
        mutableStateOf(if (minutes != null && minutes % 60 != 0) (minutes % 60).toString() else "")
    }

    // Works out the total number of minutes from both boxes and tells the form.
    fun report() {
        val hours = hoursText.toIntOrNull()
        val mins = minutesText.toIntOrNull()
        onMinutesChange(if (hours == null && mins == null) null else (hours ?: 0) * 60 + (mins ?: 0))
    }

    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hoursText,
                onValueChange = { typed ->
                    hoursText = typed.filter { it.isDigit() }.take(4)
                    report()
                },
                label = { Text("Hours") },
                singleLine = true,
                isError = errorText != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minutesText,
                onValueChange = { typed ->
                    minutesText = typed.filter { it.isDigit() }.take(4)
                    report()
                },
                label = { Text("Minutes") },
                singleLine = true,
                isError = errorText != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}