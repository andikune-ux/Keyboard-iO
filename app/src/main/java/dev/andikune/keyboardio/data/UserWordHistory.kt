package dev.andikune.keyboardio.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class UserWordHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _words = MutableStateFlow(loadWords())
    val words: StateFlow<Map<String, Int>> = _words.asStateFlow()
    private val _bigrams = MutableStateFlow(loadBigrams())
    val bigrams: StateFlow<Map<String, Map<String, Int>>> = _bigrams.asStateFlow()

    @Synchronized
    fun learn(rawWord: String, previousWord: String? = null) {
        val word = normalize(rawWord) ?: return
        val current = _words.value.toMutableMap()
        current[word] = (current[word] ?: 0) + 1
        val trimmed = current.entries.sortedByDescending { it.value }
            .take(MAX_WORDS).associate { it.key to it.value }
        _words.value = trimmed

        val previous = normalize(previousWord.orEmpty())
        if (previous != null && previous != word) {
            val updated = _bigrams.value.mapValues { it.value.toMutableMap() }.toMutableMap()
            val nextCounts = updated.getOrPut(previous) { mutableMapOf() }
            nextCounts[word] = (nextCounts[word] ?: 0) + 1
            updated[previous] = nextCounts.entries.sortedByDescending { it.value }
                .take(MAX_NEXT_WORDS).associate { it.key to it.value }

            val retainedPairs = updated.flatMap { (before, next) ->
                next.map { (after, count) -> Triple(before, after, count) }
            }.sortedByDescending { it.third }.take(MAX_BIGRAMS)
            val bounded = retainedPairs.groupBy { it.first }.mapValues { (_, pairs) ->
                pairs.associate { it.second to it.third }
            }
            _bigrams.value = bounded
            prefs.edit().putString(KEY_BIGRAMS, encodeBigrams(bounded)).apply()
        }

        prefs.edit().putString(KEY_WORDS, encode(trimmed)).apply()
    }

    private fun loadWords(): Map<String, Int> {
        val raw = prefs.getString(KEY_WORDS, null).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split('|').mapNotNull { item ->
            val separator = item.lastIndexOf('=')
            if (separator <= 0) return@mapNotNull null
            val word = normalize(item.substring(0, separator)) ?: return@mapNotNull null
            val count = item.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
            word to count.coerceAtLeast(1)
        }.toMap()
    }

    private fun loadBigrams(): Map<String, Map<String, Int>> {
        val raw = prefs.getString(KEY_BIGRAMS, null).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val result = mutableMapOf<String, MutableMap<String, Int>>()
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val before = normalize(item.optString("before")) ?: continue
                val after = normalize(item.optString("after")) ?: continue
                val count = item.optInt("count", 0).coerceAtLeast(0)
                if (count > 0) {
                    val next = result.getOrPut(before) { mutableMapOf() }
                    next[after] = (next[after] ?: 0) + count
                }
            }
            result.mapValues { (_, next) ->
                next.entries.sortedByDescending { it.value }.take(MAX_NEXT_WORDS)
                    .associate { it.key to it.value }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encode(words: Map<String, Int>): String =
        words.entries.joinToString("|") { "${it.key}=${it.value}" }

    private fun encodeBigrams(bigrams: Map<String, Map<String, Int>>): String {
        val array = JSONArray()
        bigrams.forEach { (before, nextWords) ->
            nextWords.forEach { (after, count) ->
                array.put(JSONObject().apply {
                    put("before", before)
                    put("after", after)
                    put("count", count)
                })
            }
        }
        return array.toString()
    }

    private fun normalize(rawWord: String): String? {
        val word = rawWord.trim().lowercase(Locale.ROOT)
        return word.takeIf {
            it.length in 2..40 && it.all { char -> char.isLetter() || char == '\'' }
        }
    }

    private companion object {
        const val PREFS_NAME = "keyboard_io_word_history"
        const val KEY_WORDS = "learned_words"
        const val KEY_BIGRAMS = "learned_bigrams"
        const val MAX_WORDS = 500
        const val MAX_NEXT_WORDS = 12
        const val MAX_BIGRAMS = 1000
    }
}
