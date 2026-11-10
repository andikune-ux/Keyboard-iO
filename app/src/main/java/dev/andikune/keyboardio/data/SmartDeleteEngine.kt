package dev.andikune.keyboardio.data

/**
 * Context-aware deletion using Unicode code points, not UTF-16 code units.
 * Stops at line, sentence, clause, punctuation, or word boundaries.
 */
object SmartDeleteEngine {
    fun calculateSmartDeleteCount(textBefore: String): Int {
        if (textBefore.isEmpty()) return 0
        val points = textBefore.codePoints().toArray()
        val length = points.size
        var index = length - 1

        while (index >= 0 && points[index] == ' '.code) index--
        if (index < 0) return length
        if (points[index] == '\n'.code) return length - index

        fun isBoundary(point: Int): Boolean =
            point == ','.code || point == '.'.code || point == '?'.code ||
                point == '!'.code || point == ';'.code || point == ':'.code || point == '\n'.code

        if (isBoundary(points[index]) && points[index] != '\n'.code) {
            index--
            while (index >= 0 && points[index] == ' '.code) index--
            while (index >= 0 && !Character.isWhitespace(points[index]) && !isBoundary(points[index])) index--
            return length - (index + 1)
        }

        for (cursor in index downTo 0) {
            if (isBoundary(points[cursor])) return length - (cursor + 1)
        }

        var wordStart = index
        while (wordStart >= 0 && !Character.isWhitespace(points[wordStart])) wordStart--
        return (length - (wordStart + 1)).coerceAtLeast(1)
    }
}
