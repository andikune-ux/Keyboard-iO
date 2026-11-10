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

class ClipboardRepository internal constructor(context: Context) {
    companion object {
        private const val TAG = "ClipboardRepository"

        // Remove only the exact demo entries shipped by an older build.
        // Real clipboard records are never expired, capped, or deduplicated.
        private val LEGACY_DEMO_TEXTS = setOf(
            "Keyboard iO - Next-Gen RGB Typing",
            "https://github.com/andikune",
            "dev.andikune.keyboardio"
        )

        @Volatile
        private var instance: ClipboardRepository? = null

        fun getInstance(context: Context): ClipboardRepository =
            instance ?: synchronized(this) {
                instance ?: ClipboardRepository(context.applicationContext).also { instance = it }
            }

        internal fun resetInstanceForTests() {
            synchronized(this) { instance = null }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("keyboard_io_clipboard", Context.MODE_PRIVATE)

    private val _items = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val items: StateFlow<List<ClipboardItem>> = _items.asStateFlow()
    private var nextItemId: Long = System.currentTimeMillis()

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
            // One-time cleanup of known fake demo entries from older releases.
            // This does not affect any real copied text, including duplicate copies.
            val realHistory = list.filterNot { it.text in LEGACY_DEMO_TEXTS }
            // Repair duplicate legacy IDs without discarding real history entries.
            // Never expire, cap, or deduplicate real clipboard history.
            val highestExistingId = realHistory.maxOfOrNull { it.id } ?: 0L
            var generatedId = maxOf(System.currentTimeMillis(), highestExistingId + 1L)
            val usedIds = mutableSetOf<Long>()
            val withUniqueIds = realHistory.map { item ->
                if (usedIds.add(item.id)) {
                    item
                } else {
                    item.copy(id = generatedId++).also { usedIds.add(it.id) }
                }
            }
            _items.value = withUniqueIds.sortedWith(
                compareByDescending<ClipboardItem> { it.timestamp }.thenByDescending { it.id }
            )
            nextItemId = generatedId
            if (withUniqueIds.map { it.id } != list.map { it.id } || realHistory.size != list.size) {
                saveItems(_items.value)
            }
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

    @Synchronized
    fun addClip(text: String) {
        if (text.isEmpty()) return

        // Each copy is a separate history event, even when the text is identical.
        // IDs are monotonic so LazyColumn keys remain unique for rapid consecutive copies.
        val item = ClipboardItem(
            id = nextItemId++,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        val updated = listOf(item) + _items.value
        _items.value = updated
        saveItems(updated)
    }

    @Synchronized
    fun togglePin(id: Long) {
        val updated = _items.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        _items.value = updated
        saveItems(updated)
    }

    @Synchronized
    fun removeItem(id: Long) {
        val updated = _items.value.filterNot { it.id == id }
        _items.value = updated
        saveItems(updated)
    }

    /** Explicit user action: remove only unpinned entries; pinned entries remain. */
    @Synchronized
    fun clearUnpinned() {
        val updated = _items.value.filter { it.isPinned }
        _items.value = updated
        saveItems(updated)
    }
}
