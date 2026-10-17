package com.todaysday.planner.domain.tree

/**
 * Knows which entries wait for which other entries ("requires completion of").
 *
 * @param tree         ALL entries, finished ones too (we need to know what is finished)
 * @param requirements entry id -> the ids of the entries it requires to be completed first
 *
 * One rule shapes everything here: waiting is INHERITED. If a goal waits for something,
 * everything inside that goal waits for it too. So "what an entry waits for" is what it
 * requires itself plus what each of its parent goals require.
 */
class DependencyGraph(
    private val tree: EntryTree,
    private val requirements: Map<Long, Set<Long>>
) {
    /** What this entry itself requires (not counting its parents). */
    fun requirementsOf(id: Long): Set<Long> = requirements[id].orEmpty()

    /** Everything that must be completed before this entry can start, parents' requirements included. */
    fun waitsFor(id: Long): Set<Long> = waitsFor(id, requirements)

    private fun waitsFor(id: Long, reqs: Map<Long, Set<Long>>): Set<Long> =
        tree.pathTo(id).flatMap { reqs[it.id].orEmpty() }.toSet()

    /**
     * True if something this entry waits for is not finished yet.
     * (A required entry that no longer exists is ignored.)
     */
    fun isBlocked(id: Long): Boolean = waitsFor(id).any { requiredId ->
        val required = tree[requiredId]
        required != null && required.completedAt == null
    }

    /** All entries that cannot be started yet. Callers usually skip the finished ones. */
    fun blockedIds(): Set<Long> =
        tree.all().filter { isBlocked(it.id) }.map { it.id }.toSet()


    /** True if, anywhere, things wait for each other in a circle (so none of them could ever start). */
    fun hasCycle(): Boolean = hasCycle(requirements)
    /**
     * Would making [entryId] require [requiredId] create a loop where things wait for each
     * other forever? That includes an entry requiring itself, or requiring something inside itself.
     * Use this before saving a new requirement.
     */
    fun wouldCreateCycle(entryId: Long, requiredId: Long): Boolean {
        // An entry cannot require itself.
        if (entryId == requiredId) return true

        if (requiredId in tree.descendantsOf(entryId).map {it.id}){
            return true
        }

        // Entry 1 requiring entry 2 means entry 2 cannot require entry 1.
        // Check whether entryId is reachable from requiredId through
        // the existing dependency relationships.
        val visited = mutableSetOf<Long>()

        fun reachesEntry(id: Long): Boolean {
            if (id == entryId) return true
            if (!visited.add(id)) return false

            return waitsFor(id).any { dependency ->
                reachesEntry(dependency)
            }
        }

        // Descendants inherit requirements, so requiring a descendant
        // of entryId would also create a cycle.
        if (requiredId in tree.descendantsOf(entryId).map { it.id }) {
            return true
        }

        return reachesEntry(requiredId)
    }

    /** Looks for a loop anywhere in the "waits for" relation. */
    private fun hasCycle(reqs: Map<Long, Set<Long>>): Boolean {
        // Per entry: 1 = we are still exploring it, 2 = fully explored, no loop found.
        val state = HashMap<Long, Int>()

        // Returns true as soon as we reach an entry that we are still exploring: that's a loop.
        fun visit(id: Long): Boolean {
            when (state[id]) {
                1 -> return true
                2 -> return false
            }
            state[id] = 1
            for (next in waitsFor(id, reqs)) {
                if (visit(next)) return true
            }
            state[id] = 2
            return false
        }

        return tree.all().any { visit(it.id) }
    }
}