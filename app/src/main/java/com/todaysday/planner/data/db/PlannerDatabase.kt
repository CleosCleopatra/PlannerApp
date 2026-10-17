package com.todaysday.planner.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.todaysday.planner.data.db.dao.EntryDao
import com.todaysday.planner.data.db.dao.PlaceDao
import com.todaysday.planner.data.db.dao.SettingDao
import com.todaysday.planner.data.db.dao.TagDao
import com.todaysday.planner.data.db.dao.TimeRuleDao
import com.todaysday.planner.data.db.entities.DependencyEntity
import com.todaysday.planner.data.db.entities.EntryEntity
import com.todaysday.planner.data.db.entities.EntryTagCrossRef
import com.todaysday.planner.data.db.entities.PlaceEntity
import com.todaysday.planner.data.db.entities.SettingEntity
import com.todaysday.planner.data.db.entities.TagEntity
import com.todaysday.planner.data.db.entities.TimeRuleEntity

/**
 * The app's database. Room reads the annotations below and writes the SQL code for us.
 *
 * `version` goes up every time the tables change, and an automatic migration tells Room how
 * to get from the old layout to the new one without losing your data. Room compares with the
 * saved copies of the old layouts in app/schemas, so never delete the files in that folder.
 *
 * Version 2 added the time_rules and settings tables.
 */
@Database(
    entities = [
        EntryEntity::class,
        TagEntity::class,
        EntryTagCrossRef::class,
        PlaceEntity::class,
        DependencyEntity::class,
        TimeRuleEntity::class,
        SettingEntity::class
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
abstract class PlannerDatabase : RoomDatabase() {

    // Room fills these in: each one gives access to the DAO for some of the tables.
    abstract fun entryDao(): EntryDao
    abstract fun tagDao(): TagDao
    abstract fun placeDao(): PlaceDao
    abstract fun timeRuleDao(): TimeRuleDao
    abstract fun settingDao(): SettingDao

    companion object {
        /** The real database, saved in a file on the phone. */
        fun create(context: Context): PlannerDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PlannerDatabase::class.java,
                "planner.db"
            ).build()

        /** A throw-away database that lives only in memory. Used by tests. */
        fun createInMemory(context: Context): PlannerDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                PlannerDatabase::class.java
            ).build()
    }
}