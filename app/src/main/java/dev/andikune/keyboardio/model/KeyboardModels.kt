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
    RESIZE,
    TRANSLATE,
    NICKNAME_FONT,
    VOICE_INPUT
}

enum class SoundEffectType(val id: String, val displayName: String) {
    MECHANICAL("mechanical", "Mekanikal (Clicky)"),
    TYPEWRITER("typewriter", "Mesin Tik Klasik"),
    DIGITAL_POP("digital_pop", "Digital Pop (Bubble)"),
    STANDARD("standard", "Standard Click")
}

enum class SpacebarStyle(val id: String, val displayLabel: String) {
    DEFAULT("default", "spasi"),
    BRANDED("branded", "Keyboard iO"),
    SYMBOL("symbol", "␣ Space"),
    NEON_BOLT("neon_bolt", "⚡ iO"),
    NEON_RGB("neon_rgb", "⌨ Neon iO")
}

enum class EnterActionType(val displayLabel: String) {
    RETURN("↵"),
    SEARCH("🔍"),
    SEND("➤"),
    GO("➔"),
    NEXT("⇥"),
    DONE("✓")
}

data class KeyboardEditorState(
    val isPassword: Boolean = false,
    val isNumberOnly: Boolean = false,
    val enterAction: EnterActionType = EnterActionType.RETURN
)

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
    object SmartDeleteWord : KeyAction()
    object Enter : KeyAction()
    object Shift : KeyAction()
    object SwitchToSymbols : KeyAction()
    object SwitchToLetters : KeyAction()
    object SwitchToMoreSymbols : KeyAction()
    object SwitchToPreviousSymbols : KeyAction()
    object Space : KeyAction()
    object OpenEmoji : KeyAction()
    object OpenGridMenu : KeyAction()
    object OpenVoiceInput : KeyAction()
    object OpenTranslate : KeyAction()
    object OpenNicknameFont : KeyAction()
    object SwitchIme : KeyAction()
}

data class KeyItem(
    val primary: String,
    val secondary: String? = null,
    val weight: Float = 1f,
    val action: KeyAction = KeyAction.CommitText(primary),
    val isActionKey: Boolean = false,
    val row: Int = 0,
    val col: Int = 0
)

data class AuroraWave(
    val originRow: Int,
    val originCol: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class KeyPopupState(
    val char: String = "",
    val row: Int = 0,
    val col: Int = 0,
    val isVisible: Boolean = false
)

data class ClipboardItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
