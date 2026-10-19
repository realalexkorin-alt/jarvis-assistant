package dev.jarvis.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
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

/** Android TTS is a replaceable adapter; no prerecorded or proprietary voice samples are embedded. */
class AndroidTtsVoiceEngine(
    private val context: Context,
    private val onReady: () -> Unit = {},
    private val onUtteranceFinished: () -> Unit = {},
) : VoiceEngine {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0
    private var closed = false

    override var ready by mutableStateOf(false)
        private set
    override var state by mutableStateOf("Инициализация голосового движка…")
        private set

    init { retry() }

    /**
     * User-triggered retry after installing or changing the device's TTS engine.
     * Always defer the initialization callback: Android may invoke it during construction,
     * before the TextToSpeech instance has been assigned.
     */
    fun retry() {
        if (closed) return
        val attempt = ++generation
        ready = false
        state = "Проверяем системный голос…"
        engine?.shutdown()
        engine = null
        try {
            engine = TextToSpeech(context.applicationContext) { status ->
                mainHandler.post {
                    if (attempt == generation && !closed) onInitialized(status, attempt)
                }
            }
        } catch (_: Exception) {
            state = "Не удалось открыть TTS. Проверьте настройки синтеза речи Android."
        }
    }

    private fun onInitialized(status: Int, attempt: Int) {
        val tts = engine
        if (status != TextToSpeech.SUCCESS || tts == null) {
            state = "Системный TTS не запустился. Проверьте, установлен ли голосовой движок."
            return
        }
        val languageSupport = try {
            val ruRu = tts.setLanguage(Locale.forLanguageTag("ru-RU"))
            if (ruRu >= TextToSpeech.LANG_AVAILABLE) ruRu
            else tts.setLanguage(Locale.forLanguageTag("ru"))
        } catch (_: Exception) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
        if (languageSupport < TextToSpeech.LANG_AVAILABLE) {
            state = "Русский голос недоступен. Установите русский язык в настройках TTS."
            return
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                mainHandler.post { if (!closed && attempt == generation) state = "Произношу текст…" }
            }
            override fun onDone(utteranceId: String) {
                mainHandler.post { if (!closed && attempt == generation) { state = "Голосовой движок готов"; onUtteranceFinished() } }
            }
            @Deprecated("Android TTS API")
            override fun onError(utteranceId: String) {
                mainHandler.post { if (!closed && attempt == generation) { state = "Ошибка воспроизведения TTS"; onUtteranceFinished() } }
            }
            override fun onError(utteranceId: String, errorCode: Int) {
                mainHandler.post { if (!closed && attempt == generation) { state = "Ошибка TTS: код $errorCode"; onUtteranceFinished() } }
            }
        })
        ready = true
        state = "Голосовой движок готов"
        onReady()
    }

    override fun speak(text: String, settings: VoiceSettings) {
        val tts = engine ?: return
        if (!ready || text.isBlank()) return
        try {
            tts.setSpeechRate(settings.speechRate)
            tts.setPitch(settings.pitch)
            val outcome = tts.speak(
                text.take(TextToSpeech.getMaxSpeechInputLength()),
                TextToSpeech.QUEUE_FLUSH,
                null,
                UUID.randomUUID().toString()
            )
            if (outcome != TextToSpeech.SUCCESS) { state = "TTS отклонил запрос озвучивания"; onUtteranceFinished() }
        } catch (_: Exception) {
            state = "Не удалось воспроизвести текст через TTS"
            onUtteranceFinished()
        }
    }

    override fun stop() {
        engine?.stop()
        if (ready) state = "Озвучивание остановлено"
    }

    override fun close() {
        closed = true
        generation++
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }
}
