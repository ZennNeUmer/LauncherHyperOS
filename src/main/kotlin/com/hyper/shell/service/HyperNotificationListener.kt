package com.hyper.shell.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hyper.shell.core.NotificationCenter

class HyperNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationCenter.attach(this)
    }

    override fun onListenerDisconnected() {
        NotificationCenter.detach(this)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return
        NotificationCenter.upsert(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        NotificationCenter.remove(sbn.key)
    }
}
