package com.hyper.shell.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import com.hyper.shell.service.HyperNotificationListener
import com.hyper.shell.util.IconCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotifItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val time: Long,
    val icon: ImageBitmap?,
    val pendingIntent: PendingIntent?,
    val clearable: Boolean,
    val ongoing: Boolean
)

/**
 * Shared notification store. The listener service writes into it, the Compose
 * shade reads from it. No database, no binder plumbing on the UI side.
 */
object NotificationCenter {

    private val _items = MutableStateFlow<List<NotifItem>>(emptyList())
    val items: StateFlow<List<NotifItem>> = _items.asStateFlow()

    private val map = LinkedHashMap<String, NotifItem>()
    private val appMeta = HashMap<String, Pair<String, ImageBitmap?>>()

    @Volatile
    private var service: HyperNotificationListener? = null

    @Volatile
    private var appContext: Context? = null

    fun attach(listener: HyperNotificationListener) {
        service = listener
        appContext = listener.applicationContext
        runCatching {
            listener.activeNotifications?.forEach { upsert(it) }
        }
        publish()
    }

    fun detach(listener: HyperNotificationListener) {
        if (service === listener) {
            service = null
            appContext = null
            map.clear()
            publish()
        }
    }

    fun upsert(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val context = appContext ?: return
        val extras = notification.extras ?: return

        val title = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        ).firstOrNull { !it.isNullOrBlank() }?.toString().orEmpty()

        val text = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
        ).firstOrNull { !it.isNullOrBlank() }?.toString().orEmpty()

        val meta = appMeta.getOrPut(sbn.packageName) {
            val label = runCatching {
                val appInfo = context.packageManager.getApplicationInfo(sbn.packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            }.getOrElse { sbn.packageName }
            label to IconCache.appIcon(context, sbn.packageName)
        }

        map[sbn.key] = NotifItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = meta.first,
            title = title.ifBlank { meta.first },
            text = text,
            time = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis(),
            icon = meta.second,
            pendingIntent = notification.contentIntent,
            clearable = runCatching { sbn.isClearable }.getOrDefault(true),
            ongoing = runCatching { sbn.isOngoing }.getOrDefault(false)
        )
        publish()
    }

    fun remove(key: String) {
        if (map.remove(key) != null) publish()
    }

    fun dismiss(item: NotifItem) {
        runCatching { service?.cancelNotification(item.key) }
        map.remove(item.key)
        publish()
    }

    fun open(item: NotifItem, context: Context) {
        val sent = runCatching {
            item.pendingIntent?.send()
            true
        }.getOrDefault(false)
        if (!sent) {
            val intent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("package:${item.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }

    fun clearAll() {
        runCatching { service?.cancelAllNotifications() }
        map.clear()
        publish()
    }

    private fun publish() {
        _items.value = map.values.sortedByDescending { it.time }
    }
}
