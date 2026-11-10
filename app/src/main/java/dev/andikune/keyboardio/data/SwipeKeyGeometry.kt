package dev.andikune.keyboardio.data

/** Maps pointer coordinates to the staggered QWERTY letter rows. */
object SwipeKeyGeometry {
    private const val TOTAL_UNITS = 10f

    fun letterAt(x: Float, y: Float, width: Float, rowHeight: Float): Char? {
        if (!x.isFinite() || !y.isFinite() || !width.isFinite() || !rowHeight.isFinite()) return null
        if (width <= 0f || rowHeight <= 0f || x < 0f || x >= width || y < 0f) return null
        val row = (y / rowHeight).toInt()
        val position = x / width * TOTAL_UNITS
        return when (row) {
            0 -> findLetter(position, 0f, "qwertyuiop", List(10) { 1f })
            1 -> findLetter(position, 0.5f, "asdfghjkl", List(9) { 1f })
            2 -> findLetter(position, 1.5f, "zxcvbnm", List(7) { 1f })
            else -> null
        }
    }

    private fun findLetter(position: Float, start: Float, letters: String, weights: List<Float>): Char? {
        var cursor = start
        for (index in letters.indices) {
            val end = cursor + weights[index]
            if (position >= cursor && position < end) return letters[index]
            cursor = end
        }
        return null
    }
}
