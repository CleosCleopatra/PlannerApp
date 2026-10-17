package com.todaysday.planner.data.repository

import com.todaysday.planner.data.db.dao.TimeRuleDao
import com.todaysday.planner.data.db.toDomain
import com.todaysday.planner.data.db.toEntity
import com.todaysday.planner.domain.rules.DefaultRules
import com.todaysday.planner.domain.rules.RuleEngine
import com.todaysday.planner.domain.rules.TimeRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The door to the time rules, like EntryRepository is for entries. */
class TimeRuleRepository(private val dao: TimeRuleDao) {

    /** All rules. Sends a new list every time anything changes. */
    fun observeRules(): Flow<List<TimeRule>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** A ready-to-use RuleEngine, replaced by a new one every time a rule changes. */
    fun observeEngine(): Flow<RuleEngine> =
        observeRules().map { RuleEngine(it) }

    /** Saves a rule. A rule with id 0 is new and gets inserted; any other id updates it. Returns the id. */
    suspend fun save(rule: TimeRule): Long =
        if (rule.id == 0L) {
            dao.insert(rule.toEntity())
        } else {
            dao.update(rule.toEntity())
            rule.id
        }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.deleteById(id)

    /**
     * Creates the default rules the very first time. Later calls do nothing, also if you
     * have deleted rules in the meantime. Returns true if the rules were created now.
     */
    suspend fun seedDefaultsIfNeeded(): Boolean =
        dao.seedDefaults(DefaultRules.initial().map { it.toEntity() }, SEEDED_FLAG)

    companion object {
        const val SEEDED_FLAG = "default_rules_seeded"
    }
}