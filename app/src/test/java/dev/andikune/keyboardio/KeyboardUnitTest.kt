package dev.andikune.keyboardio

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
        assertEquals("spasi", row4[2].primary)
        assertEquals("↵", row4[4].primary)
    }

    @Test
    fun testUppercaseQwerty() {
        val row1Upper = KeyboardLayouts.getQwertyRow1(true)
        assertEquals("Q", row1Upper[0].primary)
        assertEquals("1", row1Upper[0].secondary)
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
}
