package com.todaysday.planner.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * The root of the whole UI. A Scaffold is a ready-made page layout with slots for a
 * top bar, a bottom bar and the main content in between.
 *
 * The top and bottom bars are only shown on the five main screens. The forms are full
 * screen and have their own top bar with Save.
 */
@Composable
fun PlannerRoot() {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentDestination = Destination.entries
        .firstOrNull { it.route == currentRoute } ?: Destination.START

    // Is the current screen one of the five main screens? (At startup the route is still null.)
    val showBars = currentRoute == null || Destination.entries.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (showBars) {
                TopBar(
                    title = currentDestination.label,
                    onMeClick = { navController.navigateTopLevel(Destination.ME) }
                )
            }
        },
        bottomBar = {
            if (showBars) {
                BottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { navController.navigateTopLevel(it) }
                )
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            // consumeWindowInsets tells screens with their own Scaffold (the forms)
            // that this space is already taken, so they don't add it a second time.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}

/**
 * Navigate to a main screen in the standard way for bottom bars:
 * - popUpTo(start): don't pile up a long back stack of tabs
 * - saveState/restoreState: a tab remembers its state when you return
 * - launchSingleTop: tapping the current tab again doesn't open a second copy
 */
private fun NavHostController.navigateTopLevel(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}