package dev.andikune.keyboardio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LongTextChunkerTest {
    @Test fun preservesEntireLongPayloadAcrossChunks() {
        val input = buildString {
            repeat(120_000) { append(('a'.code + (it % 26)).toChar()) }
        }
        val chunks = LongTextChunker.split(input)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= LongTextChunker.DEFAULT_CHUNK_SIZE })
        assertEquals(input, chunks.joinToString(""))
    }

    @Test fun neverSplitsSurrogatePairs() {
        val input = "a".repeat(3_999) + "😀" + "b".repeat(5_000)
        val chunks = LongTextChunker.split(input, 4_000)
        assertEquals(input, chunks.joinToString(""))
        assertTrue(chunks.none { it.isNotEmpty() && Character.isHighSurrogate(it.last()) })
        assertTrue(chunks.none { it.isNotEmpty() && Character.isLowSurrogate(it.first()) })
    }

    @Test fun handlesEmptyAndSmallInputs() {
        assertTrue(LongTextChunker.split("").isEmpty())
        assertEquals(listOf("short"), LongTextChunker.split("short"))
    }
}
