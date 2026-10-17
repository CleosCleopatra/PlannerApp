package com.todaysday.planner.domain.picker

import com.todaysday.planner.domain.tree.DependencyGraph
import com.todaysday.planner.domain.tree.EntryTree

/**
 * Works out which entries must be greyed out in a picker, to stop loops.
 * For an entry that doesn't exist yet (entryId = null) nothing needs to be greyed out,
 * because nothing can point at it yet.
 */
object PickerRules {

    /**
     * The goals that [entryId] must NOT be placed in:
     * - itself and everything inside it, and
     * - any goal that would make things wait for each other forever, given the requirements.
     *
     * @param requirements entry id -> the ids it requires. The requirements of [entryId]
     *                     itself should be the ones currently chosen in the form.
     */
    fun disabledParents(
        tree: EntryTree,
        entryId: Long?,
        requirements: Map<Long, Set<Long>> = emptyMap()
    ): Set<Long> {
        if (entryId == null) return emptySet()
        val ownRequirements = requirements[entryId].orEmpty()
        return tree.all()
            .filter { candidate ->
                choicesCreateCycle(tree, requirements, entryId, candidate.id, ownRequirements)
            }
            .map { it.id }
            .toSet()
    }

    /** The entries that [entryId] must NOT require: itself, and anything that would wait for it. */
    fun disabledRequirements(graph: DependencyGraph, tree: EntryTree, entryId: Long?): Set<Long> {
        if (entryId == null) return emptySet()
        return tree.all()
            .filter { graph.wouldCreateCycle(entryId, it.id) }
            .map { it.id }
            .toSet()
    }

    /**
     * The final check before saving: would these two choices together create a loop?
     * - newParentId: the goal the entry is placed in (null = top level)
     * - requiredIds: what the entry requires
     * Both are tried out on a copy of the data, nothing is changed for real.
     */
    fun choicesCreateCycle(
        tree: EntryTree,
        requirements: Map<Long, Set<Long>>,
        entryId: Long?,
        newParentId: Long?,
        requiredIds: Set<Long>
    ): Boolean {
        if (entryId == null) return false // a new entry can't be pointed at by anything yet
        if (tree.wouldCreateCycle(entryId, newParentId)) return true

        val movedTree = tree.withParentOf(entryId, newParentId)
        val tryRequirements = requirements + (entryId to requiredIds)
        return DependencyGraph(movedTree, tryRequirements).hasCycle()
    }
}