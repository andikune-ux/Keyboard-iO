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
        // Suggestions must be EMPTY when prefix is empty (only appear when approximating active input)
        val emptySuggestions = WordSuggester.getSuggestions("")
        assertTrue(emptySuggestions.isEmpty())

        val whitespaceSuggestions = WordSuggester.getSuggestions("   ")
        assertTrue(whitespaceSuggestions.isEmpty())

        val suggestions = WordSuggester.getSuggestions("kam")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.startsWith("kam", ignoreCase = true) })

        val engSuggestions = WordSuggester.getSuggestions("hel")
        assertTrue(engSuggestions.contains("hello"))

        // Typo correction test: "sy" -> "saya" or "kmu" -> "kamu"
        val corrected = WordSuggester.findAutoCorrection("kmu")
        assertEquals("kamu", corrected)
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
        assertEquals("kamar", suggestions.first())
        assertEquals("kamu", WordSuggester.findAutoCorrection("kmu", learned))
        assertEquals(null, WordSuggester.findAutoCorrection("teh", learned))
    }

    @Test
    fun testNicknameFontGenerator() {
        val sample = "Andik"
        val sultan = dev.andikune.keyboardio.data.NicknameFontGenerator.convertText(sample, "gamer_wing")
        assertTrue(sultan.contains("Andik"))

        val gothic = dev.andikune.keyboardio.data.NicknameFontGenerator.convertText(sample, "gothic")
        assertNotNull(gothic)
        assertTrue(gothic.isNotEmpty())
    }
}
