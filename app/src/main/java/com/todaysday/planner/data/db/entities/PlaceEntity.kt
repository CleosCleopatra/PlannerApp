package com.todaysday.planner.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** The "places" table. Place names are unique. */
@Entity(
    tableName = "places",
    indices = [Index(value = ["name"], unique = true)]
)
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)