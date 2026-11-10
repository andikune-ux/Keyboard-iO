package dev.andikune.keyboardio.data

import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class EditorWordReplacementTest {
    private data class FakeEditor(
        var deleteResult: Boolean = true,
        var commitResults: MutableList<Boolean> = mutableListOf(true),
        val committed: MutableList<String> = mutableListOf(),
        var deleteCalls: Int = 0,
        var beginCalls: Int = 0,
        var endCalls: Int = 0
    ) {
        fun connection(): InputConnection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "beginBatchEdit" -> { beginCalls++; true }
                "endBatchEdit" -> { endCalls++; true }
                "deleteSurroundingTextInCodePoints" -> { deleteCalls++; deleteResult }
                "commitText" -> {
                    committed += args?.get(0)?.toString().orEmpty()
                    if (commitResults.isEmpty()) false else commitResults.removeAt(0)
                }
                else -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    else -> null
                }
            }
        } as InputConnection
    }

    @Test fun failedDeleteDoesNotCommitOrLoseOriginalText() {
        val fake = FakeEditor(deleteResult = false)
        val result = EditorWordReplacement.replace(fake.connection(), 3, 0, "typo", "fixed")

        assertFalse(result.succeeded)
        assertTrue(result.rollbackSucceeded)
        assertTrue(fake.committed.isEmpty())
        assertEquals(1, fake.deleteCalls)
        assertEquals(1, fake.endCalls)
    }

    @Test fun failedCommitAttemptsToRestoreOriginalWord() {
        val fake = FakeEditor(commitResults = mutableListOf(false, true))
        val result = EditorWordReplacement.replace(fake.connection(), 4, 0, "typo", "fixed")

        assertFalse(result.succeeded)
        assertTrue(result.rollbackSucceeded)
        assertEquals(listOf("fixed", "typo"), fake.committed)
        assertEquals(1, fake.beginCalls)
        assertEquals(1, fake.endCalls)
    }

    @Test fun successfulReplacementUsesOneBatch() {
        val fake = FakeEditor()
        val result = EditorWordReplacement.replace(fake.connection(), 4, 0, "typo", "fixed")

        assertTrue(result.succeeded)
        assertEquals(listOf("fixed"), fake.committed)
        assertEquals(1, fake.beginCalls)
        assertEquals(1, fake.endCalls)
    }
}
