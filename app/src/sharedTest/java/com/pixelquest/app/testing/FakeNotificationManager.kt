package com.pixelquest.app.testing

import android.app.Notification

/**
 * Deterministic test fake representing notification interactions.
 * Records all notifications shown, updated, and cancelled for assertions.
 */
class FakeNotificationManager {

    data class ShownNotification(
        val id: Int,
        val tag: String? = null,
        val notification: Notification? = null,
        val title: String? = null,
        val text: String? = null,
        val channelId: String? = null,
    )

    var areNotificationsEnabled: Boolean = true
    val shownNotifications = mutableListOf<ShownNotification>()
    val cancelledNotificationIds = mutableListOf<Int>()
    val cancelledNotificationTags = mutableListOf<String?>()

    fun notify(id: Int, notification: Notification) {
        notify(null, id, notification)
    }

    fun notify(tag: String?, id: Int, notification: Notification) {
        if (!areNotificationsEnabled) return
        shownNotifications.removeAll { it.id == id && it.tag == tag }
        shownNotifications.add(ShownNotification(id = id, tag = tag, notification = notification))
    }

    fun notify(id: Int, title: String?, text: String?, channelId: String? = null) {
        if (!areNotificationsEnabled) return
        shownNotifications.removeAll { it.id == id && it.tag == null }
        shownNotifications.add(ShownNotification(id = id, title = title, text = text, channelId = channelId))
    }

    fun cancel(id: Int) {
        cancel(null, id)
    }

    fun cancel(tag: String?, id: Int) {
        cancelledNotificationIds.add(id)
        tag?.let { cancelledNotificationTags.add(it) }
        shownNotifications.removeAll { it.id == id && it.tag == tag }
    }

    fun cancelAll() {
        cancelledNotificationIds.addAll(shownNotifications.map { it.id })
        shownNotifications.clear()
    }

    fun isShown(id: Int): Boolean = shownNotifications.any { it.id == id }

    fun wasCancelled(id: Int): Boolean = cancelledNotificationIds.contains(id)

    fun clear() {
        shownNotifications.clear()
        cancelledNotificationIds.clear()
        cancelledNotificationTags.clear()
    }
}
