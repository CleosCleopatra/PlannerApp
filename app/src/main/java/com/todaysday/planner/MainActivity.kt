package com.todaysday.planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.todaysday.planner.ui.navigation.PlannerRoot
import com.todaysday.planner.ui.theme.PlannerTheme

/**
 * The single Activity of the app. In Compose apps the Activity does very little:
 * it just hands the screen over to composable functions.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lets the app draw behind the status bar and navigation bar (modern look).
        enableEdgeToEdge()

        // setContent says: "everything on screen is described by this composable".
        // PlannerTheme applies colors and fonts; PlannerRoot is our whole UI.
        setContent {
            PlannerTheme {
                PlannerRoot()
            }
        }
    }
}