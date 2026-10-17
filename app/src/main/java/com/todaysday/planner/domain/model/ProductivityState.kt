package com.todaysday.planner.domain.model

/**
 * How productive you are right now. These are the Off / Half / Full buttons on the Start tab.
 * - OFF:  nothing will be done until you change it
 * - HALF: lower productivity (how much lower is set by the "Half state" time rule)
 * - FULL: normal
 */
enum class ProductivityState { OFF, HALF, FULL }