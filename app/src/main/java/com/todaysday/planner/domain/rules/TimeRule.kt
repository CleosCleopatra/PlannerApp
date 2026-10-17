package com.todaysday.planner.domain.rules

/**
 * A time rule is a setting that tells the planner something about your days, for example
 * "sleep length is 8 h 30 min" or "on Tuesdays I am 50% as productive".
 *
 * There are several kinds (see RuleTypes.kt). Every kind has these three things:
 * - id:         0 means "not saved in the database yet"
 * - enabled:    a disabled rule is ignored, as if it did not exist
 * - customName: a name typed by the user. null means "use the name made from the settings"
 *               (see RuleNamer).
 *
 * To add a new kind of rule later: add one data class in RuleTypes.kt, one line in the
 * `when` of RuleNamer and `withBase`, and use it where it is needed in RuleEngine.
 */
sealed interface TimeRule {
    val id: Long
    val enabled: Boolean
    val customName: String?
}

/** The meals the planner knows about. */
enum class MealKind { LUNCH, DINNER }