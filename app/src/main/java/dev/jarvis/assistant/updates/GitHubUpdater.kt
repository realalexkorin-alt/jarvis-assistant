package dev.jarvis.assistant.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val REPOSITORY = "realalexkorin-alt/jarvis-assistant"
private const val APK_NAME = "jarvis-assistant.apk"

data class RemoteRelease(val version: String, val assetUrl: String, val sha256: String)

/** User-initiated update channel; only Android's installer can install downloaded APKs. */
class GitHubUpdater(private val context: Context) {
    suspend fun check(currentVersionCode: Long): RemoteRelease? = withContext(Dispatchers.IO) {
        val request = URL("https://api.github.com/repos/$REPOSITORY/releases/latest").openConnection() as HttpURLConnection
        request.connectTimeout = 15000
        request.readTimeout = 15000
        request.setRequestProperty("Accept", "application/vnd.github+json")
        request.setRequestProperty("User-Agent", "JARVIS-Assistant-Android")
        try {
            if (request.responseCode == 404) return@withContext null
            if (request.responseCode != 200) error("GitHub: HTTP ${request.responseCode}")
            val release = JSONObject(request.inputStream.bufferedReader().use { it.readText() })
            val tag = release.getString("tag_name")
            val newCode = Versioning.versionCode(tag) ?: error("Неподдерживаемая версия релиза")
            if (newCode <= currentVersionCode) return@withContext null
            val assets = release.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == APK_NAME) {
                    val digest = asset.optString("digest")
                    require(digest.matches(Regex("sha256:[a-fA-F0-9]{64}"))) {
                        "Релиз не содержит проверяемого SHA-256. Установка остановлена."
                    }
                    val url = asset.getString("browser_download_url")
                    require(URL(url).protocol == "https") { "Only HTTPS releases allowed" }
                    return@withContext RemoteRelease(
                        tag, url, digest.substringAfter(":").lowercase()
                    )
                }
            }
            error("Релиз не содержит $APK_NAME")
        } finally { request.disconnect() }
    }

    suspend fun downloadVerified(release: RemoteRelease): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, APK_NAME)
        val temp = File(directory, "$APK_NAME.part")
        temp.delete()
        try {
            val connection = URL(release.assetUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 20000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            try {
                if (connection.responseCode != 200) error("Загрузка: HTTP ${connection.responseCode}")
                val digest = MessageDigest.getInstance("SHA-256")
                connection.inputStream.use { source ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val read = source.read(buffer)
                            if (read < 0) break
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual == release.sha256) { "Контрольная сумма APK не совпала" }
                val archive = context.packageManager.getPackageArchiveInfo(temp.absolutePath, 0)
                require(archive != null && archive.packageName == context.packageName) {
                    "APK не принадлежит приложению JARVIS"
                }
                target.delete()
                require(temp.renameTo(target)) { "Не удалось сохранить APK" }
                target
            } finally { connection.disconnect() }
        } catch (ex: Exception) {
            temp.delete()
            throw ex
        }
    }

    /** Returns false when Android installation approval is required. */
    fun requestInstall(apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settings)
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(install)
        return true
    }
}
