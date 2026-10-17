package com.todaysday.planner.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * "Requires completion of": entry `entryId` cannot be started before
 * entry `requiresEntryId` is complete.
 */
@Entity(
    tableName = "dependencies",
    primaryKeys = ["entryId", "requiresEntryId"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["requiresEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("requiresEntryId")]
)
data class DependencyEntity(
    val entryId: Long,
    val requiresEntryId: Long
)