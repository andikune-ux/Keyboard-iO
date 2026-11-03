package dev.andikune.keyboardio.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class UserWordHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _words = MutableStateFlow(loadWords())
    val words: StateFlow<Map<String, Int>> = _words.asStateFlow()

    fun learn(rawWord: String) {
        val word = normalize(rawWord) ?: return
        val current = _words.value.toMutableMap()
        current[word] = (current[word] ?: 0) + 1
        val trimmed = current.entries
            .sortedByDescending { it.value }
            .take(MAX_WORDS)
            .associate { it.key to it.value }
        _words.value = trimmed
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

    private fun encode(words: Map<String, Int>): String = words.entries.joinToString("|") { "${it.key}=${it.value}" }

    private fun normalize(rawWord: String): String? {
        val word = rawWord.trim().lowercase(Locale.ROOT)
        return word.takeIf { it.length >= 2 && it.length <= 40 && it.all { char -> char.isLetter() || char == '\'' } }
    }

    private companion object {
        const val PREFS_NAME = "keyboard_io_word_history"
        const val KEY_WORDS = "learned_words"
        const val MAX_WORDS = 500
    }
}
