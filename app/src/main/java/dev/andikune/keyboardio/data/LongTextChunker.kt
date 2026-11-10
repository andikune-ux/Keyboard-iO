package dev.andikune.keyboardio.data

/**
 * Splits large IME insertions into Binder-safe pieces without breaking UTF-16 surrogate pairs.
 */
object LongTextChunker {
    const val DEFAULT_CHUNK_SIZE = 4_000

    fun split(text: String, maxChunkSize: Int = DEFAULT_CHUNK_SIZE): List<String> {
        require(maxChunkSize >= 2) { "Chunk size must be at least 2 UTF-16 units" }
        if (text.isEmpty()) return emptyList()
        if (text.length <= maxChunkSize) return listOf(text)

        val chunks = ArrayList<String>((text.length / maxChunkSize) + 1)
        var start = 0
        while (start < text.length) {
            var end = minOf(start + maxChunkSize, text.length)
            if (end < text.length &&
                Character.isHighSurrogate(text[end - 1]) &&
                Character.isLowSurrogate(text[end])
            ) {
                end--
            }
            if (end == start) end = minOf(start + 2, text.length)
            chunks.add(text.substring(start, end))
            start = end
        }
        return chunks
    }
}
