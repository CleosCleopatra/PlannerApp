package com.todaysday.planner.domain.progress

/**
 * How far along an entry is. This is what the progress bar and the small
 * "3 h 20 min left" text are built from.
 *
 * @param neededMinutes total minutes the entry needs (including children that count toward it)
 * @param doneMinutes   minutes already done (never more than needed, except for repeating work)
 * @param isComplete    the entry has been checked off
 * @param unitsTotal    number goals only: how many things in total
 * @param unitsDone     number goals only: how many are done
 */
data class Progress(
    val neededMinutes: Int,
    val doneMinutes: Int,
    val isComplete: Boolean,
    val unitsTotal: Int? = null,
    val unitsDone: Int? = null
) {
    /** Minutes still to do (never below zero). */
    val leftMinutes: Int
        get() = (neededMinutes - doneMinutes).coerceAtLeast(0)

    /** Number goals only: how many are left, e.g. 600 of 1000 words. */
    val unitsLeft: Int?
        get() = if (unitsTotal != null && unitsDone != null) {
            (unitsTotal - unitsDone).coerceAtLeast(0)
        } else {
            null
        }

    /** Does a progress bar make sense? Repeating entries have no end, so they get no bar. */
    val hasBar: Boolean
        get() = neededMinutes > 0 || isComplete

    /** Between 0.0 (nothing done) and 1.0 (everything done). */
    val fraction: Float
        get() = when {
            isComplete -> 1f
            neededMinutes <= 0 -> 0f
            else -> (doneMinutes.toFloat() / neededMinutes).coerceIn(0f, 1f)
        }
}