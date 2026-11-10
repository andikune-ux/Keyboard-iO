package dev.andikune.keyboardio.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TypingActivityStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("keyboard_io_typing_activity", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun persistsFullTextAndSessionStartEndTimes() {
        val store = TypingActivityStore(context)
        val text = "Halo 😀\nIni teks lengkap yang diketik."
        val startedAt = System.currentTimeMillis() - 60_000L
        val endedAt = System.currentTimeMillis()
        store.recordSession(text, startedAt, endedAt)

        val restored = TypingActivityStore(context).getStats().sessions.single()
        assertEquals(text, restored.text)
        assertEquals(startedAt, restored.startedAt)
        assertEquals(endedAt, restored.endedAt)
        assertEquals(text.codePointCount(0, text.length), restored.charCount)
        assertTrue(restored.wordCount > 0)
    }

    @Test
    fun activeDraftIsRecoveredAfterServiceProcessRestart() {
        val store = TypingActivityStore(context)
        val startedAt = System.currentTimeMillis() - 10_000L
        store.beginSession(startedAt)
        store.updateActiveSession(startedAt, "teks yang sudah diketik 😀")

        val restored = TypingActivityStore(context).getStats().sessions.single()
        assertEquals("teks yang sudah diketik 😀", restored.text)
        assertEquals(startedAt, restored.startedAt)
    }

    @Test
    fun sessionExpiresTwentyFourHoursAfterItWasSaved() {
        val now = System.currentTimeMillis()
        val expired = JSONObject().apply {
            put("id", "expired")
            put("startedAt", now - 25L * 60 * 60 * 1000)
            put("endedAt", now - 24L * 60 * 60 * 1000 - 1L)
            put("createdAt", now - 24L * 60 * 60 * 1000 - 1L)
            put("text", "expired text")
            put("charCount", 12)
            put("wordCount", 2)
            put("durationMs", 60_000L)
        }
        val fresh = JSONObject().apply {
            put("id", "fresh")
            put("startedAt", now - 60_000L)
            put("endedAt", now - 1_000L)
            put("createdAt", now - 1_000L)
            put("text", "fresh text")
            put("charCount", 10)
            put("wordCount", 2)
            put("durationMs", 59_000L)
        }
        val raw = JSONArray().put(expired).put(fresh).toString()
        context.getSharedPreferences("keyboard_io_typing_activity", Context.MODE_PRIVATE)
            .edit().putString("typing_sessions_data", raw).commit()

        val sessions = TypingActivityStore(context).getStats().sessions
        assertEquals(1, sessions.size)
        assertEquals("fresh text", sessions.single().text)
    }

    @Test
    fun clearHistoryAlsoRemovesInterruptedDraft() {
        val store = TypingActivityStore(context)
        val startedAt = System.currentTimeMillis()
        store.beginSession(startedAt)
        store.updateActiveSession(startedAt, "temporary text")
        store.clearAll()

        val restored = TypingActivityStore(context).getStats()
        assertTrue(restored.sessions.isEmpty())
    }

    @Test
    fun emptySessionIsStillRecordedWithOpenAndCloseTimes() {
        val store = TypingActivityStore(context)
        val start = System.currentTimeMillis() - 2_000L
        val end = System.currentTimeMillis()
        store.recordSession("", start, end)

        val session = store.getStats().sessions.single()
        assertEquals("", session.text)
        assertEquals(start, session.startedAt)
        assertEquals(end, session.endedAt)
    }
}
