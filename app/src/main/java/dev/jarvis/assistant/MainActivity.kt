package dev.jarvis.assistant

import android.os.Bundle
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jarvis.assistant.core.UserSettings
import dev.jarvis.assistant.updates.GitHubUpdater
import dev.jarvis.assistant.updates.RemoteRelease
import dev.jarvis.assistant.voice.AndroidTtsVoiceEngine
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var tts: AndroidTtsVoiceEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = AndroidTtsVoiceEngine(applicationContext)
        val settings = UserSettings(this)
        val updater = GitHubUpdater(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    JarvisScreen(tts, settings, updater)
                }
            }
        }
    }
    override fun onDestroy() { tts.close(); super.onDestroy() }
}

@Composable
private fun JarvisScreen(tts: AndroidTtsVoiceEngine, storage: UserSettings, updater: GitHubUpdater) {
    var phrase by remember { mutableStateOf("Сэр, ваш персональный ассистент готов к работе.") }
    var settings by remember { mutableStateOf(storage.load()) }
    var message by remember { mutableStateOf("Готово к первому запуску") }
    var busy by remember { mutableStateOf(false) }
    var remoteRelease by remember { mutableStateOf<RemoteRelease?>(null) }
    var installedFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("J.A.R.V.I.S.", style = MaterialTheme.typography.headlineLarge)
        Text("VOICE ASSISTANT · v" + BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.primary)
        Text("Голосовой модуль", style = MaterialTheme.typography.titleLarge)
        Text(tts.state, style = MaterialTheme.typography.bodyMedium)
        if (!tts.ready) {
            Text(
                "Для произнесения текста на телефоне должен быть включён TTS с русским голосом.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { tts.retry() }) {
                    Text("Повторить проверку")
                }
            }
            OutlinedButton(onClick = {
                try {
                    context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: SecurityException) {
                    message = "Android не разрешил открыть настройки голосового движка."
                }
            }) {
                Text("Настроить голос Android")
            }
        }
        OutlinedTextField(
            value = phrase, onValueChange = { phrase = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Текст для синтеза") }, minLines = 3, maxLines = 8,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { tts.speak(phrase, settings) }, enabled = tts.ready && phrase.isNotBlank()) {
                Text("Произнести")
            }
            OutlinedButton(onClick = { tts.stop() }) { Text("Остановить") }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Профиль JARVIS · системный TTS", style = MaterialTheme.typography.titleMedium)
                Text("Скорость: " + "%.2f".format(settings.speechRate))
                Slider(
                    value = settings.speechRate, onValueChange = {
                        settings = settings.copy(speechRate = it); storage.save(settings)
                    }, valueRange = 0.6f..1.4f,
                )
                Text("Высота голоса: " + "%.2f".format(settings.pitch))
                Slider(
                    value = settings.pitch, onValueChange = {
                        settings = settings.copy(pitch = it); storage.save(settings)
                    }, valueRange = 0.6f..1.3f,
                )
                Text("Настройки сохраняются на устройстве. Индивидуальная модель голоса будет добавлена отдельным движком.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider()
        Text("Обновления", style = MaterialTheme.typography.titleLarge)
        Text(message)
        Button(enabled = !busy, onClick = {
            busy = true
            remoteRelease = null
            installedFile = null
            message = "Проверяем GitHub Releases…"
            scope.launch {
                try {
                    val result = updater.check(BuildConfig.VERSION_CODE.toLong())
                    remoteRelease = result
                    message = if (result == null) "Новых опубликованных версий пока нет" else "Доступна версия ${result.version}"
                } catch (ex: Exception) { message = "Ошибка проверки: ${ex.message}" }
                finally { busy = false }
            }
        }) { Text("Проверить обновления") }
        if (remoteRelease != null) {
            Button(enabled = !busy, onClick = {
                val release = remoteRelease ?: return@Button
                busy = true
                message = "Скачиваем и проверяем APK…"
                scope.launch {
                    try {
                        val downloaded = updater.downloadVerified(release)
                        installedFile = downloaded
                        message = "APK проверен. Подтвердите установку в Android."
                        if (!updater.requestInstall(downloaded)) {
                            message = "Разрешите установку для JARVIS и нажмите «Установить APK»"
                        }
                    } catch (ex: Exception) { message = "Ошибка установки: ${ex.message}" }
                    finally { busy = false }
                }
            }) { Text("Скачать обновление") }
        }
        if (installedFile != null) {
            OutlinedButton(onClick = {
                try { updater.requestInstall(installedFile ?: return@OutlinedButton) }
                catch (ex: Exception) { message = "Не удалось открыть установщик: ${ex.message}" }
            }) { Text("Установить APK") }
        }
        Spacer(Modifier.height(4.dp))
        Text("Уведомления и интеграции будут добавлены в следующих версиях.", style = MaterialTheme.typography.bodySmall)
    }
}
