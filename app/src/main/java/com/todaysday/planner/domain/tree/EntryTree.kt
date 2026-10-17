package com.todaysday.planner.domain.tree

import com.todaysday.planner.domain.model.Entry

/**
 * A read-only "map" of how entries are nested inside each other.
 *
 * You give it a flat list of entries (every entry knows its parentId). It builds lookup
 * tables so we can quickly ask "what are the children of X?", "what is the path to Y?"
 * and "would moving Z under W create a loop?".
 *
 * It never changes the entries and never touches the database.
 * To get a new view you create a new EntryTree.
 */
class EntryTree private constructor(
    private val entries: List<Entry>,
    rawParentIds: Map<Long, Long?>
) {
    /** The normal way to build a tree: from the parentId of each entry. */
    constructor(entries: List<Entry>) : this(entries, entries.associate { it.id to it.parentId })

    // id -> entry, for fast lookups.
    private val byId: Map<Long, Entry> = entries.associateBy { it.id }

    // id -> parent id. If an entry points to a parent that is not in the list
    // (for example because it was filtered out), we treat the entry as top level.
    private val parentOfId: Map<Long, Long?> = entries.associate { entry ->
        entry.id to rawParentIds[entry.id]?.takeIf { it in byId }
    }

    // parent id -> its children, in the same order as the original list.
    // The key null holds the top-level entries.
    private val childMap: Map<Long?, List<Entry>> = entries.groupBy { parentOfId[it.id] }

    /** Find an entry by id (null if it isn't in this tree). Use it as tree[5]. */
    operator fun get(id: Long): Entry? = byId[id]

    /** The top-level entries (the ones that are not part of anything). */
    fun roots(): List<Entry> = childMap[null].orEmpty()

    /** Every entry in this tree, in the original order. */
    fun all(): List<Entry> = entries

    /**
     * A copy of this tree in which [entryId] has a different parent. Used to ask
     * "what would happen if I moved it?" without changing anything for real.
     */
    fun withParentOf(entryId: Long, newParentId: Long?): EntryTree =
        EntryTree(entries, parentOfId + (entryId to newParentId))

    /** The entries directly under this one. */
    fun childrenOf(id: Long): List<Entry> = childMap[id].orEmpty()

    fun hasChildren(id: Long): Boolean = childrenOf(id).isNotEmpty()

    /**
     * The way from the top down to this entry, including the entry itself.
     * Example: Learn Serbian -> Finish my book -> Chapter 1.
     * Useful for a "breadcrumb" line at the top of the screen.
     */
    fun pathTo(id: Long): List<Entry> {
        val path = mutableListOf<Entry>()
        val seen = mutableSetOf<Long>() // protects against endless loops in broken data
        var current: Long? = id
        while (current != null && seen.add(current)) {
            val entry = byId[current] ?: break
            path.add(entry)
            current = parentOfId[current]
        }
        return path.reversed()
    }

    /** How deep an entry is: 0 for top level, 1 for a child of a top-level entry, and so on. */
    fun depthOf(id: Long): Int = (pathTo(id).size - 1).coerceAtLeast(0)

    /** Every entry below this one (children, grandchildren, ...), parents before their children. */
    fun descendantsOf(id: Long): List<Entry> {
        val result = mutableListOf<Entry>()
        val seen = mutableSetOf(id)

        fun visit(parentId: Long) {
            for (child in childrenOf(parentId)) {
                if (seen.add(child.id)) {
                    result.add(child)
                    visit(child.id)
                }
            }
        }

        visit(id)
        return result
    }

    /**
     * Would making [entryId] a child of [newParentId] create a loop?
     * (An entry can't be part of itself or of anything below it.)
     * Use this before saving a "part of" choice.
     */
    fun wouldCreateCycle(entryId: Long, newParentId: Long?): Boolean {
        if (newParentId == null) return false
        if (newParentId == entryId) return true
        // If the entry is somewhere on the path to the new parent, we would create a loop.
        return pathTo(newParentId).any { it.id == entryId }
    }

    /**
     * A view without the entries that are checked off, for the picker menus.
     * An unfinished entry whose parent is finished moves up to the nearest unfinished
     * ancestor (or to the top), so it never becomes unreachable.
     */
    fun withoutCompleted(): EntryTree {
        val visible = entries.filter { it.completedAt == null }
        val newParents = visible.associate { it.id to nearestUnfinishedAncestor(it.id) }
        return EntryTree(visible, newParents)
    }

    private fun nearestUnfinishedAncestor(id: Long): Long? {
        val seen = mutableSetOf<Long>()
        var current: Long? = parentOfId[id]
        while (current != null && seen.add(current)) {
            val entry = byId[current] ?: return null
            if (entry.completedAt == null) return current
            current = parentOfId[current]
        }
        return null
    }
}