package com.todaysday.planner.domain.model

/** Which of the four kinds an entry is, as an EntryType value. */
val Entry.entryType: EntryType
    get() = when (this) {
        is Goal -> EntryType.GOAL
        is Task -> EntryType.TASK
        is Event -> EntryType.EVENT
        is Repeating -> EntryType.REPEATING
    }