package dev.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jarvis.assistant.core.VoiceSettings
import java.util.Locale
import java.util.UUID

interface VoiceEngine {
    val ready: Boolean
    val state: String
    fun speak(text: String, settings: VoiceSettings)
    fun stop()
    fun close()
}

/** Replace this adapter when a licensed local or remote TTS model is available. */
class AndroidTtsVoiceEngine(context: Context) : VoiceEngine {
    private var engine: TextToSpeech? = null
    override var ready by mutableStateOf(false)
        private set
    override var state by mutableStateOf("Инициализация голосового движка…")
        private set

    init {
        engine = TextToSpeech(context.applicationContext) { result ->
            val tts = engine
            if (result == TextToSpeech.SUCCESS && tts != null) {
                val support = tts.setLanguage(Locale.forLanguageTag("ru-RU"))
                if (support == TextToSpeech.LANG_NOT_SUPPORTED || support == TextToSpeech.LANG_MISSING_DATA) {
                    state = "Русский голос отсутствует. Установите речевые данные в настройках Android."
                } else {
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) = Unit
                        override fun onDone(utteranceId: String) = Unit
                        @Deprecated("Provided by Android TTS")
                        override fun onError(utteranceId: String) = Unit
                    })
                    ready = true
                    state = "Голосовой движок готов"
                }
            } else state = "Не удалось запустить системный TTS"
        }
    }

    override fun speak(text: String, settings: VoiceSettings) {
        if (!ready || text.isBlank()) return
        engine?.apply {
            setSpeechRate(settings.speechRate)
            setPitch(settings.pitch)
            speak(text.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    override fun stop() { engine?.stop() }
    override fun close() { engine?.stop(); engine?.shutdown(); engine = null; ready = false }
}
