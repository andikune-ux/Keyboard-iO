package dev.andikune.keyboardio.data

/**
 * Smart delete engine providing context-aware deletion for Keyboard iO.
 * Intelligently recognizes sentence boundaries (.!?), newlines (\n),
 * clause/punctuation boundaries (,, ;), and words.
 */
object SmartDeleteEngine {

    /**
     * Calculates the number of characters before the cursor to delete.
     * Stops at:
     * - Newlines (\n)
     * - Sentence boundaries (.!? followed by space or at boundary)
     * - Clause boundaries (,, ;, :)
     * - Punctuation marks themselves
     * - Word boundaries (if no punctuation or newline is present)
     */
    fun calculateSmartDeleteCount(textBefore: String): Int {
        if (textBefore.isEmpty()) return 0
        val len = textBefore.length

        var i = len - 1
        // Skip trailing spaces (excluding newlines)
        while (i >= 0 && textBefore[i] == ' ') {
            i--
        }
        if (i < 0) return len // was all spaces

        // If cursor was right at or immediately after a newline
        if (textBefore[i] == '\n') {
            return len - i
        }

        // If cursor was right after a punctuation mark (comma, period, exclamation, question mark, etc.)
        val ch = textBefore[i]
        if (ch == ',' || ch == '.' || ch == '?' || ch == '!' || ch == ';' || ch == ':') {
            // Delete this punctuation mark plus the preceding word
            i--
            while (i >= 0 && textBefore[i] == ' ') i--
            while (i >= 0 && !textBefore[i].isWhitespace() && textBefore[i] !in ",.?!;:\n") {
                i--
            }
            return len - (i + 1)
        }

        // Search backwards from i to find the nearest structural boundary:
        // 1. Newline (\n)
        // 2. Sentence boundary ('.', '?', '!')
        // 3. Clause boundary (',', ';', ':')
        var boundaryIndex = -1

        for (idx in i downTo 0) {
            val c = textBefore[idx]
            if (c == '\n') {
                boundaryIndex = idx + 1 // delete everything on the current line after newline
                break
            }
            if (c == '.' || c == '?' || c == '!') {
                boundaryIndex = idx + 1 // delete everything in the current sentence
                break
            }
            if (c == ',' || c == ';' || c == ':') {
                boundaryIndex = idx + 1 // delete everything in the current clause
                break
            }
        }

        if (boundaryIndex != -1 && boundaryIndex < len) {
            val count = len - boundaryIndex
            if (count > 0) return count
        }

        // If no punctuation or newline found, delete the word
        var wordStart = i
        while (wordStart >= 0 && !textBefore[wordStart].isWhitespace()) {
            wordStart--
        }
        val count = len - (wordStart + 1)
        return if (count > 0) count else 1
    }
}
