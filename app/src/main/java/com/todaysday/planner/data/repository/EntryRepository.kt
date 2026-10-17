package com.todaysday.planner.data.repository

import com.todaysday.planner.data.db.dao.EntryDao
import com.todaysday.planner.data.db.toDomain
import com.todaysday.planner.data.db.toEntity
import com.todaysday.planner.domain.model.Entry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The only door the rest of the app uses to reach entries.
 * Screens never talk to the DAO directly: they talk to the repository, which hands out
 * and accepts domain classes (Goal, Task, ...) and hides the database rows.
 */
class EntryRepository(private val entryDao: EntryDao) {

    /** All entries. Sends a new list every time anything changes in the database. */
    fun observeEntries(): Flow<List<Entry>> =
        entryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** One entry by id, or null if it doesn't exist. */
    suspend fun getEntry(id: Long): Entry? = entryDao.getById(id)?.toDomain()

    /**
     * All "requires completion of" links at once: entry id -> the ids it requires.
     * Sends a new map every time a link changes.
     */
    fun observeRequirements(): Flow<Map<Long, Set<Long>>> =
        entryDao.observeAllDependencies().map { rows ->
            rows.groupBy({ it.entryId }, { it.requiresEntryId })
                .mapValues { (_, ids) -> ids.toSet() }
        }

    /** The ids this entry requires to be completed first. */
    suspend fun getRequiredIds(entryId: Long): Set<Long> =
        entryDao.getRequiredIds(entryId).toSet()

    /**
     * Saves an entry together with its tags. An entry with id 0 is new and gets inserted;
     * any other id updates the existing entry. Returns the id of the saved entry.
     *
     * @param requiredIds the entries it requires to be completed first.
     *                    null = leave the existing requirements as they are.
     */
    suspend fun save(entry: Entry, requiredIds: Set<Long>? = null): Long {
        val row = entry.toEntity()
        return if (entry.id == 0L) {
            if (requiredIds == null) {
                entryDao.insertWithTags(row, entry.tagIds)
            } else {
                entryDao.insertWithTagsAndRequirements(row, entry.tagIds, requiredIds)
            }
        } else {
            if (requiredIds == null) {
                entryDao.updateWithTags(row, entry.tagIds)
            } else {
                entryDao.updateWithTagsAndRequirements(row, entry.tagIds, requiredIds)
            }
            entry.id
        }
    }

    /** Deletes one entry. Its children are kept, they just become top-level entries. */
    suspend fun delete(id: Long) = entryDao.deleteById(id)
}