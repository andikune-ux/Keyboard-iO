package dev.andikune.keyboardio.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.andikune.keyboardio.BuildConfig
import dev.andikune.keyboardio.data.UpdateInfo
import dev.andikune.keyboardio.data.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class UpdateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val checking = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        UpdateManager.init(this)
        UpdateManager.showStoredIfAllowed()

        if (intent?.action == ACTION_INSTALL) {
            val info = UpdateManager.pending.value ?: UpdateManager.stored()
            if (info != null) {
                scope.launch {
                    try {
                        downloadAndInstall(info)
                    } finally {
                        stopSelf(startId)
                    }
                }
            } else {
                stopSelf(startId)
            }
        } else if (checking.compareAndSet(false, true)) {
            scope.launch {
                try {
                    checkLatestRelease()
                } finally {
                    checking.set(false)
                    stopSelf(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun checkLatestRelease() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CHECK, 0L) < CHECK_INTERVAL_MS) return
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Keyboard-iO/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode !in 200..299) return
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(body)
            val tag = release.optString("tag_name").ifBlank { return }
            val assets = release.optJSONArray("assets") ?: return
            var apkUrl = ""
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isBlank()) return
            UpdateManager.publish(
                UpdateInfo(
                    version = tag,
                    title = release.optString("name").ifBlank { "Keyboard iO $tag" },
                    notes = release.optString("body").take(2_000),
                    htmlUrl = release.optString("html_url"),
                    downloadUrl = apkUrl
                )
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadAndInstall(info: UpdateInfo) {
        val updatesDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.resolve("Keyboard-iO/Pembaruan aplikasi") ?: return
        if (!updatesDir.exists() && !updatesDir.mkdirs()) return

        val safeVersion = info.version.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val apkFile = updatesDir.resolve("Keyboard-iO-$safeVersion.apk")
        val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Keyboard-iO/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode !in 200..299) return
            connection.inputStream.use { input ->
                apkFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (apkFile.length() > 0L) launchInstaller(apkFile)
        } finally {
            connection.disconnect()
        }
    }

    private fun launchInstaller(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(permissionIntent)
            return
        }

        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(installIntent)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/andikune-ux/Keyboard-iO/releases/latest"
        const val ACTION_INSTALL = "dev.andikune.keyboardio.action.INSTALL_UPDATE"
        private const val PREFS_NAME = "keyboard_io_update"
        private const val KEY_LAST_CHECK = "last_check_ms"
        private const val CHECK_INTERVAL_MS = 30 * 60 * 1000L

        fun check(context: android.content.Context) {
            UpdateManager.init(context)
            UpdateManager.showStoredIfAllowed()
            runCatching { context.startService(Intent(context, UpdateService::class.java)) }
        }

        fun install(context: android.content.Context) {
            UpdateManager.init(context)
            runCatching {
                context.startService(Intent(context, UpdateService::class.java).setAction(ACTION_INSTALL))
            }
        }
    }
}
