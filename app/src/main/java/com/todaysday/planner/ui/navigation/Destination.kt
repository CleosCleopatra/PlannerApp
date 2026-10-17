package com.todaysday.planner.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every top-level screen of the app.
 *
 * - route: the internal "address" of the screen, used by navigation
 * - label: the text shown in the bar
 * - icon:  the icon shown in the bar
 *
 * Using an enum means a typo in a route can't happen: we always write Destination.GOALS.
 */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
    GOALS("goals", "Goals", Icons.Filled.Star),
    START("start", "Start", Icons.Filled.PlayArrow),
    SCHEDULE("schedule", "Schedule", Icons.Filled.DateRange),
    ME("me", "Me", Icons.Filled.Person)
}