package com.todaysday.planner.ui.goals.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.todaysday.planner.data.repository.EntryRepository
import com.todaysday.planner.data.repository.TagPlaceRepository
import com.todaysday.planner.domain.model.Repeating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Holds the repeating form while the user fills it in (and survives screen rotation).
 * Works like the event form ViewModel. Repeating entries have no "requires completion of",
 * so there is no loop check here.
 */
class RepeatingFormViewModel(
    private val repository: EntryRepository,
    private val tagPlaceRepository: TagPlaceRepository,
    private val entryId: Long?,
    private val initialParentId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(RepeatingFormState())
    val state: StateFlow<RepeatingFormState> = _state.asStateFlow()

    val pickerData: StateFlow<FormPickerData> =
        FormPickerSource(repository, tagPlaceRepository).data
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FormPickerData())

    // Stops a double tap on Save from saving twice.
    private var busy = false

    init {
        viewModelScope.launch {
            val existing = entryId?.let { repository.getEntry(it) } as? Repeating
            val parent = (existing?.parentId ?: initialParentId)?.let { repository.getEntry(it) }
            _state.value = if (existing != null) {
                RepeatingFormState.forRepeating(existing, parent?.name)
            } else {
                RepeatingFormState.forNew(parent?.id, parent?.name, LocalDate.now())
            }
        }
    }

    fun change(transform: (RepeatingFormState) -> RepeatingFormState) {
        _state.update(transform)
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
        val entry = _state.value.toRepeating()
        if (entry == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        busy = true
        viewModelScope.launch {
            repository.save(entry)
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
            initializer { RepeatingFormViewModel(repository, tagPlaceRepository, entryId, parentId) }
        }
    }
}