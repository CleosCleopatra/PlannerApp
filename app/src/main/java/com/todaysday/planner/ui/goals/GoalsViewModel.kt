package com.todaysday.planner.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.todaysday.planner.data.repository.EntryRepository
import com.todaysday.planner.domain.model.EntryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * The ViewModel holds the state of the Goals screen (which subtab is open, which goal we are
 * inside) and survives screen rotation. It does very little itself: it hands everything to
 * buildGoalsUiState and the screen just draws the result.
 */
class GoalsViewModel(repository: EntryRepository) : ViewModel() {

    // MutableStateFlow = a box holding one value that the screen can watch for changes.
    private val selectedTab = MutableStateFlow(EntryType.GOAL)
    private val openGoalId = MutableStateFlow<Long?>(null)

    /**
     * combine() re-runs the code in the braces whenever ANY of the four inputs changes:
     * the entries in the database, the "requires completion of" links, the selected subtab,
     * or the open goal.
     */
    val uiState: StateFlow<GoalsUiState> = combine(
        repository.observeEntries(),
        repository.observeRequirements(),
        selectedTab,
        openGoalId
    ) { entries, requirements, tab, openId ->
        buildGoalsUiState(
            entries = entries,
            loggedMinutes = emptyMap(), // the work history arrives in M13
            selectedTab = tab,
            openGoalId = openId,
            requirements = requirements
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000), // stop work 5 s after the screen is gone
        initialValue = GoalsUiState()
    )

    fun selectTab(tab: EntryType) {
        selectedTab.value = tab
    }

    fun openGoal(id: Long) {
        openGoalId.value = id
    }

    /** One level up: to the parent goal, or to the top level. */
    fun goUp() {
        openGoalId.value = uiState.value.parentOfOpenGoalId
    }

    companion object {
        /** Tells Android how to create this ViewModel (it needs the repository). */
        fun factory(repository: EntryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { GoalsViewModel(repository) }
        }
    }
}