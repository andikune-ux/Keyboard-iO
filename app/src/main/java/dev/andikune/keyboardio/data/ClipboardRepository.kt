package dev.andikune.keyboardio.data

import android.content.Context
import android.content.SharedPreferences
import dev.andikune.keyboardio.model.ClipboardItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ClipboardRepository(context: Context) {
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
            val defaults = listOf(
                ClipboardItem(id = 1, text = "Keyboard iO - Next-Gen RGB Typing", isPinned = true),
                ClipboardItem(id = 2, text = "https://github.com/andikune", isPinned = false),
                ClipboardItem(id = 3, text = "dev.andikune.keyboardio", isPinned = false)
            )
            _items.value = defaults
            saveItems(defaults)
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
            _items.value = list
        } catch (_: Exception) {
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
        val current = _items.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.text == text }
        if (existingIndex >= 0) {
            val existing = current.removeAt(existingIndex)
            current.add(0, existing.copy(timestamp = System.currentTimeMillis()))
        } else {
            current.add(0, ClipboardItem(text = text))
        }
        // Limit to 30 items max, keep pinned items
        val pinned = current.filter { it.isPinned }
        val unpinned = current.filterNot { it.isPinned }.take(30 - pinned.size)
        val updated = pinned + unpinned
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
