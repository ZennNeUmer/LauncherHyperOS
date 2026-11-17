package com.hyper.shell.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class WidgetKind {
    CLOCK,
    BATTERY,
    SEARCH,
    NOTIFICATIONS,
    APPWIDGET
}

data class WidgetConfig(
    val id: Long,
    val kind: WidgetKind,
    val appWidgetId: Int = -1,
    val x: Float = 16f,
    val y: Float = 16f,
    val w: Float = 320f,
    val h: Float = 160f
)

data class LauncherSettings(
    val homeWallpaper: String? = null,
    val shadeWallpaper: String? = null,
    /** Shade dimming strength in percent (35..85). */
    val shadeBlur: Int = 60,
    val animations: Boolean = true,
    val showLabels: Boolean = false,
    val accent: Int = 0xFF3482FF.toInt(),
    val edgeDrawer: Boolean = true,
    val swipeLeftShade: Boolean = true,
    val swipeUpDrawer: Boolean = true,
    val swipeDownShade: Boolean = true,
    val showDock: Boolean = true,
    val dockApps: List<String> = emptyList(),
    val hiddenApps: Set<String> = emptySet(),
    val widgetsSeeded: Boolean = false,
    val editRequest: Long = 0L
)

/**
 * Small, dependency-free persistence layer. Everything is a StateFlow so both the
 * launcher and the settings app react instantly to changes.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("hyper_shell", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _widgets = MutableStateFlow(readWidgets())
    val widgets: StateFlow<List<WidgetConfig>> = _widgets.asStateFlow()

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        sp.edit().putString(KEY_SETTINGS, next.toJson().toString()).apply()
    }

    fun requestWidgetEdit() {
        update { it.copy(editRequest = System.currentTimeMillis()) }
    }

    fun setWidgets(list: List<WidgetConfig>) {
        _widgets.value = list
        persistWidgets(list)
    }

    fun addWidget(widget: WidgetConfig) {
        setWidgets(_widgets.value + widget)
    }

    fun updateWidget(widget: WidgetConfig) {
        setWidgets(_widgets.value.map { if (it.id == widget.id) widget else it })
    }

    fun removeWidget(id: Long) {
        setWidgets(_widgets.value.filterNot { it.id == id })
    }

    fun markWidgetsSeeded() {
        update { it.copy(widgetsSeeded = true) }
    }

    private fun persistWidgets(list: List<WidgetConfig>) {
        val arr = JSONArray()
        list.forEach { w ->
            arr.put(
                JSONObject()
                    .put("id", w.id)
                    .put("kind", w.kind.name)
                    .put("appWidgetId", w.appWidgetId)
                    .put("x", w.x.toDouble())
                    .put("y", w.y.toDouble())
                    .put("w", w.w.toDouble())
                    .put("h", w.h.toDouble())
            )
        }
        sp.edit().putString(KEY_WIDGETS, arr.toString()).apply()
    }

    private fun readSettings(): LauncherSettings {
        val raw = sp.getString(KEY_SETTINGS, null) ?: return LauncherSettings()
        return runCatching { parseSettings(JSONObject(raw)) }.getOrElse { LauncherSettings() }
    }

    private fun readWidgets(): List<WidgetConfig> {
        val raw = sp.getString(KEY_WIDGETS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val kind = runCatching { WidgetKind.valueOf(o.optString("kind", "CLOCK")) }
                        .getOrElse { WidgetKind.CLOCK }
                    add(
                        WidgetConfig(
                            id = o.optLong("id", System.nanoTime()),
                            kind = kind,
                            appWidgetId = o.optInt("appWidgetId", -1),
                            x = o.optDouble("x", 16.0).toFloat(),
                            y = o.optDouble("y", 16.0).toFloat(),
                            w = o.optDouble("w", 320.0).toFloat(),
                            h = o.optDouble("h", 160.0).toFloat()
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun parseSettings(o: JSONObject): LauncherSettings {
        val dock = mutableListOf<String>()
        o.optJSONArray("dockApps")?.let { arr ->
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let(dock::add)
        }
        val hidden = mutableSetOf<String>()
        o.optJSONArray("hiddenApps")?.let { arr ->
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let(hidden::add)
        }
        return LauncherSettings(
            homeWallpaper = o.nullableString("homeWallpaper"),
            shadeWallpaper = o.nullableString("shadeWallpaper"),
            shadeBlur = o.optInt("shadeBlur", 60),
            animations = o.optBoolean("animations", true),
            showLabels = o.optBoolean("showLabels", false),
            accent = o.optInt("accent", 0xFF3482FF.toInt()),
            edgeDrawer = o.optBoolean("edgeDrawer", true),
            swipeLeftShade = o.optBoolean("swipeLeftShade", true),
            swipeUpDrawer = o.optBoolean("swipeUpDrawer", true),
            swipeDownShade = o.optBoolean("swipeDownShade", true),
            showDock = o.optBoolean("showDock", true),
            dockApps = dock,
            hiddenApps = hidden,
            widgetsSeeded = o.optBoolean("widgetsSeeded", false),
            editRequest = o.optLong("editRequest", 0L)
        )
    }

    private fun LauncherSettings.toJson(): JSONObject = JSONObject().apply {
        put("homeWallpaper", homeWallpaper ?: JSONObject.NULL)
        put("shadeWallpaper", shadeWallpaper ?: JSONObject.NULL)
        put("shadeBlur", shadeBlur)
        put("animations", animations)
        put("showLabels", showLabels)
        put("accent", accent)
        put("edgeDrawer", edgeDrawer)
        put("swipeLeftShade", swipeLeftShade)
        put("swipeUpDrawer", swipeUpDrawer)
        put("swipeDownShade", swipeDownShade)
        put("showDock", showDock)
        put("dockApps", JSONArray(dockApps))
        put("hiddenApps", JSONArray(hiddenApps.toList()))
        put("widgetsSeeded", widgetsSeeded)
        put("editRequest", editRequest)
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    companion object {
        private const val KEY_SETTINGS = "settings_v1"
        private const val KEY_WIDGETS = "widgets_v1"
    }
}
