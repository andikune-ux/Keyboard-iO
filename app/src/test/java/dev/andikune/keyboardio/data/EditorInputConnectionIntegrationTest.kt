package dev.andikune.keyboardio.data

import android.text.Selection
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputConnection
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EditorInputConnectionIntegrationTest {
    @Test fun readsWordAndPunctuationFromRealBaseInputConnectionCursor() {
        val connection = BaseInputConnection(TextView(ApplicationProvider.getApplicationContext<Context>()), true)
        val editable = connection.editable ?: error("BaseInputConnection did not expose its editable buffer")
        editable.append("hello wor, next")
        Selection.setSelection(editable, 9)

        val snapshot = EditorInputConnectionContextReader.read(connection)

        assertEquals("wor", snapshot?.composingWord)
        assertEquals(", next", snapshot?.textAfterWord)
        assertTrue(snapshot?.cursorAtWordEnd == true)
    }

    @Test fun cursorMovementChangesTheResolvedWordContext() {
        val connection = BaseInputConnection(TextView(ApplicationProvider.getApplicationContext<Context>()), true)
        val editable = connection.editable ?: error("BaseInputConnection did not expose its editable buffer")
        editable.append("hello world")
        Selection.setSelection(editable, 8)

        val snapshot = EditorInputConnectionContextReader.read(connection)

        assertEquals("world", snapshot?.composingWord)
        assertFalse(snapshot?.cursorAtWordEnd == true)
    }

    @Test fun activeSelectionDisablesComposingContext() {
        val connection = BaseInputConnection(TextView(ApplicationProvider.getApplicationContext<Context>()), true)
        val editable = connection.editable ?: error("BaseInputConnection did not expose its editable buffer")
        editable.append("hello world")
        Selection.setSelection(editable, 6, 11)

        val snapshot = EditorInputConnectionContextReader.read(connection)

        assertTrue(snapshot?.hasSelection == true)
        assertEquals("", snapshot?.composingWord)
    }

    @Test fun nullConnectionAndUnavailableSurroundingTextReturnNoSnapshot() {
        assertNull(EditorInputConnectionContextReader.read(null))

        val connection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getTextBeforeCursor" -> null
                "getTextAfterCursor" -> "after"
                else -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    else -> null
                }
            }
        } as InputConnection

        assertNull(EditorInputConnectionContextReader.read(connection))
    }

    @Test fun editorConnectionExceptionsAreHandledAsUnavailableContext() {
        val connection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java)
        ) { _, method, _ ->
            if (method.name == "getTextBeforeCursor") {
                throw IllegalStateException("editor connection is no longer valid")
            }
            null
        } as InputConnection

        assertNull(EditorInputConnectionContextReader.read(connection))
    }
}
