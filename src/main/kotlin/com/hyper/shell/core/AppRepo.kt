package com.hyper.shell.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import com.hyper.shell.util.IconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap?
) {
    val component: ComponentName get() = ComponentName(packageName, activityName)
}

/**
 * Reads the launchable activities through LauncherApps (the official launcher API).
 * It automatically includes work profiles on devices where they are available to
 * the current user and it is dramatically faster than querying PackageManager.
 */
class AppRepo(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val collator = Collator.getInstance()

    @Volatile
    private var cache: List<AppEntry>? = null

    suspend fun load(force: Boolean = false): List<AppEntry> = withContext(Dispatchers.IO) {
        if (!force) cache?.let { return@withContext it }
        val result = runCatching {
            val activities = launcherApps?.getActivityList(null, Process.myUserHandle()).orEmpty()
            activities.mapNotNull { info ->
                runCatching {
                    val component = info.componentName
                    val label = info.label?.toString()?.takeIf { it.isNotBlank() }
                        ?: info.applicationInfo.loadLabel(context.packageManager).toString()
                    AppEntry(
                        label = label,
                        packageName = component.packageName,
                        activityName = component.className,
                        icon = IconCache.appIcon(context, component.packageName)
                    )
                }.getOrNull()
            }
                .distinctBy { "${it.packageName}/${it.activityName}" }
                .sortedWith { a, b -> collator.compare(a.label, b.label) }
        }.getOrElse { emptyList() }

        cache = result
        result
    }

    fun invalidate() {
        cache = null
    }
}
