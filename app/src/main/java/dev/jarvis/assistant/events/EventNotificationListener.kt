package dev.jarvis.assistant.events

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.jarvis.assistant.core.UserSettings
import java.util.LinkedHashMap

/**
 * System-bound notification listener, independent from the foreground speech service.
 * It never starts a foreground service from the background; user starts it in the UI.
 */
class EventNotificationListener : NotificationListenerService() {
    private lateinit var settings: UserSettings
    private val seen = LinkedHashMap<String, Pair<String, Long>>()

    override fun onCreate() {
        super.onCreate()
        settings = UserSettings(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName) return
        val cfg = settings.loadEvents()
        if (!cfg.notificationsEnabled || sbn.packageName !in cfg.allowedApps) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (n.category == Notification.CATEGORY_SERVICE) return
        val extras = n.extras
        val title = clean(extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty())
        val body = clean(
            (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        )
        if (title.isBlank() && body.isBlank()) return
        val fingerprint = title + "|" + body
        val now = System.currentTimeMillis()
        val last = seen[sbn.key]
        if (last != null && last.first == fingerprint && now - last.second in 0L..180_000L) return
        seen[sbn.key] = fingerprint to now
        while (seen.size > 100) seen.remove(seen.keys.first())
        val app = try {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) { sbn.packageName }
        val phrase = if (cfg.speakContent) {
            val text = listOf(title, body).filter { it.isNotBlank() }.distinct().joinToString(". ")
            "Уведомление из приложения " + app + ". " + text
        } else "Новое уведомление из приложения " + app
        JarvisBackgroundService.enqueueNotification(phrase)
    }

    private fun clean(text: String): String =
        text.replace(Regex("[\\p{Cntrl}\\s]+"), " ").trim().take(450)
}
