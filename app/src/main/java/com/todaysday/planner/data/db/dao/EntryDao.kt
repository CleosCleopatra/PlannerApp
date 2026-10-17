package com.todaysday.planner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.todaysday.planner.data.db.entities.DependencyEntity
import com.todaysday.planner.data.db.entities.EntryEntity
import com.todaysday.planner.data.db.entities.EntryTagCrossRef
import com.todaysday.planner.data.db.relations.EntryWithTags
import kotlinx.coroutines.flow.Flow

/**
 * A DAO ("data access object") is the list of things we can do with a table.
 * We only write the question (the @Query text); Room writes the code that runs it.
 *
 * - `suspend` functions run in the background without freezing the screen.
 * - Functions returning `Flow` keep sending the NEW list whenever the data changes,
 *   so screens update by themselves.
 */
@Dao
abstract class EntryDao {

    // ---------- Entries ----------

    @Insert
    abstract suspend fun insert(entry: EntryEntity): Long   // returns the new id

    @Update
    abstract suspend fun update(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Transaction // @Transaction is needed because tags are read with a second query
    @Query("SELECT * FROM entries WHERE id = :id")
    abstract suspend fun getById(id: Long): EntryWithTags?

    @Transaction
    @Query("SELECT * FROM entries ORDER BY createdAtMillis")
    abstract fun observeAll(): Flow<List<EntryWithTags>>

    @Transaction
    @Query("SELECT * FROM entries ORDER BY createdAtMillis")
    abstract suspend fun getAll(): List<EntryWithTags>

    @Transaction
    @Query("SELECT * FROM entries WHERE parentId = :parentId ORDER BY createdAtMillis")
    abstract fun observeChildren(parentId: Long): Flow<List<EntryWithTags>>

    // ---------- Tags on entries ----------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTagLinks(links: List<EntryTagCrossRef>)

    @Query("DELETE FROM entry_tags WHERE entryId = :entryId")
    abstract suspend fun clearTags(entryId: Long)

    // These two save the entry AND its tags in one step. If something fails halfway,
    // nothing is saved (that is what @Transaction guarantees).
    @Transaction
    open suspend fun insertWithTags(entry: EntryEntity, tagIds: Set<Long>): Long {
        val id = insert(entry)
        insertTagLinks(tagIds.map { EntryTagCrossRef(entryId = id, tagId = it) })
        return id
    }

    @Transaction
    open suspend fun updateWithTags(entry: EntryEntity, tagIds: Set<Long>) {
        update(entry)
        clearTags(entry.id)
        insertTagLinks(tagIds.map { EntryTagCrossRef(entryId = entry.id, tagId = it) })
    }

    // ---------- "Requires completion of" ----------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertDependency(dependency: DependencyEntity)

    @Query("DELETE FROM dependencies WHERE entryId = :entryId AND requiresEntryId = :requiresEntryId")
    abstract suspend fun deleteDependency(entryId: Long, requiresEntryId: Long)

    @Query("SELECT requiresEntryId FROM dependencies WHERE entryId = :entryId")
    abstract suspend fun getRequiredIds(entryId: Long): List<Long>

    @Query("DELETE FROM dependencies WHERE entryId = :entryId")
    abstract suspend fun clearDependencies(entryId: Long)

    /** Replaces ALL requirements of an entry with the given ones. An entry can't require itself. */
    @Transaction
    open suspend fun setDependencies(entryId: Long, requiredIds: Set<Long>) {
        clearDependencies(entryId)
        requiredIds
            .filter { it != entryId }
            .forEach { insertDependency(DependencyEntity(entryId = entryId, requiresEntryId = it)) }
    }

    /** Saves a new entry together with its tags and requirements, all or nothing. */
    @Transaction
    open suspend fun insertWithTagsAndRequirements(
        entry: EntryEntity,
        tagIds: Set<Long>,
        requiredIds: Set<Long>
    ): Long {
        val id = insertWithTags(entry, tagIds)
        setDependencies(id, requiredIds)
        return id
    }

    /** Updates an entry together with its tags and requirements, all or nothing. */
    @Transaction
    open suspend fun updateWithTagsAndRequirements(
        entry: EntryEntity,
        tagIds: Set<Long>,
        requiredIds: Set<Long>
    ) {
        updateWithTags(entry, tagIds)
        setDependencies(entry.id, requiredIds)
    }

    @Query("SELECT * FROM dependencies")
    abstract fun observeAllDependencies(): Flow<List<DependencyEntity>>
}