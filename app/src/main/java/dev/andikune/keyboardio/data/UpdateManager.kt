package dev.andikune.keyboardio.data

import android.content.Context
import dev.andikune.keyboardio.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared in-process state for the Activity and the IME update prompt. */
data class UpdateInfo(
    val version: String,
    val title: String,
    val notes: String,
    val htmlUrl: String,
    val downloadUrl: String
)

object UpdateManager {
    private const val PREFS_NAME = "keyboard_io_update"
    private const val KEY_VERSION = "stored_version"
    private const val KEY_TITLE = "stored_title"
    private const val KEY_NOTES = "stored_notes"
    private const val KEY_HTML_URL = "stored_html_url"
    private const val KEY_DOWNLOAD_URL = "stored_download_url"
    private const val KEY_SKIPPED_VERSION = "skipped_version"

    private lateinit var appContext: Context
    private val _pending = MutableStateFlow<UpdateInfo?>(null)
    val pending: StateFlow<UpdateInfo?> = _pending.asStateFlow()

    fun init(context: Context) {
        if (!::appContext.isInitialized) appContext = context.applicationContext
    }

    fun publish(info: UpdateInfo) {
        if (isSkipped(info.version) || !isNewer(info.version)) return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_VERSION, info.version)
            .putString(KEY_TITLE, info.title)
            .putString(KEY_NOTES, info.notes)
            .putString(KEY_HTML_URL, info.htmlUrl)
            .putString(KEY_DOWNLOAD_URL, info.downloadUrl)
            .apply()
        _pending.value = info
    }

    fun showStoredIfAllowed() {
        if (!::appContext.isInitialized) return
        val info = stored() ?: return
        val version = info.version
        if (!isSkipped(version) && isNewer(version)) _pending.value = info
    }

    fun stored(): UpdateInfo? {
        if (!::appContext.isInitialized) return null
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        return UpdateInfo(
            version = version,
            title = prefs.getString(KEY_TITLE, "Pembaruan Keyboard iO") ?: "Pembaruan Keyboard iO",
            notes = prefs.getString(KEY_NOTES, "") ?: "",
            htmlUrl = prefs.getString(KEY_HTML_URL, "") ?: "",
            downloadUrl = prefs.getString(KEY_DOWNLOAD_URL, "") ?: ""
        )
    }

    fun dismissForNow() {
        _pending.value = null
    }

    fun skip(version: String) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SKIPPED_VERSION, version).apply()
        _pending.value = null
    }

    private fun isSkipped(version: String): Boolean =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SKIPPED_VERSION, null) == version

    private fun isNewer(remote: String): Boolean {
        val remoteParts = parseVersion(remote)
        val localParts = parseVersion(BuildConfig.VERSION_NAME)
        if (remoteParts == null || localParts == null) return remote != BuildConfig.VERSION_NAME
        return remoteParts.zip(localParts).any { it.first > it.second } ||
                remoteParts.size > localParts.size && remoteParts.drop(localParts.size).any { it > 0 }
    }

    private fun parseVersion(raw: String): List<Int>? {
        val values = raw.trim().removePrefix("v").split(".")
        if (values.isEmpty() || values.any { it.toIntOrNull() == null }) return null
        return values.map { it.toInt() }.let { it + List((4 - it.size).coerceAtLeast(0)) { 0 } }
    }
}
