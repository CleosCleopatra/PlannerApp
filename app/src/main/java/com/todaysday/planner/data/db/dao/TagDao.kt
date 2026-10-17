package com.todaysday.planner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.todaysday.planner.data.db.entities.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TagDao {

    // IGNORE: if a tag with that exact name exists, do nothing (the result is -1).
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(tag: TagEntity): Long

    @Update
    abstract suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    // COLLATE NOCASE ignores upper/lower case, so "serbian" finds "Serbian".
    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<TagEntity>>

    /** "Select existing or create new": returns the tag with this name, creating it if needed. */
    @Transaction
    open suspend fun getOrCreate(name: String): TagEntity {
        val cleanName = name.trim()
        findByName(cleanName)?.let { return it }
        val id = insertIgnore(TagEntity(name = cleanName))
        return if (id != -1L) TagEntity(id = id, name = cleanName) else findByName(cleanName)!!
    }
}