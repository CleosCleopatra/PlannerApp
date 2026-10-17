package com.todaysday.planner.ui.goals.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.todaysday.planner.data.repository.EntryRepository
import com.todaysday.planner.data.repository.TagPlaceRepository
import com.todaysday.planner.domain.model.Task
import com.todaysday.planner.domain.picker.PickerRules
import com.todaysday.planner.domain.tree.EntryTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Same idea as GoalFormViewModel, for the task form. */
class TaskFormViewModel(
    private val repository: EntryRepository,
    private val tagPlaceRepository: TagPlaceRepository,
    private val entryId: Long?,
    private val initialParentId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(TaskFormState())
    val state: StateFlow<TaskFormState> = _state.asStateFlow()

    val pickerData: StateFlow<FormPickerData> =
        FormPickerSource(repository, tagPlaceRepository).data
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FormPickerData())

    private var busy = false

    init {
        viewModelScope.launch {
            val existing = entryId?.let { repository.getEntry(it) } as? Task
            val parent = (existing?.parentId ?: initialParentId)?.let { repository.getEntry(it) }
            val required = if (existing != null) {
                repository.getRequiredIds(existing.id)
            } else {
                parent?.let {repository.getRequiredIds(it.id)}.orEmpty()
            }
            _state.value = if (existing != null) {
                TaskFormState.forTask(existing, parent?.name, required)
            } else {
                TaskFormState.forNew(parent?.id, parent?.name).copy (
                    description = parent?.description.orEmpty(),
                    priority = parent?.priority
                        ?: com.todaysday.planner.domain.model.Level.NORMAL,
                    effort = parent?.effort
                        ?: com.todaysday.planner.domain.model.Level.NORMAL,
                    dueDate = parent?.dueDate,
                    placeId = parent?.placeId,
                    tagIds = parent?.tagIds.orEmpty(),
                    requiredIds = required,
                    countsTowardParent = parent?.countsTowardParent ?: true
                )
            }
        }
    }

    fun change(transform: (TaskFormState) -> TaskFormState) {
        _state.update { transform(it).copy(saveError = null) }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            tagPlaceRepository.getOrCreateTag(name)?.let { tag ->
                _state.update { it.copy(tagIds = it.tagIds + tag.id) }
            }
        }
    }

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
        val task = form.toTask()
        if (task == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }

        val data = pickerData.value
        val loop = PickerRules.choicesCreateCycle(
            tree = EntryTree(data.entries),
            requirements = data.requirements,
            entryId = form.original?.id,
            newParentId = form.parentId,
            requiredIds = form.requiredIds
        )
        if (loop) {
            _state.update { it.copy(saveError = GoalFormViewModel.LOOP_MESSAGE) }
            return
        }

        busy = true
        viewModelScope.launch {
            repository.save(task, requiredIds = form.requiredIds)
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
        fun factory(
            repository: EntryRepository,
            tagPlaceRepository: TagPlaceRepository,
            entryId: Long?,
            parentId: Long?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { TaskFormViewModel(repository, tagPlaceRepository, entryId, parentId) }
        }
    }
}