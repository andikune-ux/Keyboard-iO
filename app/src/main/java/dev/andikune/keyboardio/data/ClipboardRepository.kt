package dev.andikune.keyboardio.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dev.andikune.keyboardio.model.ClipboardItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ClipboardRepository(context: Context) {
    private companion object {
        const val TAG = "ClipboardRepository"
        const val MAX_ITEMS = 30
        const val RETENTION_DAYS = 7
        const val RETENTION_MS = RETENTION_DAYS * 24L * 60L * 60L * 1000L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("keyboard_io_clipboard", Context.MODE_PRIVATE)

    private val _items = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val items: StateFlow<List<ClipboardItem>> = _items.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val raw = prefs.getString("clips_json", null)
        if (raw.isNullOrBlank()) {
            _items.value = emptyList()
            return
        }

        try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<ClipboardItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ClipboardItem(
                        id = obj.optLong("id", System.currentTimeMillis() + i),
                        text = obj.getString("text"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
            val now = System.currentTimeMillis()
            val seededExamples = setOf(
                "Keyboard iO - Next-Gen RGB Typing",
                "https://github.com/andikune",
                "dev.andikune.keyboardio"
            )
            val retained = list.filterNot { it.text in seededExamples }
                .filter { it.isPinned || now - it.timestamp <= RETENTION_MS }
                .sortedByDescending { it.timestamp }
            _items.value = retained
            if (retained.size != list.size) saveItems(retained)
        } catch (exception: Exception) {
            Log.e(TAG, "Gagal membaca clipboard tersimpan; memakai daftar kosong", exception)
            _items.value = emptyList()
        }
    }

    @Synchronized
    private fun saveItems(list: List<ClipboardItem>) {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("timestamp", item.timestamp)
                put("isPinned", item.isPinned)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("clips_json", jsonArray.toString()).apply()
    }

    fun addClip(text: String) {
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        val current = _items.value.filter { it.isPinned || now - it.timestamp <= RETENTION_MS }.toMutableList()
        val existingIndex = current.indexOfFirst { it.text == text }
        if (existingIndex >= 0) {
            val existing = current.removeAt(existingIndex)
            current.add(0, existing.copy(timestamp = now))
        } else {
            current.add(0, ClipboardItem(text = text))
        }
        // Limit to 30 items max, keep pinned items
        val pinned = current.filter { it.isPinned }
        // Jangan pernah meneruskan nilai negatif ke take() jika data lama
        // memiliki lebih dari MAX_ITEMS item pinned.
        val unpinnedLimit = (MAX_ITEMS - pinned.size).coerceAtLeast(0)
        val unpinned = current.filterNot { it.isPinned }.take(unpinnedLimit)
        val updated = pinned.sortedByDescending { it.timestamp } + unpinned.sortedByDescending { it.timestamp }
        _items.value = updated
        saveItems(updated)
    }

    fun togglePin(id: Long) {
        val updated = _items.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        _items.value = updated
        saveItems(updated)
    }

    fun removeItem(id: Long) {
        val updated = _items.value.filterNot { it.id == id }
        _items.value = updated
        saveItems(updated)
    }

    fun clearUnpinned() {
        val updated = _items.value.filter { it.isPinned }
        _items.value = updated
        saveItems(updated)
    }
}
