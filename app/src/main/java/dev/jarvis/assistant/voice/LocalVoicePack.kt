package dev.jarvis.assistant.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * JARVIS-owned local PCM player. A future neural backend will generate samples into this
 * interface; the Android system TTS is a fallback rather than an architectural dependency.
 * Output is 16-bit signed mono PCM. No network, no cloud SDK, no remote voice data.
 */
interface LocalPcmSynthesisBackend {
    val loaded: Boolean
    fun synthesize(text: String): PcmSpeech?
}

data class PcmSpeech(val sampleRate: Int, val samples: ShortArray) {
    init {
        require(sampleRate in 8000..48000)
        require(samples.isNotEmpty())
    }
}

class LocalPcmPlayer {
    private val worker = Executors.newSingleThreadExecutor()
    private val generation = AtomicInteger(0)
    @Volatile private var track: AudioTrack? = null

    fun play(speech: PcmSpeech, onFinished: () -> Unit) {
        val id = generation.incrementAndGet()
        stopTrack()
        worker.execute {
            try {
                if (generation.get() != id) return@execute
                val chunk = AudioTrack.getMinBufferSize(
                    speech.sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)
                val t = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setSampleRate(speech.sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(chunk)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track = t
                t.play()
                var index = 0
                while (index < speech.samples.size && generation.get() == id) {
                    val written = t.write(speech.samples, index, minOf(2048, speech.samples.size - index))
                    if (written <= 0) break
                    index += written
                }
                if (generation.get() == id && index == speech.samples.size) {
                    // AudioTrack.write() only queues audio. Wait for playback to finish
                    // before releasing the track (previous code cut off the last samples).
                    val deadline = System.currentTimeMillis() +
                        (1000L * speech.samples.size / speech.sampleRate) + 1500L
                    while (generation.get() == id && System.currentTimeMillis() < deadline &&
                        t.playbackHeadPosition.toLong() < speech.samples.size.toLong()) {
                        Thread.sleep(20)
                    }
                    if (generation.get() == id) t.stop()
                }
                t.release()
                if (track === t) track = null
            } catch (_: Exception) {
                stopTrack()
            } finally {
                if (generation.get() == id) onFinished()
            }
        }
    }
    fun stop() { generation.incrementAndGet(); stopTrack() }
    private fun stopTrack() {
        val old = track
        track = null
        try { old?.pause(); old?.flush() } catch (_: Exception) {}
    }
    fun close() { stop(); worker.shutdownNow() }
}

/**
 * Content-addressed private model packs will be managed under application-internal storage.
 * A reference WAV is NOT a runnable text-to-speech model. Refuse false 'ready' states.
 */
class LocalVoiceModelStore(context: Context) {
    private val models = File(context.filesDir, "local_voice_models")
    val hasNeuralModel: Boolean get() = File(models, "model.onnx").isFile &&
        File(models, "model.onnx.json").isFile
    val status: String get() = if (hasNeuralModel)
        "Файлы модели найдены. Нейронный runtime ещё не подключён."
    else "Нейросетевая модель не установлена. RHVoice остаётся резервом."
}
