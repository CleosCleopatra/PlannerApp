package com.todaysday.planner

import android.app.Application

/**
 * The Application class runs once when the app process starts, before any screen.
 * We use it to create the AppContainer, which will hold the database and other
 * shared objects so every screen can reach them.
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
    }
}