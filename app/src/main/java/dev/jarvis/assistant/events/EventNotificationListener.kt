package dev.jarvis.assistant.events

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.jarvis.assistant.core.EventPolicy
import dev.jarvis.assistant.core.UserSettings
import dev.jarvis.assistant.voice.AndroidTtsVoiceEngine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.LinkedHashMap

/** Events use the system-bound listener; hourly ticks may be delayed by Android/Huawei Doze. */
class EventNotificationListener : NotificationListenerService() {
    private lateinit var settings: UserSettings
    private lateinit var voice: AndroidTtsVoiceEngine
    private val pending = ArrayDeque<String>()
    private var speaking = false
    private val seen = LinkedHashMap<String, Pair<String, Long>>()
    private val markers by lazy { getSharedPreferences("event_markers", Context.MODE_PRIVATE) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> batteryChanged(intent)
                Intent.ACTION_TIME_TICK -> minuteTick()
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        settings = UserSettings(applicationContext)
        voice = AndroidTtsVoiceEngine(applicationContext,
            onReady = { flush() },
            onUtteranceFinished = { speaking = false; flush() }
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }
        registerReceiver(receiver, filter)
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
        if (last != null && last.first == fingerprint && now - last.second in 0L..180000L) return
        seen[sbn.key] = fingerprint to now
        while (seen.size > 100) seen.remove(seen.keys.first())
        val app = try {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) { sbn.packageName }
        val phrase = if (cfg.speakContent) {
            val content = listOf(title, body).filter { it.isNotBlank() }.distinct().joinToString(". ")
            "Уведомление из приложения " + app + ". " + content
        } else "Новое уведомление из приложения " + app
        enqueue(phrase)
    }

    private fun batteryChanged(event: Intent) {
        val level = event.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = event.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return
        val percentage = (level.toDouble() * 100.0 / scale).toInt().coerceIn(0,100)
        val charging = event.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val previous = markers.getInt("battery", -1)
        val previousAt = markers.getLong("battery_at", 0)
        val now = System.currentTimeMillis()
        markers.edit().putInt("battery", percentage).putLong("battery_at", now).apply()
        if (now - previousAt !in 0L..7200000L) return
        if (settings.loadEvents().batteryEnabled &&
            EventPolicy.crossedBatteryDecile(previous, percentage, charging)) {
            enqueue("Заряд батареи " + percentage + " процентов")
        }
    }

    private fun minuteTick() {
        if (!settings.loadEvents().hourlyEnabled) return
        val now = LocalDateTime.now()
        val hourKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"))
        val last = markers.getString("hour", null)
        if (EventPolicy.hourlyDue(now.minute, hourKey, last)) {
            markers.edit().putString("hour", hourKey).apply()
            enqueue(EventPolicy.hourPhrase(now.hour))
        }
    }

    private fun enqueue(text: String) {
        if (pending.size >= 10) pending.removeFirst()
        pending.addLast(text)
        flush()
    }
    private fun flush() {
        if (speaking || !voice.ready || pending.isEmpty()) return
        speaking = true
        voice.speak(pending.removeFirst(), settings.load())
    }
    private fun clean(text: String): String =
        text.replace(Regex("[\\p{Cntrl}\\s]+"), " ").trim().take(450)
    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
        pending.clear()
        voice.close()
        super.onDestroy()
    }
}
