package com.todaysday.planner

import android.content.Context
import com.todaysday.planner.data.db.PlannerDatabase
import com.todaysday.planner.data.repository.EntryRepository
import com.todaysday.planner.data.repository.TagPlaceRepository

/**
 * AppContainer is a simple "toolbox" that creates the shared objects of the app
 * (database, repositories, the scheduler, ...) in ONE place.
 * Screens will ask this class for what they need.
 */
class AppContainer(val context: Context) {

    // `by lazy` means the object is only created the first time somebody uses it.
    val database: PlannerDatabase by lazy { PlannerDatabase.create(context) }

    val entryRepository: EntryRepository by lazy { EntryRepository(database.entryDao()) }

    val tagPlaceRepository: TagPlaceRepository by lazy {
        TagPlaceRepository(database.tagDao(), database.placeDao())
    }
}