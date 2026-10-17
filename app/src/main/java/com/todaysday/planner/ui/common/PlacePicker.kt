package com.todaysday.planner.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.todaysday.planner.domain.model.Place

/**
 * A pop-up for choosing ONE place: "Anywhere" (an entry that can be done anywhere), an
 * existing place, or a new one typed in below.
 *
 * @param selectedId the chosen place, or null for Anywhere
 * @param onSelect   called with the place id, or null for Anywhere (the caller closes the pop-up)
 * @param onCreate   called with a typed name (the place is created or reused, then chosen)
 */
@Composable
fun PlacePickerDialog(
    places: List<Place>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Place") },
        text = {
            Column {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    item(key = "anywhere") {
                        PlaceRow(name = "Anywhere", selected = selectedId == null) { onSelect(null) }
                    }
                    items(places, key = { it.id }) { place ->
                        PlaceRow(name = place.name, selected = place.id == selectedId) {
                            onSelect(place.id)
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New place") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = newName.isNotBlank(),
                        onClick = {
                            onCreate(newName)
                            newName = ""
                        }
                    ) { Text("Add") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun PlaceRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}