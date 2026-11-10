package dev.andikune.keyboardio.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONArray
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ClipboardRepositoryTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("keyboard_io_clipboard", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun keepsMoreThanThirtyItemsAndPersistsAfterRecreation() {
        val repository = ClipboardRepository(context)
        repeat(75) { repository.addClip("Clipboard item $it") }

        val restored = ClipboardRepository(context)
        assertEquals(75, restored.items.value.size)
        assertEquals("Clipboard item 74", restored.items.value.first().text)
        assertEquals("Clipboard item 0", restored.items.value.last().text)
    }

    @Test
    fun keepsRepeatedCopyEventsAsSeparateHistoryEntries() {
        val repository = ClipboardRepository(context)
        repository.addClip("teks yang sama")
        repository.addClip("teks yang sama")

        assertEquals(2, repository.items.value.size)
        assertTrue(repository.items.value[0].id != repository.items.value[1].id)
    }

    @Test
    fun keepsOldEntriesWithoutAutomaticExpiry() {
        val oldItem = JSONObject().apply {
            put("id", 100L)
            put("text", "riwayat lama")
            put("timestamp", 1L)
            put("isPinned", false)
        }
        val json = JSONArray().put(oldItem).toString()
        context.getSharedPreferences("keyboard_io_clipboard", Context.MODE_PRIVATE)
            .edit().putString("clips_json", json).commit()

        val repository = ClipboardRepository(context)
        assertEquals(1, repository.items.value.size)
        assertEquals("riwayat lama", repository.items.value.single().text)
    }

    @Test
    fun onlyExplicitDeleteActionsRemoveHistory() {
        val repository = ClipboardRepository(context)
        repository.addClip("keep pinned")
        repository.addClip("delete this")
        val pinned = repository.items.value.first { it.text == "keep pinned" }
        val removable = repository.items.value.first { it.text == "delete this" }

        repository.togglePin(pinned.id)
        repository.clearUnpinned()

        assertEquals(1, repository.items.value.size)
        assertEquals("keep pinned", repository.items.value.single().text)
        assertTrue(repository.items.value.single().isPinned)

        repository.removeItem(pinned.id)
        assertTrue(repository.items.value.isEmpty())
        assertFalse(repository.items.value.any { it.id == removable.id })
    }

    @Test
    fun repairsDuplicateLegacyIdsWithoutDroppingItems() {
        val duplicatedId = 42L
        val first = JSONObject().apply {
            put("id", duplicatedId)
            put("text", "first copy")
            put("timestamp", 2L)
            put("isPinned", false)
        }
        val second = JSONObject().apply {
            put("id", duplicatedId)
            put("text", "second copy")
            put("timestamp", 1L)
            put("isPinned", false)
        }
        val json = JSONArray().put(first).put(second).toString()
        context.getSharedPreferences("keyboard_io_clipboard", Context.MODE_PRIVATE)
            .edit().putString("clips_json", json).commit()

        val repository = ClipboardRepository(context)
        assertEquals(2, repository.items.value.size)
        assertEquals(2, repository.items.value.map { it.id }.distinct().size)
        assertTrue(repository.items.value.any { it.text == "first copy" })
        assertTrue(repository.items.value.any { it.text == "second copy" })
    }

    @Test
    fun preservesLongClipboardTextExactly() {
        val longText = buildString {
            repeat(120_000) { append(('a'.code + (it % 26)).toChar()) }
        }
        val repository = ClipboardRepository(context)
        repository.addClip(longText)

        val restored = ClipboardRepository(context)
        assertEquals(longText, restored.items.value.single().text)
    }
}
