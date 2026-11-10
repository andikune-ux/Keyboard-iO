package dev.andikune.keyboardio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardLayoutPolicyTest {
    @Test fun numericEditorCannotSwitchToLetters() {
        assertNull(KeyboardLayouts.nextMode(
            KeyboardLayoutMode.NUMBERS_SYMBOLS,
            KeyAction.SwitchToLetters,
            isNumberOnly = true
        ))
    }

    @Test fun numericEditorCanOpenAdditionalSymbols() {
        assertEquals(
            KeyboardLayoutMode.MORE_SYMBOLS,
            KeyboardLayouts.nextMode(
                KeyboardLayoutMode.NUMBERS_SYMBOLS,
                KeyAction.SwitchToMoreSymbols,
                isNumberOnly = true
            )
        )
    }

    @Test fun numericEditorCanReturnToNumbersFromAdditionalSymbols() {
        assertEquals(
            KeyboardLayoutMode.NUMBERS_SYMBOLS,
            KeyboardLayouts.nextMode(
                KeyboardLayoutMode.MORE_SYMBOLS,
                KeyAction.SwitchToSymbols,
                isNumberOnly = true
            )
        )
    }

    @Test fun numericEditorIgnoresShift() {
        assertNull(KeyboardLayouts.nextMode(
            KeyboardLayoutMode.NUMBERS_SYMBOLS,
            KeyAction.Shift,
            isNumberOnly = true
        ))
    }

    @Test fun textEditorCanSwitchBetweenLettersAndSymbols() {
        assertEquals(
            KeyboardLayoutMode.NUMBERS_SYMBOLS,
            KeyboardLayouts.nextMode(
                KeyboardLayoutMode.QWERTY_LOWER,
                KeyAction.SwitchToSymbols,
                isNumberOnly = false
            )
        )
        assertEquals(
            KeyboardLayoutMode.QWERTY_LOWER,
            KeyboardLayouts.nextMode(
                KeyboardLayoutMode.NUMBERS_SYMBOLS,
                KeyAction.SwitchToLetters,
                isNumberOnly = false
            )
        )
    }

    @Test fun textEditorShiftCyclesCase() {
        assertEquals(
            KeyboardLayoutMode.QWERTY_UPPER,
            KeyboardLayouts.nextMode(
                KeyboardLayoutMode.QWERTY_LOWER,
                KeyAction.Shift,
                isNumberOnly = false
            )
        )
        assertEquals(
            KeyboardLayoutMode.CAPS_LOCK,
            KeyboardLayouts.nextMode(
                KeyboardLayoutMode.QWERTY_UPPER,
                KeyAction.Shift,
                isNumberOnly = false
            )
        )
    }
}
