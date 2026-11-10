package dev.andikune.keyboardio.data

/**
 * Finds the previous user-perceived character (extended grapheme-like cluster)
 * before the cursor. Offsets are UTF-16 because Kotlin String and InputConnection
 * expose UTF-16, while deletion count is expressed in Unicode code points.
 */
object UnicodeBackspaceEngine {
    data class Deletion(val utf16Start: Int, val codePointCount: Int)

    fun previousGrapheme(textBeforeCursor: String): Deletion? {
        if (textBeforeCursor.isEmpty()) return null

        var start = textBeforeCursor.length
        var current = Character.codePointBefore(textBeforeCursor, start)
        start -= Character.charCount(current)
        var regionalIndicators = if (isRegionalIndicator(current)) 1 else 0

        while (start > 0) {
            val previous = Character.codePointBefore(textBeforeCursor, start)
            val joinCurrent = current == ZWJ
            val joinPrevious = previous == ZWJ
            val attachCurrent = isExtend(current)
            val pairRegional = isRegionalIndicator(previous) &&
                isRegionalIndicator(current) && regionalIndicators % 2 == 1
            val pairCarriageReturn = previous == CR && current == LF

            if (!joinCurrent && !joinPrevious && !attachCurrent &&
                !pairRegional && !pairCarriageReturn
            ) break

            start -= Character.charCount(previous)
            if (isRegionalIndicator(previous)) regionalIndicators++
            current = previous
        }

        val deleted = textBeforeCursor.substring(start)
        return Deletion(
            utf16Start = start,
            codePointCount = deleted.codePointCount(0, deleted.length)
        )
    }

    private fun isExtend(codePoint: Int): Boolean {
        val type = Character.getType(codePoint)
        return type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt() ||
            codePoint in 0xFE00..0xFE0F ||
            codePoint in 0xE0100..0xE01EF ||
            codePoint in 0x1F3FB..0x1F3FF ||
            codePoint in 0xE0020..0xE007F ||
            codePoint == 0x20E3
    }

    private fun isRegionalIndicator(codePoint: Int) = codePoint in 0x1F1E6..0x1F1FF

    private const val ZWJ = 0x200D
    private const val CR = 0x000D
    private const val LF = 0x000A
}
