package com.todaysday.planner.ui.goals.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.todaysday.planner.data.repository.EntryRepository
import com.todaysday.planner.data.repository.TagPlaceRepository
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.picker.PickerRules
import com.todaysday.planner.domain.tree.EntryTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the goal form while the user fills it in (and survives screen rotation).
 *
 * @param entryId         the goal to edit, or null to create a new one
 * @param initialParentId the goal a NEW goal should be placed in (the goal we were inside)
 */
class GoalFormViewModel(
    private val repository: EntryRepository,
    private val tagPlaceRepository: TagPlaceRepository,
    private val entryId: Long?,
    private val initialParentId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(GoalFormState())
    val state: StateFlow<GoalFormState> = _state.asStateFlow()

    /** The entries, tags and places that the pickers show. Updates when they change. */
    val pickerData: StateFlow<FormPickerData> =
        FormPickerSource(repository, tagPlaceRepository).data
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FormPickerData())

    // Stops a double tap on Save from saving twice.
    private var busy = false

    init {
        viewModelScope.launch {
            val existing = entryId?.let { repository.getEntry(it) } as? Goal
            val parent = (existing?.parentId ?: initialParentId)?.let { repository.getEntry(it) }
            val required = if (existing != null) repository.getRequiredIds(existing.id) else emptySet()
            _state.value = if (existing != null) {
                GoalFormState.forGoal(existing, parent?.name, required)
            } else {
                GoalFormState.forNew(parent?.id, parent?.name)
            }
        }
    }

    /** The screen calls this with a small function describing the change, e.g. { it.copy(name = "x") }. */
    fun change(transform: (GoalFormState) -> GoalFormState) {
        // Any change clears an old "can't save" message.
        _state.update { transform(it).copy(saveError = null) }
    }

    /** Creates a tag (or reuses one with the same name) and ticks it. */
    fun createTag(name: String) {
        viewModelScope.launch {
            tagPlaceRepository.getOrCreateTag(name)?.let { tag ->
                _state.update { it.copy(tagIds = it.tagIds + tag.id) }
            }
        }
    }

    /** Creates a place (or reuses one with the same name) and chooses it. */
    fun createPlace(name: String) {
        viewModelScope.launch {
            tagPlaceRepository.getOrCreatePlace(name)?.let { place ->
                _state.update { it.copy(placeId = place.id) }
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        if (busy) return
        val form = _state.value
        val goal = form.toGoal()
        if (goal == null) {
            _state.update { it.copy(showErrors = true) } // now the error messages appear
            return
        }

        // The pickers check one choice at a time. Here we check all choices together.
        val data = pickerData.value
        val loop = PickerRules.choicesCreateCycle(
            tree = EntryTree(data.entries),
            requirements = data.requirements,
            entryId = form.original?.id,
            newParentId = form.parentId,
            requiredIds = form.requiredIds
        )
        if (loop) {
            _state.update { it.copy(saveError = LOOP_MESSAGE) }
            return
        }

        busy = true
        viewModelScope.launch {
            repository.save(goal, requiredIds = form.requiredIds)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _state.value.original?.id ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            repository.delete(id)
            onDeleted()
        }
    }

    companion object {
        const val LOOP_MESSAGE =
            "These choices would make things wait for each other forever. " +
                    "Change \"Part of\" or \"Requires completion of\"."

        fun factory(
            repository: EntryRepository,
            tagPlaceRepository: TagPlaceRepository,
            entryId: Long?,
            parentId: Long?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { GoalFormViewModel(repository, tagPlaceRepository, entryId, parentId) }
        }
    }
}