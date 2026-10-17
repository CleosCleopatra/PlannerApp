package com.todaysday.planner

import android.content.Context
import com.todaysday.planner.data.db.PlannerDatabase
import com.todaysday.planner.data.repository.EntryRepository
import com.todaysday.planner.data.repository.SettingsRepository
import com.todaysday.planner.data.repository.TagPlaceRepository
import com.todaysday.planner.data.repository.TimeRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * AppContainer is a simple "toolbox" that creates the shared objects of the app
 * (database, repositories, the scheduler, ...) in ONE place.
 * Screens will ask this class for what they need.
 */
class AppContainer(val context: Context) {

    // A scope for work that belongs to the whole app and not to one screen
    // (for example creating the default rules when the app starts).
    // SupervisorJob: if one piece of work fails, the others keep running.
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // `by lazy` means the object is only created the first time somebody uses it.
    val database: PlannerDatabase by lazy { PlannerDatabase.create(context) }

    val entryRepository: EntryRepository by lazy { EntryRepository(database.entryDao()) }

    val tagPlaceRepository: TagPlaceRepository by lazy {
        TagPlaceRepository(database.tagDao(), database.placeDao())
    }

    val timeRuleRepository: TimeRuleRepository by lazy {
        TimeRuleRepository(database.timeRuleDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(database.settingDao())
    }
}