package com.todaysday.planner.data.repository

import com.todaysday.planner.data.db.dao.SettingDao
import com.todaysday.planner.data.db.entities.SettingEntity
import com.todaysday.planner.domain.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The door to the simple settings (session lengths, when a new day starts). */
class SettingsRepository(private val dao: SettingDao) {

    /** The settings. Sends a new value every time one changes. Missing values use the defaults. */
    fun observeSettings(): Flow<AppSettings> =
        dao.observeAll().map { rows -> AppSettings.fromMap(rows.associate { it.name to it.value }) }

    /** Saves the settings (after fixing values that make no sense, like a maximum below the minimum). */
    suspend fun update(settings: AppSettings) {
        val rows = settings.sanitized().toMap().map { (name, value) -> SettingEntity(name, value) }
        dao.upsertAll(rows)
    }
}