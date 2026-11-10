package dev.andikune.keyboardio.data

import android.view.inputmethod.InputConnection

/**
 * Replaces text around the current cursor as one editor batch.
 * If insertion fails after deletion, the original token is restored before returning.
 */
object EditorWordReplacement {
    data class Result(
        val succeeded: Boolean,
        val rollbackSucceeded: Boolean
    )

    fun replace(
        connection: InputConnection,
        beforeCodePoints: Int,
        afterCodePoints: Int,
        originalText: String,
        replacementText: String
    ): Result {
        var deleted = false
        try {
            connection.beginBatchEdit()
            deleted = connection.deleteSurroundingTextInCodePoints(
                beforeCodePoints.coerceAtLeast(0),
                afterCodePoints.coerceAtLeast(0)
            )
            if (!deleted) return Result(succeeded = false, rollbackSucceeded = true)

            if (connection.commitText(replacementText, 1)) {
                return Result(succeeded = true, rollbackSucceeded = true)
            }

            val restored = originalText.isEmpty() || connection.commitText(originalText, 1)
            return Result(succeeded = false, rollbackSucceeded = restored)
        } catch (_: Exception) {
            val restored = if (deleted && originalText.isNotEmpty()) {
                runCatching { connection.commitText(originalText, 1) }.getOrDefault(false)
            } else {
                !deleted || originalText.isEmpty()
            }
            return Result(succeeded = false, rollbackSucceeded = restored)
        } finally {
            runCatching { connection.endBatchEdit() }
        }
    }
}
