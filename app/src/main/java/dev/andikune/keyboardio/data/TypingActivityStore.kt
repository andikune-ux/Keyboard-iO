package dev.andikune.keyboardio.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val SESSION_WORD_PATTERN = Regex("[\\p{L}\\p{N}]+")

data class TypingSessionRecord(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val text: String = "",
    val charCount: Int = text.codePointCount(0, text.length),
    val wordCount: Int = SESSION_WORD_PATTERN.findAll(text).count(),
    val durationMs: Long = (endedAt - startedAt).coerceAtLeast(0L)
) {
    val timestamp: Long get() = endedAt
}

data class TypingActivityStats(
    val totalCharacters: Int,
    val totalWords: Int,
    val totalSessions: Int,
    val totalDurationMs: Long,
    val sessions: List<TypingSessionRecord>
)

class TypingActivityStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _sessions = MutableStateFlow(loadAndPruneSessions())
    val sessions: StateFlow<List<TypingSessionRecord>> = _sessions.asStateFlow()

    @Synchronized
    fun recordSession(text: String, startedAt: Long, endedAt: Long) {
        val now = System.currentTimeMillis()
        val current = prune(_sessions.value, now).toMutableList()
        current.add(
            TypingSessionRecord(
                startedAt = startedAt,
                endedAt = endedAt,
                createdAt = now,
                text = text,
                charCount = text.codePointCount(0, text.length),
                wordCount = SESSION_WORD_PATTERN.findAll(text).count(),
                durationMs = (endedAt - startedAt).coerceAtLeast(0L)
            )
        )
        val updated = current.sortedByDescending { it.createdAt }
        _sessions.value = updated
        saveSessions(updated)
    }

    @Synchronized
    fun getStats(): TypingActivityStats {
        val valid = prune(_sessions.value, System.currentTimeMillis())
        if (valid.size != _sessions.value.size) {
            _sessions.value = valid
            saveSessions(valid)
        }
        return TypingActivityStats(
            totalCharacters = valid.sumOf { it.charCount },
            totalWords = valid.sumOf { it.wordCount },
            totalSessions = valid.size,
            totalDurationMs = valid.sumOf { it.durationMs },
            sessions = valid
        )
    }

    @Synchronized
    fun clearAll() {
        _sessions.value = emptyList()
        prefs.edit().remove(KEY_SESSIONS).apply()
    }

    private fun loadAndPruneSessions(): List<TypingSessionRecord> {
        val raw = prefs.getString(KEY_SESSIONS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        val parsed = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val text = item.optString("text", "")
                    val startedAt = item.optLong("startedAt", item.optLong("timestamp", 0L))
                    val endedAt = item.optLong("endedAt", item.optLong("timestamp", startedAt))
                    add(
                        TypingSessionRecord(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            startedAt = startedAt,
                            endedAt = endedAt,
                            createdAt = item.optLong("createdAt", endedAt),
                            text = text,
                            charCount = item.optInt("charCount", text.codePointCount(0, text.length)),
                            wordCount = item.optInt("wordCount", SESSION_WORD_PATTERN.findAll(text).count()),
                            durationMs = item.optLong("durationMs", (endedAt - startedAt).coerceAtLeast(0L))
                        )
                    )
                }
            }
        }.getOrElse {
            // Migrate the previous timestamp:charCount:wordCount:duration:id format.
            raw.split(';').mapNotNull { entry ->
                val parts = entry.split(':')
                if (parts.size < 4) return@mapNotNull null
                val endedAt = parts[0].toLongOrNull() ?: return@mapNotNull null
                val duration = parts[3].toLongOrNull() ?: 0L
                TypingSessionRecord(
                    id = parts.getOrNull(4) ?: UUID.randomUUID().toString(),
                    startedAt = (endedAt - duration).coerceAtLeast(0L),
                    endedAt = endedAt,
                    createdAt = endedAt,
                    text = "",
                    charCount = parts[1].toIntOrNull() ?: 0,
                    wordCount = parts[2].toIntOrNull() ?: 0,
                    durationMs = duration
                )
            }
        }
        val valid = prune(parsed, System.currentTimeMillis())
        if (valid.size != parsed.size || !raw.startsWith("[")) saveSessions(valid)
        return valid
    }

    private fun prune(records: List<TypingSessionRecord>, now: Long): List<TypingSessionRecord> =
        records.filter { now >= it.createdAt && now - it.createdAt < RETENTION_MS }
            .sortedByDescending { it.createdAt }

    private fun saveSessions(records: List<TypingSessionRecord>) {
        val array = JSONArray()
        records.forEach { session ->
            array.put(JSONObject().apply {
                put("id", session.id)
                put("startedAt", session.startedAt)
                put("endedAt", session.endedAt)
                put("createdAt", session.createdAt)
                put("text", session.text)
                put("charCount", session.charCount)
                put("wordCount", session.wordCount)
                put("durationMs", session.durationMs)
            })
        }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "keyboard_io_typing_activity"
        const val KEY_SESSIONS = "typing_sessions_data"
        const val RETENTION_MS = 24 * 60 * 60 * 1000L
    }
}
