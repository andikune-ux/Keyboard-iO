package dev.andikune.keyboardio.data

import java.util.Locale

/** Derives active-word and next-word context from both sides of the editor cursor. */
object EditorTextContextSynchronizer {
    const val MAX_CONTEXT_CHARS = 256

    data class Snapshot(
        val composingWord: String,
        val previousWord: String,
        val prefixBeforeCursor: String,
        val suffixAfterCursor: String,
        val textAfterWord: String,
        val cursorAtWordEnd: Boolean,
        val canReplaceCurrentWord: Boolean,
        val canPredictNextWord: Boolean,
        val hasSelection: Boolean,
        val isReliable: Boolean
    ) {
        companion object {
            val EMPTY = Snapshot("", "", "", "", "", false, false, false, false, false)
        }
    }

    fun resolve(before: String?, after: String?, hasSelection: Boolean = false): Snapshot {
        if (hasSelection) return EMPTY_SELECTION
        val left = before.orEmpty()
        val right = after.orEmpty()
        val prefixStart = wordStart(left)
        val suffixEnd = wordEnd(right)
        val prefix = left.substring(prefixStart)
        val suffix = right.substring(0, suffixEnd)

        // Never expose a partial token when the bounded editor context cuts through it.
        val prefixTruncated = left.length >= MAX_CONTEXT_CHARS && prefixStart == 0 &&
            left.isNotEmpty() && isWordCodePoint(Character.codePointAt(left, 0))
        val suffixTruncated = right.length >= MAX_CONTEXT_CHARS && suffixEnd == right.length &&
            right.isNotEmpty() && isWordCodePoint(Character.codePointBefore(right, right.length))
        val reliable = !prefixTruncated && !suffixTruncated
        val current = if (reliable) prefix + suffix else ""
        val previous = if (reliable) extractLastWord(left.substring(0, prefixStart)) else ""
        val textAfterWord = right.substring(suffixEnd)
        val canPredict = reliable && current.isEmpty() && right.isEmpty()

        return Snapshot(
            composingWord = current,
            previousWord = previous,
            prefixBeforeCursor = prefix,
            suffixAfterCursor = suffix,
            textAfterWord = textAfterWord,
            cursorAtWordEnd = suffix.isEmpty(),
            canReplaceCurrentWord = reliable && current.isNotEmpty(),
            canPredictNextWord = canPredict,
            hasSelection = false,
            isReliable = reliable
        )
    }

    private val EMPTY_SELECTION = Snapshot("", "", "", "", "", false, false, false, true, false)

    private fun wordStart(text: String): Int {
        var index = text.length
        while (index > 0) {
            val cp = Character.codePointBefore(text, index)
            if (!isWordCodePoint(cp)) break
            index -= Character.charCount(cp)
        }
        return index
    }

    private fun wordEnd(text: String): Int {
        var index = 0
        while (index < text.length) {
            val cp = Character.codePointAt(text, index)
            if (!isWordCodePoint(cp)) break
            index += Character.charCount(cp)
        }
        return index
    }

    private fun extractLastWord(text: String): String {
        var end = text.length
        while (end > 0) {
            val cp = Character.codePointBefore(text, end)
            if (isWordCodePoint(cp)) break
            end -= Character.charCount(cp)
        }
        val wordEnd = end
        while (end > 0) {
            val cp = Character.codePointBefore(text, end)
            if (!isWordCodePoint(cp)) break
            end -= Character.charCount(cp)
        }
        return text.substring(end, wordEnd).lowercase(Locale.ROOT)
    }

    private fun isWordCodePoint(cp: Int): Boolean {
        val type = Character.getType(cp)
        return Character.isLetterOrDigit(cp) ||
            type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt() ||
            cp == '\''.code || cp == 0x2019 || cp == '-'.code
    }
}
