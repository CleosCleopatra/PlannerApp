package com.todaysday.planner.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Links entries and tags. One entry can have many tags and one tag can be on many entries,
 * so this table holds one row per (entry, tag) pair.
 * CASCADE means: if an entry or tag is deleted, its rows here are deleted too.
 */
@Entity(
    tableName = "entry_tags",
    primaryKeys = ["entryId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class EntryTagCrossRef(
    val entryId: Long,
    val tagId: Long
)