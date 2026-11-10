package dev.andikune.keyboardio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTextContextSynchronizerTest {
    @Test fun readsComposingAndPreviousWordsAtCursorEnd() {
        val context = EditorTextContextSynchronizer.resolve("hello wor", "")
        assertEquals("wor", context.composingWord)
        assertEquals("hello", context.previousWord)
        assertTrue(context.cursorAtWordEnd)
        assertTrue(context.canReplaceCurrentWord)
        assertFalse(context.canPredictNextWord)
    }

    @Test fun predictsOnlyAtEditorEndAfterCompletedWord() {
        val context = EditorTextContextSynchronizer.resolve("hello ", "")
        assertEquals("", context.composingWord)
        assertEquals("hello", context.previousWord)
        assertTrue(context.canPredictNextWord)
    }

    @Test fun combinesBothSidesWhenCursorIsInsideWord() {
        val context = EditorTextContextSynchronizer.resolve("hel", "lo, world")
        assertEquals("hello", context.composingWord)
        assertEquals("hel", context.prefixBeforeCursor)
        assertEquals("lo", context.suffixAfterCursor)
        assertEquals(", world", context.textAfterWord)
        assertFalse(context.cursorAtWordEnd)
        assertTrue(context.canReplaceCurrentWord)
        assertFalse(context.canPredictNextWord)
    }

    @Test fun preservesPunctuationAfterCurrentWord() {
        val context = EditorTextContextSynchronizer.resolve("hello", ", world")
        assertEquals("hello", context.composingWord)
        assertEquals(", world", context.textAfterWord)
    }

    @Test fun selectionDisablesComposingAndPrediction() {
        val context = EditorTextContextSynchronizer.resolve("hello ", "world", hasSelection = true)
        assertTrue(context.hasSelection)
        assertEquals("", context.composingWord)
        assertEquals("", context.previousWord)
        assertFalse(context.isReliable)
    }

    @Test fun handlesCombiningMarksAndApostrophes() {
        val context = EditorTextContextSynchronizer.resolve("cafe\u0301 can'", "t")
        assertEquals("can't", context.composingWord)
        assertEquals("cafe\u0301", context.previousWord)
    }

    @Test fun handlesNonLatinLetters() {
        val context = EditorTextContextSynchronizer.resolve("こんにちは ", "")
        assertEquals("こんにちは", context.previousWord)
        assertTrue(context.canPredictNextWord)
    }

    @Test fun refusesPartialLongWordsAtContextBoundaries() {
        val longPrefix = "a".repeat(EditorTextContextSynchronizer.MAX_CONTEXT_CHARS)
        val context = EditorTextContextSynchronizer.resolve(longPrefix, "")
        assertFalse(context.isReliable)
        assertFalse(context.canReplaceCurrentWord)
        assertEquals("", context.composingWord)
    }
    @Test fun autocorrectionAddsSpaceOnlyAtDocumentEnd() {
        assertEquals("the ", EditorTextContextSynchronizer.autoCorrectionCommitText("the", ""))
        assertEquals("the", EditorTextContextSynchronizer.autoCorrectionCommitText("the", " "))
        assertEquals("the", EditorTextContextSynchronizer.autoCorrectionCommitText("the", ", lanjut"))
        assertEquals("the", EditorTextContextSynchronizer.autoCorrectionCommitText("the", "."))
    }
}

