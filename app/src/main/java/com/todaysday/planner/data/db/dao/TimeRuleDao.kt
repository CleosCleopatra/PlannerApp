package com.todaysday.planner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.todaysday.planner.data.db.entities.SettingEntity
import com.todaysday.planner.data.db.entities.TimeRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TimeRuleDao {

    /** All rules. Sends a new list every time one is added, changed or deleted. */
    @Query("SELECT * FROM time_rules ORDER BY id")
    abstract fun observeAll(): Flow<List<TimeRuleEntity>>

    @Query("SELECT * FROM time_rules ORDER BY id")
    abstract suspend fun getAll(): List<TimeRuleEntity>

    @Insert
    abstract suspend fun insert(rule: TimeRuleEntity): Long   // returns the new id

    @Insert
    abstract suspend fun insertAll(rules: List<TimeRuleEntity>)

    @Update
    abstract suspend fun update(rule: TimeRuleEntity)

    @Query("UPDATE time_rules SET enabled = :enabled WHERE id = :id")
    abstract suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM time_rules WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    // ---------- Seeding: creating the default rules once ----------

    @Query("SELECT value FROM settings WHERE name = :name")
    abstract suspend fun readSetting(name: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun writeSetting(setting: SettingEntity)

    /**
     * Inserts the given rules, but only if this has never been done before. A flag in the
     * settings table remembers that it happened, so rules you delete later are not
     * created again. Returns true if the rules were inserted now.
     * @Transaction makes the check and the inserts happen as one step.
     */
    @Transaction
    open suspend fun seedDefaults(rules: List<TimeRuleEntity>, flagName: String): Boolean {
        if (readSetting(flagName) != null) return false
        insertAll(rules)
        writeSetting(SettingEntity(name = flagName, value = "true"))
        return true
    }
}