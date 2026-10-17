package com.todaysday.planner.data.repository

import com.todaysday.planner.data.db.dao.PlaceDao
import com.todaysday.planner.data.db.dao.TagDao
import com.todaysday.planner.data.db.toDomain
import com.todaysday.planner.domain.model.Place
import com.todaysday.planner.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tags and places: list them, and "select an existing one or create a new one". */
class TagPlaceRepository(
    private val tagDao: TagDao,
    private val placeDao: PlaceDao
) {
    fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observePlaces(): Flow<List<Place>> =
        placeDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /**
     * Returns the tag with this name (upper/lower case doesn't matter), creating it if it
     * doesn't exist yet. Returns null if the name is empty.
     */
    suspend fun getOrCreateTag(name: String): Tag? =
        name.trim().takeIf { it.isNotEmpty() }?.let { tagDao.getOrCreate(it).toDomain() }

    /** Same for places. */
    suspend fun getOrCreatePlace(name: String): Place? =
        name.trim().takeIf { it.isNotEmpty() }?.let { placeDao.getOrCreate(it).toDomain() }
}