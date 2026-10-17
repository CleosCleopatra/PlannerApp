package com.todaysday.planner.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// The order here is the order on screen: Settings is the leftmost item (bottom-left corner),
// followed by the three main tabs. "Me" is not here because it lives in the top bar.
private val bottomBarItems = listOf(
    Destination.SETTINGS,
    Destination.GOALS,
    Destination.START,
    Destination.SCHEDULE
)

/**
 * The bar at the bottom of the screen.
 *
 * @param currentRoute the route of the screen currently shown (used to highlight an item)
 * @param onNavigate   called when the user taps an item; the parent decides how to navigate
 */
@Composable
fun BottomBar(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    NavigationBar {
        bottomBarItems.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}