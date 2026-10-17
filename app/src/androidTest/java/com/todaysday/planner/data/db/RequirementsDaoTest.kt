package com.todaysday.planner.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.todaysday.planner.data.db.dao.EntryDao
import com.todaysday.planner.domain.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Tests saving entries together with their "requires completion of" links. */
@RunWith(AndroidJUnit4::class)
class RequirementsDaoTest {

    private lateinit var db: PlannerDatabase
    private lateinit var entryDao: EntryDao

    private val created = Instant.ofEpochMilli(1_700_000_000_000)

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = PlannerDatabase.createInMemory(context)
        entryDao = db.entryDao()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    private suspend fun newTask(name: String): Long =
        entryDao.insert(Task(name = name, createdAt = created).toEntity())

    @Test
    fun new_entry_is_saved_together_with_its_requirements() = runBlocking {
        val a = newTask("A")
        val b = newTask("B")

        val c = entryDao.insertWithTagsAndRequirements(
            Task(name = "C", createdAt = created).toEntity(), emptySet(), setOf(a, b)
        )

        assertEquals(setOf(a, b), entryDao.getRequiredIds(c).toSet())
    }

    @Test
    fun updating_replaces_the_requirements() = runBlocking {
        val a = newTask("A")
        val b = newTask("B")
        val c = entryDao.insertWithTagsAndRequirements(
            Task(name = "C", createdAt = created).toEntity(), emptySet(), setOf(a)
        )

        val saved = entryDao.getById(c)!!.entry
        entryDao.updateWithTagsAndRequirements(saved.copy(name = "C renamed"), emptySet(), setOf(b))

        assertEquals(setOf(b), entryDao.getRequiredIds(c).toSet())
        assertEquals("C renamed", entryDao.getById(c)!!.entry.name)
    }

    @Test
    fun an_entry_cannot_require_itself() = runBlocking {
        val a = newTask("A")

        entryDao.setDependencies(a, setOf(a))

        assertEquals(emptyList<Long>(), entryDao.getRequiredIds(a))
    }

    @Test
    fun all_requirements_can_be_listed_at_once() = runBlocking {
        val a = newTask("A")
        val b = newTask("B")
        entryDao.setDependencies(b, setOf(a))

        val all = entryDao.observeAllDependencies().first()

        assertEquals(1, all.size)
        assertEquals(b, all.single().entryId)
        assertEquals(a, all.single().requiresEntryId)
    }
}