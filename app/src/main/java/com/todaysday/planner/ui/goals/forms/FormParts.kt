package com.todaysday.planner.ui.goals.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todaysday.planner.domain.model.Level
import com.todaysday.planner.ui.common.ChoiceChips

/**
 * The frame of a form: a top bar with a close (X) button and a Save button,
 * and a scrollable column for the fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScaffold(
    title: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = { TextButton(onClick = onSave) { Text("Save") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding() // lifts the form above the keyboard
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun NameField(value: String, onValueChange: (String) -> Unit, error: String?) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Name") },
        singleLine = true,
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun DescriptionField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Description (optional)") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Title + Low/Normal/High buttons, used for both priority and effort. */
@Composable
fun LevelChips(label: String, selected: Level, onSelected: (Level) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        ChoiceChips(
            options = listOf(Level.LOW to "Low", Level.NORMAL to "Normal", Level.HIGH to "High"),
            selected = selected,
            onSelected = onSelected
        )
    }
}

/** The independent / counts-toward-parent switch. Only shown when the entry is part of a goal. */
@Composable
fun CountsTowardParentSwitch(
    parentName: String?,
    countsTowardParent: Boolean,
    onCountsTowardParentChange: (Boolean) -> Unit
) {
    if (parentName != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Time counts toward \"$parentName\"")
                Text(
                    text = if (countsTowardParent) {
                        "On: this time is added to the parent's total, and work on this counts toward it."
                    } else {
                        "Off: independent. The parent's time and progress are not affected."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = countsTowardParent, onCheckedChange = onCountsTowardParentChange)
        }
    }
}

/** Only shown when editing: a "Done" switch and a Delete button. */
@Composable
fun DoneAndDeleteSection(
    isDone: Boolean,
    onDoneChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Done", modifier = Modifier.weight(1f))
            Switch(checked = isDone, onCheckedChange = onDoneChange)
        }
        OutlinedButton(
            onClick = onDeleteClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete")
        }
    }
}