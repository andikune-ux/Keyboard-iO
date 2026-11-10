package dev.andikune.keyboardio.data

import android.view.inputmethod.InputConnection

/**
 * Reads a bounded, cursor-aware snapshot from the live editor connection.
 * Null surrounding text is treated as unavailable context, never as an empty document.
 */
object EditorInputConnectionContextReader {
    fun read(connection: InputConnection?): EditorTextContextSynchronizer.Snapshot? {
        if (connection == null) return null
        return try {
            val before = connection.getTextBeforeCursor(
                EditorTextContextSynchronizer.MAX_CONTEXT_CHARS, 0
            ) ?: return null
            val after = connection.getTextAfterCursor(
                EditorTextContextSynchronizer.MAX_CONTEXT_CHARS, 0
            ) ?: return null
            val selection = connection.getSelectedText(0)
            EditorTextContextSynchronizer.resolve(
                before = before.toString(),
                after = after.toString(),
                hasSelection = !selection.isNullOrEmpty()
            )
        } catch (_: Exception) {
            null
        }
    }
}
