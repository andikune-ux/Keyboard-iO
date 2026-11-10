package dev.andikune.keyboardio.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorAutoCorrectionPolicyTest {
    private fun allowed(
        manuallyEdited: Boolean = false,
        reliable: Boolean = true,
        selection: Boolean = false,
        cursorAtEnd: Boolean = true,
        word: String = "typo"
    ) = EditorAutoCorrectionPolicy.shouldCorrect(
        isEnabled = true,
        editorAllowsCorrection = true,
        wasManuallyEdited = manuallyEdited,
        contextIsReliable = reliable,
        hasSelection = selection,
        cursorAtWordEnd = cursorAtEnd,
        word = word
    )

    @Test fun allowsOnlyUntouchedReliableWordAtCursorEnd() {
        assertTrue(allowed())
    }

    @Test fun neverCorrectsWordAfterManualBackspaceOrDeletion() {
        assertFalse(allowed(manuallyEdited = true))
    }

    @Test fun rejectsUnreliableContextSelectionAndMidWordCursor() {
        assertFalse(allowed(reliable = false))
        assertFalse(allowed(selection = true))
        assertFalse(allowed(cursorAtEnd = false))
    }

    @Test fun rejectsWordsShorterThanThreeCharacters() {
        assertFalse(allowed(word = "ok"))
    }
}
