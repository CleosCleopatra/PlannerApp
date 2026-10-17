package com.todaysday.planner.data.db.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.todaysday.planner.data.db.entities.EntryEntity
import com.todaysday.planner.data.db.entities.EntryTagCrossRef
import com.todaysday.planner.data.db.entities.TagEntity

/**
 * An entry together with its tags, read in one go.
 *
 * @Embedded   = the columns of the entry itself
 * @Relation   = "also fetch the tags". The tags are linked through the entry_tags table
 *               (the Junction), which is where the pairs (entryId, tagId) are stored.
 */
data class EntryWithTags(
    @Embedded val entry: EntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryTagCrossRef::class,
            parentColumn = "entryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)