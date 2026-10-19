package dev.jarvis.assistant.core
import android.content.Context

data class VoiceSettings(val speechRate: Float = 0.88f, val pitch: Float = 0.85f)
data class EventSettings(
    val notificationsEnabled: Boolean = true,
    val speakContent: Boolean = false,
    val batteryEnabled: Boolean = true,
    val hourlyEnabled: Boolean = true,
    val allowedApps: Set<String> = emptySet(),
    val neuralVoiceEnabled: Boolean = false,
)

class UserSettings(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
    fun load() = VoiceSettings(
        speechRate = prefs.getFloat("speech_rate", 0.88f).coerceIn(0.6f, 1.4f),
        pitch = prefs.getFloat("pitch", 0.85f).coerceIn(0.6f, 1.3f),
    )
    fun save(settings: VoiceSettings) {
        prefs.edit().putFloat("speech_rate", settings.speechRate)
            .putFloat("pitch", settings.pitch).apply()
    }
    fun loadEvents() = EventSettings(
        notificationsEnabled = prefs.getBoolean("events_notifications", true),
        speakContent = prefs.getBoolean("events_content", false),
        batteryEnabled = prefs.getBoolean("events_battery", true),
        hourlyEnabled = prefs.getBoolean("events_hourly", true),
        allowedApps = prefs.getStringSet("events_apps", emptySet())?.toSet().orEmpty(),
        neuralVoiceEnabled = prefs.getBoolean("events_neural_voice", false),
    )
    fun saveEvents(settings: EventSettings) {
        prefs.edit()
            .putBoolean("events_notifications", settings.notificationsEnabled)
            .putBoolean("events_content", settings.speakContent)
            .putBoolean("events_battery", settings.batteryEnabled)
            .putBoolean("events_hourly", settings.hourlyEnabled)
            .putStringSet("events_apps", settings.allowedApps.toSet())
            .putBoolean("events_neural_voice", settings.neuralVoiceEnabled).apply()
    }
}
