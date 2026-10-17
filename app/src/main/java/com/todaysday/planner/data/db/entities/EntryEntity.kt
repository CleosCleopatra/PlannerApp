package com.todaysday.planner.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.todaysday.planner.domain.model.EntryType
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.Level
import com.todaysday.planner.domain.model.RepeatType

/**
 * The "entries" table. ALL four kinds (goal, task, event, repeating) live in this one table.
 * The `type` column says which kind a row is; columns that don't apply to a kind stay empty (null).
 * One table makes the tree simple: every row just points to its parent with `parentId`.
 *
 * Foreign keys keep the data consistent. SET_NULL means: if a parent entry or a place is
 * deleted, the rows that pointed to it are NOT deleted, their link is just cleared.
 * (In a later stage the delete button will ask what to do with the children.)
 */
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    // Indices make looking up "all children of X" or "all entries of type Y" fast.
    indices = [Index("parentId"), Index("placeId"), Index("type")]
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    val parentId: Long? = null,
    val name: String,
    val description: String = "",
    val priority: Level = Level.NORMAL,
    val effort: Level = Level.NORMAL,
    val dueDateEpochDay: Long? = null,     // LocalDate saved as a day number
    val placeId: Long? = null,             // null = Anywhere
    val countsTowardParent: Boolean = true,
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null,

    // --- goals (and tasks use timeNeededMinutes too) ---
    val goalKind: GoalKind? = null,
    val timeNeededMinutes: Int? = null,
    val numberTarget: Int? = null,
    val minutesPerUnit: Int? = null,
    val numberDone: Int = 0,

    // --- events ---
    val eventStartMillis: Long? = null,
    val eventEndMillis: Long? = null,
    val externalId: String? = null,

    // --- repeating ---
    val repeatType: RepeatType? = null,
    val repeatWeekdaysMask: Int = 0,       // weekdays saved as bits: Monday = 1, Tuesday = 2, Wednesday = 4 ...
    val repeatN: Int = 1,
    val repeatAnchorEpochDay: Long? = null,
    val sessionMinutes: Int? = null
)