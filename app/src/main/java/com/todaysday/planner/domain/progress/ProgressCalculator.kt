package com.todaysday.planner.domain.progress

import com.todaysday.planner.domain.model.Entry
import com.todaysday.planner.domain.model.Event
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.model.GoalKind
import com.todaysday.planner.domain.model.Repeating
import com.todaysday.planner.domain.model.Task
import com.todaysday.planner.domain.tree.EntryTree
import java.time.Duration

/**
 * Works out how much of each entry is done and how much is left.
 *
 * @param tree          ALL entries (also the finished ones), so the totals add up correctly
 * @param loggedMinutes how many minutes have been worked on each entry so far, by entry id.
 *                      This comes from the work history that arrives in M13. Until then it's empty.
 *
 * The rules:
 * - Time goal, achievement goal and task: needed = the time set on it, done = minutes logged.
 *   Logged time beyond the needed time is not counted twice.
 * - Number goal: needed = how many x minutes each, done = units done x minutes each.
 * - Event: needed = its length.
 * - Repeating: it has no end, so nothing is "needed". But the time you log on it counts as
 *   done time for its parent (if it counts toward the parent).
 * - Parent roll-up: an entry's totals also include its children that are set to
 *   "counts toward parent" (and their children that count, and so on).
 *   Independent children are shown in the tree but never change the parent's bar.
 * - An entry that is checked off is always fully done.
 */
class ProgressCalculator(
    private val tree: EntryTree,
    private val loggedMinutes: Map<Long, Int> = emptyMap()
) {

    /** Progress for the entry with this id (null if it isn't in the tree). */
    fun progressOf(id: Long): Progress? = tree[id]?.let { progressOf(it) }

    fun progressOf(entry: Entry): Progress {
        val totals = totalsOf(entry, emptySet())

        // Units are only shown for the number goal itself, not rolled up from children.
        var unitsTotal: Int? = null
        var unitsDone: Int? = null
        if (entry is Goal && entry.kind == GoalKind.NUMBER) {
            unitsTotal = entry.numberTarget
            unitsDone = entry.numberDone
        }

        return Progress(
            neededMinutes = totals.needed,
            doneMinutes = totals.done,
            isComplete = entry.completedAt != null,
            unitsTotal = unitsTotal,
            unitsDone = unitsDone
        )
    }

    // A small holder for two numbers.
    private class Totals(val needed: Int, val done: Int)

    /**
     * The totals of an entry: its own time plus its counting children, recursively.
     * `path` holds the ids we are already inside, so broken data (a loop) can't make
     * this run forever.
     */
    private fun totalsOf(entry: Entry, path: Set<Long>): Totals {
        if (entry.id in path) return Totals(0, 0)

        val own = ownTotals(entry)
        var needed = own.needed
        var done = own.done

        val newPath = path + entry.id
        for (child in tree.childrenOf(entry.id)) {
            if (!child.countsTowardParent) continue // independent child: leave it out
            val childTotals = totalsOf(child, newPath)
            needed += childTotals.needed
            done += childTotals.done
        }

        // A checked-off entry counts as fully done.
        if (entry.completedAt != null) done = maxOf(done, needed)

        return Totals(needed, done)
    }

    /** Only this entry's own time, without its children. */
    private fun ownTotals(entry: Entry): Totals {
        val logged = loggedMinutes[entry.id] ?: 0

        return when (entry) {
            is Goal -> {
                val target = entry.numberTarget
                val perUnit = entry.minutesPerUnit
                if (entry.kind == GoalKind.NUMBER && target != null && perUnit != null) {
                    val needed = (target * perUnit).coerceAtLeast(0)
                    Totals(needed, (entry.numberDone * perUnit).coerceIn(0, needed))
                } else {
                    timeBased(entry.timeNeededMinutes, logged)
                }
            }

            is Task -> timeBased(entry.timeNeededMinutes, logged)

            is Event -> {
                val length = Duration.between(entry.start, entry.end).toMinutes().toInt()
                timeBased(length, logged)
            }

            // No end, so nothing is needed. The logged time still counts as done time.
            is Repeating -> Totals(0, logged)
        }
    }

    private fun timeBased(neededMinutes: Int, logged: Int): Totals {
        val needed = neededMinutes.coerceAtLeast(0)
        return Totals(needed, logged.coerceIn(0, needed))
    }
}