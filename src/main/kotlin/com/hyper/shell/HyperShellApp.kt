package com.hyper.shell

import android.app.Application
import android.appwidget.AppWidgetHost
import com.hyper.shell.core.AppRepo
import com.hyper.shell.core.Prefs

class HyperShellApp : Application() {

    lateinit var prefs: Prefs
        private set
    lateinit var appRepo: AppRepo
        private set
    lateinit var widgetHost: AppWidgetHost
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        appRepo = AppRepo(this)
        widgetHost = AppWidgetHost(this, APP_WIDGET_HOST_ID)
    }

    companion object {
        // Stable per installation. Must never change while widgets are bound.
        private const val APP_WIDGET_HOST_ID = 7331
        const val SETTINGS_SECTION = "settings_section"
    }
}
