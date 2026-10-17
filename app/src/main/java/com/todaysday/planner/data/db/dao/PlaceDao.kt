package com.todaysday.planner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.todaysday.planner.data.db.entities.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(place: PlaceEntity): Long

    @Update
    abstract suspend fun update(place: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("SELECT * FROM places WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findByName(name: String): PlaceEntity?

    @Query("SELECT * FROM places ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<PlaceEntity>>

    /** Returns the place with this name, creating it if it doesn't exist yet. */
    @Transaction
    open suspend fun getOrCreate(name: String): PlaceEntity {
        val cleanName = name.trim()
        findByName(cleanName)?.let { return it }
        val id = insertIgnore(PlaceEntity(name = cleanName))
        return if (id != -1L) PlaceEntity(id = id, name = cleanName) else findByName(cleanName)!!
    }
}