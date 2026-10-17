package com.todaysday.planner.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todaysday.planner.PlannerApp
import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.EntryType

// The four subtabs, in order: which kind of entry each one shows, and its label.
private val subtabs = listOf(
    EntryType.GOAL to "Goals",
    EntryType.TASK to "Tasks",
    EntryType.EVENT to "Events",
    EntryType.REPEATING to "Repeating"
)

// What the + button can create. Repeating entries are added in the next message (M6b).
// What the + button can create.
private val creatable = listOf(
    EntryType.GOAL to "New goal",
    EntryType.TASK to "New task",
    EntryType.EVENT to "New event",
    EntryType.REPEATING to "New repeating"
)

/**
 * The Goals tab. This function connects the ViewModel to the screen below.
 *
 * @param onCreate called when the + button asks for a new entry
 *                 (the Long is the goal to put it in, or null)
 * @param onEdit   called when an entry is tapped to be edited
 */
@Composable
fun GoalsScreen(
    onCreate: (EntryType, Long?) -> Unit,
    onEdit: (Entry) -> Unit
) {
    val app = LocalContext.current.applicationContext as PlannerApp
    val viewModel: GoalsViewModel = viewModel(
        factory = GoalsViewModel.factory(app.container.entryRepository)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GoalsContent(
        state = state,
        onTabSelected = viewModel::selectTab,
        onGoalClick = viewModel::openGoal,
        onBack = viewModel::goUp,
        onCreate = onCreate,
        onEdit = onEdit
    )
}

/** The actual layout: subtabs on top, an optional Back button, the list, and the + button. */
@Composable
private fun GoalsContent(
    state: GoalsUiState,
    onTabSelected: (EntryType) -> Unit,
    onGoalClick: (Long) -> Unit,
    onBack: () -> Unit,
    onCreate: (EntryType, Long?) -> Unit,
    onEdit: (Entry) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    // The phone's back button goes up one goal level while we are inside a goal.
    BackHandler(enabled = state.openGoalId != null) { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = subtabs.indexOfFirst { it.first == state.selectedTab }.coerceAtLeast(0)
            ) {
                subtabs.forEach { (type, label) ->
                    Tab(
                        selected = state.selectedTab == type,
                        onClick = { onTabSelected(type) },
                        text = { Text(label) }
                    )
                }
            }

            if (state.openGoalId != null) {
                TextButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back")
                }
            }

            EntryListScreen(
                state = state,
                onGoalClick = onGoalClick,
                onEdit = onEdit,
                modifier = Modifier.weight(1f)
            )
        }

        // The + button. Hidden on a subtab whose entries can't be created yet.
        val canCreate = creatable.any { it.first == state.selectedTab }
        if (canCreate) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (state.selectedTab == EntryType.GOAL) {
                            menuOpen = true // on the Goals subtab you choose what to create
                        } else {
                            onCreate(state.selectedTab, null)
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    creatable.forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                menuOpen = false
                                // Inside an open goal, the new entry goes into that goal.
                                onCreate(type, state.openGoalId)
                            }
                        )
                    }
                }
            }
        }
    }
}