package dev.andikune.keyboardio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnicodeBackspaceEngineTest {
    private fun deleted(text: String): String {
        val deletion = UnicodeBackspaceEngine.previousGrapheme(text) ?: return ""
        return text.substring(deletion.utf16Start)
    }

    @Test fun deletesAsciiOneCharacter() {
        val d = UnicodeBackspaceEngine.previousGrapheme("hello")
        assertEquals("o", deleted("hello"))
        assertEquals(1, d?.codePointCount)
        assertEquals(4, d?.utf16Start)
    }

    @Test fun deletesSupplementaryEmojiAsOneVisibleCharacter() {
        val text = "hello😀"
        val d = UnicodeBackspaceEngine.previousGrapheme(text)
        assertEquals("😀", deleted(text))
        assertEquals(1, d?.codePointCount)
        assertEquals(2, text.length - (d?.utf16Start ?: text.length))
    }

    @Test fun deletesEmojiWithSkinToneAsOneCluster() {
        val text = "👍🏽"
        val d = UnicodeBackspaceEngine.previousGrapheme(text)
        assertEquals(text, deleted(text))
        assertEquals(2, d?.codePointCount)
    }

    @Test fun deletesFamilyZwjSequenceAsOneCluster() {
        val text = "A👨‍👩‍👧‍👦"
        val d = UnicodeBackspaceEngine.previousGrapheme(text)
        assertEquals("👨‍👩‍👧‍👦", deleted(text))
        assertEquals(7, d?.codePointCount)
    }

    @Test fun deletesFlagPairAsOneCluster() {
        val text = "ID🇮🇩"
        val d = UnicodeBackspaceEngine.previousGrapheme(text)
        assertEquals("🇮🇩", deleted(text))
        assertEquals(2, d?.codePointCount)
    }

    @Test fun deletesCombiningAccentWithBaseLetter() {
        val text = "cafe\u0301"
        val d = UnicodeBackspaceEngine.previousGrapheme(text)
        assertEquals("e\u0301", deleted(text))
        assertEquals(2, d?.codePointCount)
    }

    @Test fun deletesKeycapSequenceAsOneCluster() {
        val text = "1️⃣"
        val d = UnicodeBackspaceEngine.previousGrapheme(text)
        assertEquals(text, deleted(text))
        assertEquals(3, d?.codePointCount)
    }

    @Test fun deletesOnlyOneOfTwoAdjacentEmojiClusters() {
        val text = "😀😁"
        val d = UnicodeBackspaceEngine.previousGrapheme(text)
        assertEquals("😁", deleted(text))
        assertEquals(1, d?.codePointCount)
    }

    @Test fun handlesEmptyInput() {
        assertNull(UnicodeBackspaceEngine.previousGrapheme(""))
    }
}
