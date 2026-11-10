package dev.andikune.keyboardio

import dev.andikune.keyboardio.data.WordSuggester
import dev.andikune.keyboardio.model.EnterActionType
import dev.andikune.keyboardio.model.KeyboardLayouts
import dev.andikune.keyboardio.model.RgbThemeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardUnitTest {

    @Test
    fun testQwertyRowCounts() {
        val row1 = KeyboardLayouts.getQwertyRow1(false)
        assertEquals(10, row1.size)
        assertEquals("q", row1[0].primary)
        assertEquals("1", row1[0].secondary)
        assertEquals("p", row1[9].primary)
        assertEquals("0", row1[9].secondary)

        val row2 = KeyboardLayouts.getQwertyRow2(false)
        assertEquals(9, row2.size)
        assertEquals("a", row2[0].primary)
        assertEquals("@", row2[0].secondary)

        val row3 = KeyboardLayouts.getQwertyRow3(false)
        // Shift + 7 letters + Backspace = 9 keys
        assertEquals(9, row3.size)
        assertEquals("⇧", row3[0].primary)
        assertEquals("⌫", row3[8].primary)

        val row4 = KeyboardLayouts.getQwertyRow4()
        assertEquals(5, row4.size)
        assertEquals("?123", row4[0].primary)
        assertEquals(",", row4[1].primary)
        assertEquals("␣", row4[2].primary)
        assertEquals(".", row4[3].primary)
        assertEquals("↵", row4[4].primary)
        // Also test custom spacebar label
        val row4Custom = KeyboardLayouts.getQwertyRow4(spacebarLabel = "Keyboard iO")
        assertEquals("Keyboard iO", row4Custom[2].primary)
    }

    @Test
    fun testUppercaseQwerty() {
        val row1Upper = KeyboardLayouts.getQwertyRow1(true)
        assertEquals("Q", row1Upper[0].primary)
        assertEquals("1", row1Upper[0].secondary)
    }

    @Test
    fun testDynamicEnterLabel() {
        val row4Search = KeyboardLayouts.getQwertyRow4(EnterActionType.SEARCH.displayLabel)
        assertEquals("🔍", row4Search[4].primary)

        val row4Send = KeyboardLayouts.getQwertyRow4(EnterActionType.SEND.displayLabel)
        assertEquals("➤", row4Send[4].primary)
    }

    @Test
    fun testSymbolsLayout() {
        val symRow1 = KeyboardLayouts.getSymbolsRow1()
        assertEquals(10, symRow1.size)
        assertEquals("1", symRow1[0].primary)

        val symRow2 = KeyboardLayouts.getSymbolsRow2()
        assertEquals(10, symRow2.size)
        assertEquals("@", symRow2[0].primary)
    }

    @Test
    fun testRgbThemeStyles() {
        assertTrue(RgbThemeStyle.entries.size >= 5)
        val rainbow = RgbThemeStyle.entries.find { it.id == "rainbow" }
        assertNotNull(rainbow)
        val cyberpunk = RgbThemeStyle.entries.find { it.id == "cyberpunk" }
        assertNotNull(cyberpunk)
    }

    @Test
    fun testWordSuggesterAndAutoCorrection() {
        // While typing: completions are empty for blank input
        val emptySuggestions = WordSuggester.getWordCompletionsAndCorrections("")
        assertTrue(emptySuggestions.isEmpty())

        val whitespaceSuggestions = WordSuggester.getWordCompletionsAndCorrections("   ")
        assertTrue(whitespaceSuggestions.isEmpty())

        // Next word predictions: returns top starters when previous word is blank
        val nextWordBlank = WordSuggester.getNextWordPredictions("")
        assertTrue(nextWordBlank.isEmpty())

        val suggestions = WordSuggester.getWordCompletionsAndCorrections("kam")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.contains("kam", ignoreCase = true) })

        val engSuggestions = WordSuggester.getWordCompletionsAndCorrections("hel")
        assertTrue(engSuggestions.any { it.contains("hel", ignoreCase = true) })

        // Exact user text is always preserved as the first suggestion.
        assertEquals("kam", WordSuggester.getWordCompletionsAndCorrections("kam").first())
        assertTrue(WordSuggester.getNextWordPredictions("").isEmpty())
        val personalPredictions = WordSuggester.getNextWordPredictions(
            "saya",
            learnedBigrams = mapOf("saya" to mapOf("ngopi" to 12, "makan" to 2))
        )
        assertEquals("ngopi", personalPredictions.first())
        val corrected = WordSuggester.findAutoCorrection("kmu")
        assertEquals("kamu", corrected)
        assertEquals(null, WordSuggester.findAutoCorrection("keyboard", mapOf("keyboard" to 20)))
        assertEquals(null, WordSuggester.findAutoCorrection("Andi"))
        assertEquals("kamu", WordSuggester.getSwipeSuggestion("kamu"))
        assertEquals(null, WordSuggester.findAutoCorrection("Kmu"))
    }

    @Test
    fun testSmartDeleteEngineSentenceAndPunctuation() {
        // 1. Sentence boundary (.!?)
        val sentenceText = "Ini kalimat pertama. Ini kalimat kedua"
        val countSentence = dev.andikune.keyboardio.data.SmartDeleteEngine.calculateSmartDeleteCount(sentenceText)
        assertEquals(" Ini kalimat kedua".length, countSentence)

        // 2. Comma / clause boundary (,)
        val commaText = "Halo kawan, apa kabar"
        val countComma = dev.andikune.keyboardio.data.SmartDeleteEngine.calculateSmartDeleteCount(commaText)
        assertEquals(" apa kabar".length, countComma)

        // 3. Newline boundary (\n)
        val newlineText = "Baris satu\nBaris dua"
        val countNewline = dev.andikune.keyboardio.data.SmartDeleteEngine.calculateSmartDeleteCount(newlineText)
        assertEquals("Baris dua".length, countNewline)

        // 4. Cursor right on punctuation
        val punctText = "Halo kawan,"
        val countPunct = dev.andikune.keyboardio.data.SmartDeleteEngine.calculateSmartDeleteCount(punctText)
        assertEquals("kawan,".length, countPunct)

        // 5. Cursor on newline
        val justNewline = "Baris satu\n"
        val countJustNewline = dev.andikune.keyboardio.data.SmartDeleteEngine.calculateSmartDeleteCount(justNewline)
        assertEquals(1, countJustNewline)

        // 6. Plain word
        val wordText = "Halo dunia"
        val countWord = dev.andikune.keyboardio.data.SmartDeleteEngine.calculateSmartDeleteCount(wordText)
        assertEquals("dunia".length, countWord)

        // Unicode emoji is one code point, not two UTF-16 code units.
        assertEquals(1, dev.andikune.keyboardio.data.SmartDeleteEngine.calculateSmartDeleteCount("Halo 😀"))
    }

    @Test
    fun testTypingSessionRecordStoresFullUnicodeTextAndTimes() {
        val text = "Halo 😀\nIni seluruh teks yang diketik."
        val start = 1_800_000_000_000L
        val end = start + 95_000L
        val session = dev.andikune.keyboardio.data.TypingSessionRecord(
            startedAt = start,
            endedAt = end,
            createdAt = end,
            text = text
        )
        assertEquals(text, session.text)
        assertEquals(start, session.startedAt)
        assertEquals(end, session.endedAt)
        assertEquals(text.codePointCount(0, text.length), session.charCount)
        assertEquals(95_000L, session.durationMs)
    }

    @Test
    fun testOfflineTranslator() {
        val translatedEn = dev.andikune.keyboardio.data.OfflineTranslator.translateIdToEn("Selamat pagi")
        assertTrue(translatedEn.equals("Good morning", ignoreCase = true))

        val translatedId = dev.andikune.keyboardio.data.OfflineTranslator.translateEnToId("Thank you")
        assertTrue(translatedId.equals("Terima kasih", ignoreCase = true))

        assertTrue(dev.andikune.keyboardio.data.OfflineTranslator.detectIdToEn("saya sedang belajar"))
        assertTrue(!dev.andikune.keyboardio.data.OfflineTranslator.detectIdToEn("I am learning"))
        assertTrue(dev.andikune.keyboardio.data.OfflineTranslator.translateAuto("selamat pagi").contains("good morning", ignoreCase = true))
        assertTrue(dev.andikune.keyboardio.data.OfflineTranslator.translateAuto("thank you").contains("terima kasih", ignoreCase = true))
    }

    @Test
    fun testLearnedWordsRankSuggestionsAndCorrection() {
        val learned = mapOf("kamar" to 20, "kami" to 1)
        val suggestions = WordSuggester.getSuggestions("kam", learnedWords = learned)
        assertEquals("kam", suggestions.first())
        assertTrue(suggestions.contains("kamar"))
        assertEquals("kamu", WordSuggester.findAutoCorrection("kmu", learned))
        assertEquals(null, WordSuggester.findAutoCorrection("teh", learned))
    }

    @Test
    fun testNicknameFontGeneratorSupportsExpandedUnicodeStyles() {
        val sample = "Keyboard iO 123 😀 café"
        val styles = dev.andikune.keyboardio.data.NicknameFontGenerator.STYLES
        assertTrue(styles.size >= 20)
        assertEquals(styles.size, styles.map { it.id }.distinct().size)

        styles.forEach { style ->
            val converted = dev.andikune.keyboardio.data.NicknameFontGenerator.convertText(sample, style.id)
            assertNotNull(converted)
            assertTrue("Style ${style.id} should keep emoji", converted.contains("😀"))
            assertTrue("Style ${style.id} should keep spaces", converted.contains(" "))
        }

        assertEquals(
            sample,
            dev.andikune.keyboardio.data.NicknameFontGenerator.convertText(sample, "unknown-style")
        )
        assertTrue(
            dev.andikune.keyboardio.data.NicknameFontGenerator.convertText("ABC xyz 123", "fullwidth")
                .contains("ＡＢＣ")
        )
    }
}
