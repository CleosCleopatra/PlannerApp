package com.todaysday.planner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.todaysday.planner.ui.goals.GoalsScreen
import com.todaysday.planner.ui.goals.forms.EventFormScreen
import com.todaysday.planner.ui.goals.forms.GoalFormScreen
import com.todaysday.planner.ui.goals.forms.RepeatingFormScreen
import com.todaysday.planner.ui.goals.forms.TaskFormScreen
import com.todaysday.planner.ui.me.MeScreen
import com.todaysday.planner.ui.schedule.ScheduleScreen
import com.todaysday.planner.ui.settings.SettingsScreen
import com.todaysday.planner.ui.start.StartScreen

/**
 * The "map" of the app: which screen is shown for which route.
 * Besides the five main screens there are the four form screens.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destination.START.route,
        modifier = modifier
    ) {
        composable(Destination.SETTINGS.route) { SettingsScreen() }
        composable(Destination.GOALS.route) {
            GoalsScreen(
                onCreate = { type, parentId ->
                    navController.navigate(FormRoutes.create(type, parentId))
                },
                onEdit = { entry -> navController.navigate(FormRoutes.edit(entry)) }
            )
        }
        composable(Destination.START.route) { StartScreen() }
        composable(Destination.SCHEDULE.route) { ScheduleScreen() }
        composable(Destination.ME.route) { MeScreen() }

        composable(route = FormRoutes.GOAL_FORM, arguments = FormRoutes.arguments) { backStackEntry ->
            GoalFormScreen(
                entryId = backStackEntry.optionalId(FormRoutes.ENTRY_ID),
                parentId = backStackEntry.optionalId(FormRoutes.PARENT_ID),
                onClose = { navController.popBackStack() }
            )
        }
        composable(route = FormRoutes.TASK_FORM, arguments = FormRoutes.arguments) { backStackEntry ->
            TaskFormScreen(
                entryId = backStackEntry.optionalId(FormRoutes.ENTRY_ID),
                parentId = backStackEntry.optionalId(FormRoutes.PARENT_ID),
                onClose = { navController.popBackStack() }
            )
        }
        composable(route = FormRoutes.EVENT_FORM, arguments = FormRoutes.arguments) { backStackEntry ->
            EventFormScreen(
                entryId = backStackEntry.optionalId(FormRoutes.ENTRY_ID),
                parentId = backStackEntry.optionalId(FormRoutes.PARENT_ID),
                onClose = { navController.popBackStack() }
            )
        }
        composable(route = FormRoutes.REPEATING_FORM, arguments = FormRoutes.arguments) { backStackEntry ->
            RepeatingFormScreen(
                entryId = backStackEntry.optionalId(FormRoutes.ENTRY_ID),
                parentId = backStackEntry.optionalId(FormRoutes.PARENT_ID),
                onClose = { navController.popBackStack() }
            )
        }
    }
}