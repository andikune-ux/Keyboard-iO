package dev.andikune.keyboardio.data

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashReportStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): String? = prefs.getString(KEY_REPORT, null)?.takeIf { it.isNotBlank() }

    fun clear() {
        prefs.edit().remove(KEY_REPORT).apply()
    }

    fun save(thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS XXX", Locale.US).format(Date())
        val report = buildString {
            appendLine("Keyboard iO crash report")
            appendLine("Waktu: $timestamp")
            appendLine("Thread: ${thread.name} (${thread.id})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Package: ${appContext.packageName}")
            appendLine()
            appendLine("${throwable::class.java.name}: ${throwable.message.orEmpty()}")
            appendLine(android.util.Log.getStackTraceString(throwable))
        }

        // commit() dipakai karena proses mungkin segera dihentikan oleh Android.
        prefs.edit().putString(KEY_REPORT, report).commit()
    }

    private companion object {
        const val PREFS_NAME = "keyboard_io_crash_reports"
        const val KEY_REPORT = "last_crash_report"
    }
}
