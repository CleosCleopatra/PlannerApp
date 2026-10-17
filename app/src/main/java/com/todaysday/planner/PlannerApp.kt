package com.todaysday.planner

import android.app.Application
import kotlinx.coroutines.launch

/**
 * The Application class runs once when the app process starts, before any screen.
 * We use it to create the AppContainer, which holds the database and other shared objects,
 * and to create the default time rules the very first time the app starts.
 *
 * It is registered in AndroidManifest.xml with android:name=".PlannerApp".
 */
class PlannerApp : Application() {

    // lateinit means "I promise to set this before anyone reads it" (we do, in onCreate).
    // private set means only this class can change it; everyone else can only read it.
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // In the background: create the default rules if this is the first start.
        // Nothing happens on later starts.
        container.applicationScope.launch {
            container.timeRuleRepository.seedDefaultsIfNeeded()
        }
    }
}