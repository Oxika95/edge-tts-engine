package com.edgetts.engine.edge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class EdgeTtsVoicesTest {
    @Test
    fun matchingUsesExactLocaleWhenAvailable() {
        val voices = EdgeTtsVoices.matching(Locale.US)
        assertTrue(voices.isNotEmpty())
        assertTrue(voices.all { it.lang == "en-US" })
        assertTrue(voices.any { it.id == "en-US-AriaNeural" })
    }

    @Test
    fun voiceNamesStripLocaleAndNeuralSuffix() {
        val aria = EdgeTtsVoices.fromId("en-US-AriaNeural")
        assertEquals("Aria", aria?.name)
    }

    @Test
    fun localeKeysIgnoreCase() {
        assertEquals(
            EdgeTtsVoices.localeKey(Locale.US),
            EdgeTtsVoices.localeKey(Locale.forLanguageTag("en-US"))
        )
    }

    @Test
    fun catalogHasManyLocales() {
        assertTrue(EdgeTtsVoices.locales.size > 50)
        assertTrue(EdgeTtsVoices.all.size > 100)
    }
}

class TextChunkerTest {
    @Test
    fun shortTextStaysOneChunk() {
        val chunks = TextChunker.chunk("Hello world.")
        assertEquals(listOf("Hello world."), chunks)
    }

    @Test
    fun splitsOnSentenceBoundaries() {
        val text = "One. Two. Three."
        val chunks = TextChunker.chunk(text, maxChars = 10)
        assertTrue(chunks.size >= 2)
        assertTrue(chunks.all { it.length <= 10 || it == "Three." || it.contains("Three") })
    }

    @Test
    fun forceSplitsVeryLongSentence() {
        val long = "a".repeat(5000)
        val chunks = TextChunker.chunk(long, maxChars = 1000)
        assertTrue(chunks.size >= 5)
        assertTrue(chunks.all { it.length <= 1000 })
    }
}

class SecMsGecTest {
    @Test
    fun roundsToFiveMinuteWindows() {
        // Two timestamps in the same 5-minute window should produce the same token.
        val base = 1_700_000_000_000L // fixed epoch ms
        val a = EdgeTtsClient.generateSecMsGecForTest(base)
        val b = EdgeTtsClient.generateSecMsGecForTest(base + 60_000L)
        assertEquals(a, b)
        assertEquals(64, a.length)
    }

    @Test
    fun changesAcrossWindows() {
        val base = 1_700_000_000_000L
        val a = EdgeTtsClient.generateSecMsGecForTest(base)
        val b = EdgeTtsClient.generateSecMsGecForTest(base + 301_000L)
        assertTrue(a != b)
    }
}
