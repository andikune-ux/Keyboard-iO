package dev.andikune.keyboardio.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class TypingSessionRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val charCount: Int = 0,
    val wordCount: Int = 0,
    val durationMs: Long = 0L
)

data class TypingActivityStats(
    val totalCharacters: Int,
    val totalWords: Int,
    val totalSessions: Int,
    val totalDurationMs: Long,
    val sessions: List<TypingSessionRecord>
)

/**
 * Stores and manages typing activity sessions.
 * Automatically purges / resets any session records older than 24 hours
 * to respect user privacy and adhere to the 24-hour retention requirement.
 */
class TypingActivityStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _sessions = MutableStateFlow(loadAndPruneSessions())
    val sessions: StateFlow<List<TypingSessionRecord>> = _sessions.asStateFlow()

    fun recordSession(charCount: Int, wordCount: Int, durationMs: Long) {
        if (charCount <= 0 && durationMs < 500L) return

        val now = System.currentTimeMillis()
        val currentPruned = _sessions.value.filter { now - it.timestamp <= RETENTION_MS }.toMutableList()

        val newRecord = TypingSessionRecord(
            id = UUID.randomUUID().toString(),
            timestamp = now,
            charCount = charCount,
            wordCount = wordCount.coerceAtLeast(if (charCount > 0) 1 else 0),
            durationMs = durationMs.coerceAtLeast(0L)
        )

        currentPruned.add(0, newRecord) // latest first
        val capped = currentPruned.take(MAX_SESSIONS)
        _sessions.value = capped
        saveSessions(capped)
    }

    fun getStats(): TypingActivityStats {
        val now = System.currentTimeMillis()
        val valid = _sessions.value.filter { now - it.timestamp <= RETENTION_MS }
        return TypingActivityStats(
            totalCharacters = valid.sumOf { it.charCount },
            totalWords = valid.sumOf { it.wordCount },
            totalSessions = valid.size,
            totalDurationMs = valid.sumOf { it.durationMs },
            sessions = valid
        )
    }

    fun clearAll() {
        _sessions.value = emptyList()
        prefs.edit().remove(KEY_SESSIONS).apply()
    }

    private fun loadAndPruneSessions(): List<TypingSessionRecord> {
        val raw = prefs.getString(KEY_SESSIONS, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        val now = System.currentTimeMillis()
        val parsed = raw.split(';').mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size >= 4) {
                val timestamp = parts[0].toLongOrNull() ?: return@mapNotNull null
                val charCount = parts[1].toIntOrNull() ?: 0
                val wordCount = parts[2].toIntOrNull() ?: 0
                val durationMs = parts[3].toLongOrNull() ?: 0L
                val id = if (parts.size >= 5) parts[4] else UUID.randomUUID().toString()
                TypingSessionRecord(id, timestamp, charCount, wordCount, durationMs)
            } else null
        }

        // Auto-reset / purge items older than 24 hours
        val valid = parsed.filter { now - it.timestamp <= RETENTION_MS }
        if (valid.size != parsed.size) {
            saveSessions(valid)
        }
        return valid
    }

    private fun saveSessions(list: List<TypingSessionRecord>) {
        val encoded = list.joinToString(";") {
            "${it.timestamp}:${it.charCount}:${it.wordCount}:${it.durationMs}:${it.id}"
        }
        prefs.edit().putString(KEY_SESSIONS, encoded).apply()
    }

    companion object {
        private const val PREFS_NAME = "keyboard_io_typing_activity"
        private const val KEY_SESSIONS = "typing_sessions_data"
        private const val RETENTION_MS = 24 * 60 * 60 * 1000L // 24 Hours
        private const val MAX_SESSIONS = 200
    }
}
