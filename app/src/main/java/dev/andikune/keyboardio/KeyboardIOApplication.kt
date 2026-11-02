package dev.andikune.keyboardio

import android.app.Application
import android.util.Log
import dev.andikune.keyboardio.data.CrashReportStore

class KeyboardIOApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashStore = CrashReportStore(this)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                crashStore.save(thread, throwable)
            } catch (saveException: Exception) {
                Log.e(TAG, "Gagal menyimpan crash report", saveException)
            } finally {
                // Tetap teruskan ke handler Android agar perilaku force close standar tidak berubah.
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private companion object {
        const val TAG = "KeyboardIOApplication"
    }
}
