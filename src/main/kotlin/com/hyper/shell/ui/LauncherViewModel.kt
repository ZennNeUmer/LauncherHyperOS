package com.hyper.shell.ui

import android.app.Application
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyper.shell.HyperShellApp
import com.hyper.shell.core.AppEntry
import com.hyper.shell.core.LauncherSettings
import com.hyper.shell.core.NotificationCenter
import com.hyper.shell.core.WidgetConfig
import com.hyper.shell.core.WidgetKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val hyper = application as HyperShellApp
    val prefs = hyper.prefs

    val settings: StateFlow<LauncherSettings> = prefs.settings
    val widgets: StateFlow<List<WidgetConfig>> = prefs.widgets
    val notifications = NotificationCenter.items

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val launcherApps: LauncherApps? = application.getSystemService(LauncherApps::class.java)
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(application)

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle?) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle?) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle?) = reload()
        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) = reload()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) = reload()
    }

    init {
        reload()
        runCatching { launcherApps?.registerCallback(packageCallback) }
    }

    fun reload() {
        viewModelScope.launch {
            _loading.value = true
            hyper.appRepo.invalidate()
            _apps.value = hyper.appRepo.load(force = true)
            _loading.value = false
        }
    }

    fun visibleApps(): List<AppEntry> {
        val hidden = settings.value.hiddenApps
        return _apps.value.filterNot { it.packageName in hidden }
    }

    fun launch(app: AppEntry) {
        val started = runCatching {
            launcherApps?.startMainActivity(app.component, Process.myUserHandle(), null, null)
        }.isSuccess
        if (!started) {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { getApplication<Application>().startActivity(intent) }
        }
    }

    fun openAppInfo(packageName: String) {
        startActivitySafe(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun uninstall(packageName: String) {
        startActivitySafe(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun hideApp(packageName: String) {
        prefs.update {
            val next = it.hiddenApps.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            it.copy(hiddenApps = next)
        }
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        prefs.update {
            val next = it.hiddenApps.toMutableSet()
            if (hidden) next.add(packageName) else next.remove(packageName)
            it.copy(hiddenApps = next)
        }
    }

    fun setDock(packageName: String, pinned: Boolean) {
        prefs.update {
            val next = it.dockApps.toMutableList()
            if (pinned && packageName !in next && next.size < 5) next.add(packageName)
            if (!pinned) next.remove(packageName)
            it.copy(dockApps = next)
        }
    }

    fun moveDock(packageName: String, direction: Int) {
        prefs.update {
            val next = it.dockApps.toMutableList()
            val index = next.indexOf(packageName)
            if (index >= 0) {
                val target = (index + direction).coerceIn(0, next.lastIndex)
                if (target != index) {
                    next.removeAt(index)
                    next.add(target, packageName)
                }
            }
            it.copy(dockApps = next)
        }
    }

    fun hasNotificationAccess(): Boolean {
        val context = getApplication<Application>()
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun openNotificationAccess() {
        startActivitySafe(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openNotificationPolicyAccess() {
        startActivitySafe(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openDefaultLauncherSettings() {
        startActivitySafe(
            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openWriteSettings() {
        startActivitySafe(
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${getApplication<Application>().packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openSettings(section: String = "appearance") {
        startActivitySafe(
            Intent(getApplication(), SettingsActivity::class.java)
                .putExtra(HyperShellApp.SETTINGS_SECTION, section)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openWallpaperPicker() {
        startActivitySafe(Intent(Intent.ACTION_SET_WALLPAPER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------- widgets

    fun allocateWidgetId(): Int = hyper.widgetHost.allocateAppWidgetId()

    fun deleteWidgetId(id: Int) {
        if (id >= 0) runCatching { hyper.widgetHost.deleteAppWidgetId(id) }
    }

    fun nextWidgetY(): Float = (widgets.value.maxOfOrNull { it.y + it.h } ?: 20f) + 14f

    fun addPickedWidget(appWidgetId: Int, widthDp: Float, heightDp: Float): Boolean {
        val info = runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()
            ?: return false
        val width = min(
            max(info.minWidth + 32f, 180f),
            (widthDp - 32f).coerceAtLeast(180f)
        )
        val height = max(info.minHeight + 32f, 120f)
        val y = nextWidgetY().coerceAtMost(max(20f, heightDp - height - 120f))
        prefs.addWidget(
            WidgetConfig(
                id = System.currentTimeMillis(),
                kind = WidgetKind.APPWIDGET,
                appWidgetId = appWidgetId,
                x = 16f,
                y = y,
                w = width,
                h = height
            )
        )
        prefs.markWidgetsSeeded()
        return true
    }

    fun addBuiltInWidget(kind: WidgetKind, widthDp: Float, heightDp: Float) {
        if (kind == WidgetKind.APPWIDGET) return
        val width = (widthDp - 32f).coerceAtMost(560f).coerceAtLeast(200f)
        val (w, h) = when (kind) {
            WidgetKind.CLOCK -> width to 172f
            WidgetKind.SEARCH -> width to 64f
            WidgetKind.BATTERY -> min(180f, width) to 124f
            WidgetKind.NOTIFICATIONS -> min(180f, width) to 124f
            WidgetKind.APPWIDGET -> width to 160f
        }
        val y = nextWidgetY().coerceAtMost(max(24f, heightDp - h - 130f))
        prefs.addWidget(
            WidgetConfig(
                id = System.currentTimeMillis(),
                kind = kind,
                x = 16f,
                y = y,
                w = w,
                h = h
            )
        )
        prefs.markWidgetsSeeded()
    }

    fun updateWidget(widget: WidgetConfig) = prefs.updateWidget(widget)

    fun removeWidget(widget: WidgetConfig) {
        if (widget.kind == WidgetKind.APPWIDGET) deleteWidgetId(widget.appWidgetId)
        prefs.removeWidget(widget.id)
    }

    fun resetWidgets(widthDp: Float, heightDp: Float) {
        widgets.value.filter { it.kind == WidgetKind.APPWIDGET }.forEach {
            deleteWidgetId(it.appWidgetId)
        }
        prefs.setWidgets(emptyList())
        prefs.update { it.copy(widgetsSeeded = false) }
        ensureDefaultWidgets(widthDp, heightDp)
    }

    fun resetEverything() {
        widgets.value.filter { it.kind == WidgetKind.APPWIDGET }.forEach {
            deleteWidgetId(it.appWidgetId)
        }
        prefs.setWidgets(emptyList())
        prefs.update { LauncherSettings() }
        toast("Все настройки сброшены")
    }

    fun ensureDefaultWidgets(widthDp: Float, heightDp: Float) {
        if (settings.value.widgetsSeeded) return
        val w = (widthDp - 32f).coerceAtMost(560f).coerceAtLeast(260f)
        val gap = 12f
        val smallW = (w - gap) / 2f
        val y1 = 12f
        val y2 = y1 + 172f + gap
        val y3 = y2 + 64f + gap
        val list = listOf(
            WidgetConfig(10L, WidgetKind.CLOCK, x = 16f, y = y1, w = w, h = 172f),
            WidgetConfig(11L, WidgetKind.SEARCH, x = 16f, y = y2, w = w, h = 64f),
            WidgetConfig(12L, WidgetKind.BATTERY, x = 16f, y = y3, w = smallW, h = 124f),
            WidgetConfig(
                13L,
                WidgetKind.NOTIFICATIONS,
                x = 16f + smallW + gap,
                y = y3,
                w = smallW,
                h = 124f
            )
        )
        prefs.setWidgets(list)
        prefs.markWidgetsSeeded()
    }

    fun applySystemWallpaper(uri: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                        WallpaperManager.getInstance(context).setStream(input)
                    }
                }
            }
            toast(
                if (result.isSuccess) "Обои установлены в системе"
                else "Не удалось установить обои"
            )
        }
    }

    private fun startActivitySafe(intent: Intent) {
        runCatching { getApplication<Application>().startActivity(intent) }
            .onFailure { toast("Не удалось открыть системный экран") }
    }

    override fun onCleared() {
        runCatching { launcherApps?.unregisterCallback(packageCallback) }
        super.onCleared()
    }
}
