package com.todaysday.planner.ui.goals.forms

import com.todaysday.planner.data.repository.EntryRepository
import com.todaysday.planner.data.repository.TagPlaceRepository
import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.Place
import com.todaysday.planner.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Everything the pickers in a form need to know, kept up to date as the database changes. */
data class FormPickerData(
    val entries: List<Entry> = emptyList(),
    val requirements: Map<Long, Set<Long>> = emptyMap(),
    val tags: List<Tag> = emptyList(),
    val places: List<Place> = emptyList()
)

/** Joins four live lists from the database into one FormPickerData that updates by itself. */
class FormPickerSource(
    entryRepository: EntryRepository,
    tagPlaceRepository: TagPlaceRepository
) {
    val data: Flow<FormPickerData> = combine(
        entryRepository.observeEntries(),
        entryRepository.observeRequirements(),
        tagPlaceRepository.observeTags(),
        tagPlaceRepository.observePlaces()
    ) { entries, requirements, tags, places ->
        FormPickerData(entries, requirements, tags, places)
    }
}