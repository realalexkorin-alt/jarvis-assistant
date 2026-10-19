package dev.jarvis.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.util.concurrent.Executors

/** Offline Russian VITS neural synthesis using packaged sherpa-onnx JNI/model. */
class NeuralOfflineVoice(private val context: Context) {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val player = LocalPcmPlayer()
    private var engine: OfflineTts? = null
    @Volatile private var closed = false
    @Volatile var status = "Модель ожидает запуска"
        private set

    fun synthesizeAndPlay(text: String, speed: Float = 1f, finished: (Boolean) -> Unit) {
        if (closed) { finished(false); return }
        worker.execute {
            try {
                if (closed) return@execute
                val tts = engine ?: load().also { engine = it }
                status = "Синтезирую офлайн…"
                val audio = tts.generate(text.take(350), sid = 0, speed = speed.coerceIn(0.7f, 1.3f))
                require(audio.samples.isNotEmpty()) { "Пустой результат" }
                val pcm = ShortArray(audio.samples.size) { i ->
                    (audio.samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }
                status = "Нейромодель готова"
                player.play(PcmSpeech(audio.sampleRate, pcm)) { main.post { finished(true) } }
            } catch (e: Throwable) {
                status = "Ошибка: " + (e.message ?: e.javaClass.simpleName).take(100)
                main.post { finished(false) }
            }
        }
    }

    private fun load(): OfflineTts {
        status = "Загружаю нейромодель…"
        val directory = "vits-piper-ru_RU-dmitri-medium"
        return OfflineTts(
            assetManager = context.assets,
            config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = "$directory/ru_RU-dmitri-medium.onnx",
                        tokens = "$directory/tokens.txt",
                        dataDir = "$directory/espeak-ng-data",
                    ),
                    numThreads = 2,
                    provider = "cpu",
                )
            )
        )
    }

    fun stop() = player.stop()

    fun close() {
        closed = true
        player.close()
        worker.execute { try { engine?.release() } catch (_: Throwable) {} }
        worker.shutdown()
    }
}
