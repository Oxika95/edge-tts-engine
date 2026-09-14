package com.edgetts.engine.edge

/**
 * Splits text into Edge SSML-sized chunks on sentence boundaries.
 */
object TextChunker {
    private const val MAX_CHUNK_CHARS = 1800

    private val SENTENCE_SPLIT = Regex("(?<=[.!?。！？…])\\s+")

    fun chunk(text: String, maxChars: Int = MAX_CHUNK_CHARS): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxChars) return listOf(trimmed)

        val sentences = SENTENCE_SPLIT.split(trimmed).map { it.trim() }.filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return forceSplit(trimmed, maxChars)

        val chunks = ArrayList<String>()
        val current = StringBuilder()
        for (sentence in sentences) {
            if (sentence.length > maxChars) {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                chunks.addAll(forceSplit(sentence, maxChars))
                continue
            }
            if (current.isNotEmpty() && current.length + 1 + sentence.length > maxChars) {
                chunks.add(current.toString().trim())
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }
        return chunks
    }

    private fun forceSplit(text: String, maxChars: Int): List<String> {
        val out = ArrayList<String>()
        var start = 0
        while (start < text.length) {
            var end = minOf(start + maxChars, text.length)
            if (end < text.length) {
                val space = text.lastIndexOf(' ', end - 1)
                if (space > start + maxChars / 2) {
                    end = space
                }
            }
            out.add(text.substring(start, end).trim())
            start = end
            while (start < text.length && text[start].isWhitespace()) start++
        }
        return out.filter { it.isNotEmpty() }
    }
}
