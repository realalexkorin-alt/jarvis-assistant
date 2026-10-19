package dev.jarvis.assistant.events

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jarvis.assistant.MainActivity
import dev.jarvis.assistant.core.EventPolicy
import dev.jarvis.assistant.core.UserSettings
import dev.jarvis.assistant.voice.AndroidTtsVoiceEngine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque

/**
 * User-initiated foreground companion to NotificationListenerService.
 * Android may still terminate it (especially on Huawei); there is no silent permission bypass.
 * No voice processing or notification contents leave the device.
 */
class JarvisBackgroundService : Service() {
    companion object {
        private const val CHANNEL_ID = "jarvis_background_voice"
        private const val NOTIFICATION_ID = 2001
        private val mainHandler = Handler(Looper.getMainLooper())
        private var active: JarvisBackgroundService? = null

        var running by mutableStateOf(false)
            private set

        fun enqueueNotification(phrase: String) {
            active?.enqueue(phrase)
        }

        /** On-demand test: user locks the phone and hears the phrase 20 s later. */
        fun scheduleBackgroundTest(): Boolean {
            val service = active ?: return false
            mainHandler.postDelayed({
                if (active === service) service.enqueue("Сэр, фоновая озвучка работает.")
            }, 20_000L)
            return true
        }
    }

    private lateinit var storage: UserSettings
    private lateinit var voice: AndroidTtsVoiceEngine
    private val queue = ArrayDeque<String>()
    private var speaking = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val markers by lazy { getSharedPreferences("event_markers", Context.MODE_PRIVATE) }
    private val systemEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> batteryChanged(intent)
                Intent.ACTION_TIME_TICK -> minuteTick()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notice = foregroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notice, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notice)
        }
        storage = UserSettings(applicationContext)
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:tts_utterance").apply {
            setReferenceCounted(false)
        }
        voice = AndroidTtsVoiceEngine(applicationContext,
            onReady = { flushQueue() },
            onUtteranceFinished = {
                releaseWakeLock()
                speaking = false
                flushQueue()
            }
        )
        active = this
        running = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }
        registerReceiver(systemEvents, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "Фоновая озвучка JARVIS", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Показывает, когда включена фоновая голосовая служба" }
        )
    }

    private fun foregroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("JARVIS работает в фоне")
            .setContentText("Озвучивание выбранных уведомлений, батареи и времени")
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun batteryChanged(event: Intent) {
        val level = event.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = event.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return
        val percentage = (level.toDouble() * 100 / scale).toInt().coerceIn(0, 100)
        val charging = event.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val before = markers.getInt("battery", -1)
        val beforeAt = markers.getLong("battery_at", 0L)
        val now = System.currentTimeMillis()
        markers.edit().putInt("battery", percentage).putLong("battery_at", now).apply()
        if (now - beforeAt !in 0L..7_200_000L) return
        if (storage.loadEvents().batteryEnabled && EventPolicy.crossedBatteryDecile(before, percentage, charging)) {
            enqueue("Заряд батареи " + percentage + " процентов")
        }
    }

    private fun minuteTick() {
        if (!storage.loadEvents().hourlyEnabled) return
        val time = LocalDateTime.now()
        val key = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"))
        if (EventPolicy.hourlyDue(time.minute, key, markers.getString("hour", null))) {
            markers.edit().putString("hour", key).apply()
            enqueue(EventPolicy.hourPhrase(time.hour))
        }
    }

    private fun enqueue(phrase: String) {
        if (phrase.isBlank()) return
        if (queue.size >= 12) queue.removeFirst()
        queue.addLast(phrase.take(700))
        flushQueue()
    }

    private fun flushQueue() {
        if (speaking || queue.isEmpty() || !voice.ready) return
        speaking = true
        try {
            wakeLock?.acquire(45_000L)
            voice.speak(queue.removeFirst(), storage.load())
        } catch (_: Exception) {
            releaseWakeLock()
            speaking = false
        }
    }

    private fun releaseWakeLock() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        if (active === this) active = null
        running = false
        try { unregisterReceiver(systemEvents) } catch (_: IllegalArgumentException) {}
        queue.clear()
        voice.close()
        releaseWakeLock()
        super.onDestroy()
    }
}
