package com.todaysday.planner.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.todaysday.planner.data.db.dao.EntryDao
import com.todaysday.planner.data.db.dao.PlaceDao
import com.todaysday.planner.data.db.dao.TagDao
import com.todaysday.planner.data.db.entities.DependencyEntity
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Tests the DAOs against a real (but temporary, in-memory) database on the phone.
 * Your real app data is NOT touched: every test starts with an empty database.
 */
@RunWith(AndroidJUnit4::class)
class EntryDaoTest {

    private lateinit var db: PlannerDatabase
    private lateinit var entryDao: EntryDao
    private lateinit var tagDao: TagDao
    private lateinit var placeDao: PlaceDao

    private val created = Instant.ofEpochMilli(1_700_000_000_000)

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = PlannerDatabase.createInMemory(context)
        entryDao = db.entryDao()
        tagDao = db.tagDao()
        placeDao = db.placeDao()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    @Test
    fun entry_is_saved_and_loaded_with_its_tags() = runBlocking {
        val serbian = tagDao.getOrCreate("Serbian")
        val study = tagDao.getOrCreate("Study")

        val id = entryDao.insertWithTags(
            Goal(name = "Learn Serbian", createdAt = created).toEntity(),
            setOf(serbian.id, study.id)
        )

        val loaded = entryDao.getById(id)!!.toDomain()
        assertEquals("Learn Serbian", loaded.name)
        assertEquals(setOf(serbian.id, study.id), loaded.tagIds)
    }

    @Test
    fun children_query_returns_only_the_children_of_that_entry() = runBlocking {
        val parentId = entryDao.insert(Goal(name = "Learn Serbian", createdAt = created).toEntity())
        entryDao.insert(Task(name = "Buy book", parentId = parentId, createdAt = created).toEntity())
        entryDao.insert(Task(name = "Find tutor", parentId = parentId, createdAt = created).toEntity())
        entryDao.insert(Task(name = "Unrelated", createdAt = created).toEntity())

        val children = entryDao.observeChildren(parentId).first().map { it.toDomain().name }
        assertEquals(setOf("Buy book", "Find tutor"), children.toSet())
    }

    @Test
    fun deleting_a_parent_keeps_the_child_but_clears_its_link() = runBlocking {
        val parentId = entryDao.insert(Goal(name = "Parent", createdAt = created).toEntity())
        val childId = entryDao.insert(Task(name = "Child", parentId = parentId, createdAt = created).toEntity())

        entryDao.deleteById(parentId)

        assertNull(entryDao.getById(childId)!!.toDomain().parentId)
    }

    @Test
    fun updating_with_tags_replaces_the_old_tags() = runBlocking {
        val a = tagDao.getOrCreate("A")
        val b = tagDao.getOrCreate("B")
        val id = entryDao.insertWithTags(Task(name = "Task", createdAt = created).toEntity(), setOf(a.id))

        val saved = entryDao.getById(id)!!.entry
        entryDao.updateWithTags(saved.copy(name = "Renamed"), setOf(b.id))

        val loaded = entryDao.getById(id)!!.toDomain()
        assertEquals("Renamed", loaded.name)
        assertEquals(setOf(b.id), loaded.tagIds)
    }

    @Test
    fun deleting_a_tag_removes_it_from_entries() = runBlocking {
        val tag = tagDao.getOrCreate("Temp")
        val id = entryDao.insertWithTags(Task(name = "Task", createdAt = created).toEntity(), setOf(tag.id))

        tagDao.deleteById(tag.id)

        assertEquals(emptySet<Long>(), entryDao.getById(id)!!.toDomain().tagIds)
    }

    @Test
    fun tag_names_are_not_case_sensitive() = runBlocking {
        val first = tagDao.getOrCreate("Serbian")
        val second = tagDao.getOrCreate("  serbian ")
        assertEquals(first.id, second.id)
    }

    @Test
    fun deleting_a_place_makes_entries_anywhere_again() = runBlocking {
        val home = placeDao.getOrCreate("Home")
        val id = entryDao.insert(Task(name = "Task", placeId = home.id, createdAt = created).toEntity())

        placeDao.deleteById(home.id)

        assertNull(entryDao.getById(id)!!.toDomain().placeId)
    }

    @Test
    fun dependencies_are_saved_and_removed_with_their_entries() = runBlocking {
        val first = entryDao.insert(Task(name = "First", createdAt = created).toEntity())
        val second = entryDao.insert(Task(name = "Second", createdAt = created).toEntity())

        entryDao.insertDependency(DependencyEntity(entryId = second, requiresEntryId = first))
        assertEquals(listOf(first), entryDao.getRequiredIds(second))

        entryDao.deleteById(first)
        assertEquals(emptyList<Long>(), entryDao.getRequiredIds(second))
    }
}