package com.todaysday.planner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todaysday.planner.data.db.entities.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SettingDao {

    /** All settings. Sends a new list every time one changes. */
    @Query("SELECT * FROM settings")
    abstract fun observeAll(): Flow<List<SettingEntity>>

    @Query("SELECT value FROM settings WHERE name = :name")
    abstract suspend fun get(name: String): String?

    // REPLACE: saving a setting that already exists overwrites it.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(settings: List<SettingEntity>)
}