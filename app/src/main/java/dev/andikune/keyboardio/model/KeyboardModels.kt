package dev.andikune.keyboardio.model

import androidx.compose.ui.graphics.Color
import dev.andikune.keyboardio.ui.theme.NeonBlue
import dev.andikune.keyboardio.ui.theme.NeonCyan
import dev.andikune.keyboardio.ui.theme.NeonGold
import dev.andikune.keyboardio.ui.theme.NeonGreen
import dev.andikune.keyboardio.ui.theme.NeonMagenta
import dev.andikune.keyboardio.ui.theme.NeonPurple

enum class KeyboardLayoutMode {
    QWERTY_LOWER,
    QWERTY_UPPER,
    CAPS_LOCK,
    NUMBERS_SYMBOLS,
    MORE_SYMBOLS
}

enum class OneHandedMode {
    OFF,
    LEFT,
    RIGHT
}

enum class ActivePanel {
    NONE,
    GRID_MENU,
    EMOJI,
    CLIPBOARD,
    RESIZE
}

enum class RgbThemeStyle(
    val id: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color
) {
    RAINBOW("rainbow", "RGB Wave", NeonCyan, NeonMagenta, NeonGold),
    CYBERPUNK("cyberpunk", "Cyberpunk Neon", NeonMagenta, NeonCyan, NeonPurple),
    MATRIX("matrix", "Matrix Green", NeonGreen, NeonCyan, Color(0xFF00FF88)),
    ELECTRIC_BLUE("electric_blue", "Electric Blue", NeonBlue, NeonCyan, NeonPurple),
    SOLAR_FLAME("solar_flame", "Solar Neon", NeonGold, Color(0xFFFF5722), NeonMagenta)
}

sealed class KeyAction {
    data class CommitText(val text: String) : KeyAction()
    object Backspace : KeyAction()
    object Enter : KeyAction()
    object Shift : KeyAction()
    object SwitchToSymbols : KeyAction()
    object SwitchToLetters : KeyAction()
    object SwitchToMoreSymbols : KeyAction()
    object SwitchToPreviousSymbols : KeyAction()
    object Space : KeyAction()
    object OpenEmoji : KeyAction()
    object OpenGridMenu : KeyAction()
    object SwitchIme : KeyAction()
}

data class KeyItem(
    val primary: String,
    val secondary: String? = null,
    val weight: Float = 1f,
    val action: KeyAction = KeyAction.CommitText(primary),
    val isActionKey: Boolean = false
)

data class ClipboardItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
