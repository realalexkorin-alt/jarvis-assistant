package dev.jarvis.assistant.core
import android.content.Context

data class VoiceSettings(val speechRate: Float = 0.88f, val pitch: Float = 0.85f)

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
}
