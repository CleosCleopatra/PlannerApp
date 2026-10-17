package com.todaysday.planner.domain.model

/** A place where an entry can be done, e.g. "Home" or "Library". No place = Anywhere. */
data class Place(
    val id: Long = 0,
    val name: String
)