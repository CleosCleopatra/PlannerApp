package com.todaysday.planner.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

/**
 * The bar at the top of the screen: shows the title of the current screen and the
 * "Me" button in the top-right corner.
 *
 * @OptIn is needed because TopAppBar is marked "experimental" by Google. It works fine,
 * we just have to say that we know.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onMeClick: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        // "actions" are the icons on the right-hand side of the bar.
        actions = {
            IconButton(onClick = onMeClick) {
                Icon(Destination.ME.icon, contentDescription = Destination.ME.label)
            }
        }
    )
}