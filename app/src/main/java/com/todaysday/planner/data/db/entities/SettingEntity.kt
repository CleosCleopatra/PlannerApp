package com.todaysday.planner.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The "settings" table: simple name/value pairs, e.g. "min_session_minutes" = "15". */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val name: String,
    val value: String
)