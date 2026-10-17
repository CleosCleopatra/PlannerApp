package com.todaysday.planner.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * A text field that only accepts whole numbers. An empty field means "no value" (null).
 *
 * The field remembers the text the user typed ("text"), and tells the form the number.
 * maxDigits keeps numbers small enough that later multiplications cannot overflow.
 */
@Composable
fun IntField(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    maxDigits: Int = 6
) {
    // rememberSaveable keeps the text even when the screen is rotated.
    var text by rememberSaveable { mutableStateOf(value?.toString() ?: "") }

    OutlinedTextField(
        value = text,
        onValueChange = { typed ->
            text = typed.filter { it.isDigit() }.take(maxDigits)
            onValueChange(text.toIntOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        isError = errorText != null,
        supportingText = { if (errorText != null) Text(errorText) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}