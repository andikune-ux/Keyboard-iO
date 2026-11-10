package dev.andikune.keyboardio.model

object KeyboardLayouts {

    fun getQwertyRow1(isUpper: Boolean): List<KeyItem> {
        val letters = if (isUpper) {
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
        } else {
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        }
        val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        return letters.mapIndexed { col, char ->
            KeyItem(
                primary = char,
                secondary = digits.getOrNull(col),
                action = KeyAction.CommitText(char),
                row = 0,
                col = col
            )
        }
    }

    fun getQwertyRow2(isUpper: Boolean): List<KeyItem> {
        val letters = if (isUpper) {
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
        } else {
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        }
        val symbols = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")")
        return letters.mapIndexed { col, char ->
            KeyItem(
                primary = char,
                secondary = symbols.getOrNull(col),
                action = KeyAction.CommitText(char),
                row = 1,
                col = col
            )
        }
    }

    fun getQwertyRow3(isUpper: Boolean): List<KeyItem> {
        val letters = if (isUpper) {
            listOf("Z", "X", "C", "V", "B", "N", "M")
        } else {
            listOf("z", "x", "c", "v", "b", "n", "m")
        }
        val symbols = listOf("*", "\"", "'", ":", ";", "!", "?")

        val list = mutableListOf<KeyItem>()
        // Shift key
        list.add(
            KeyItem(
                primary = "⇧",
                weight = 1.5f,
                action = KeyAction.Shift,
                isActionKey = true,
                row = 2,
                col = 0
            )
        )
        // Letter keys
        letters.forEachIndexed { idx, char ->
            list.add(
                KeyItem(
                    primary = char,
                    secondary = symbols.getOrNull(idx),
                    action = KeyAction.CommitText(char),
                    row = 2,
                    col = idx + 1
                )
            )
        }
        // Backspace key
        list.add(
            KeyItem(
                primary = "⌫",
                weight = 1.5f,
                action = KeyAction.Backspace,
                isActionKey = true,
                row = 2,
                col = letters.size + 1
            )
        )
        return list
    }

    /**
     * Bottom row for QWERTY:
     * - ?123: 1.4f
     * - Comma: 1.0f
     * - Space: 5.0f
     * - Period: 1.0f
     * - Enter: 1.6f
     * Total = 10.0f matching Rows 1, 2, and 3
     */
    fun getQwertyRow4(enterLabel: String = "↵", spacebarLabel: String = "␣"): List<KeyItem> {
        return listOf(
            KeyItem(
                primary = "?123",
                weight = 1.4f,
                action = KeyAction.SwitchToSymbols,
                isActionKey = true,
                row = 3,
                col = 0
            ),
            KeyItem(
                primary = ",",
                secondary = ";",
                weight = 1.0f,
                action = KeyAction.CommitText(","),
                row = 3,
                col = 1
            ),
            KeyItem(
                primary = spacebarLabel,
                secondary = null,
                weight = 5.0f,
                action = KeyAction.Space,
                row = 3,
                col = 2
            ),
            KeyItem(
                primary = ".",
                secondary = "?",
                weight = 1.0f,
                action = KeyAction.CommitText("."),
                row = 3,
                col = 3
            ),
            KeyItem(
                primary = enterLabel,
                weight = 1.6f,
                action = KeyAction.Enter,
                isActionKey = true,
                row = 3,
                col = 4
            )
        )
    }

    // Number & Symbols layout
    fun getSymbolsRow1(): List<KeyItem> {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        return keys.mapIndexed { col, it ->
            KeyItem(primary = it, action = KeyAction.CommitText(it), row = 0, col = col)
        }
    }

    fun getSymbolsRow2(): List<KeyItem> {
        val keys = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
        return keys.mapIndexed { col, it ->
            KeyItem(primary = it, action = KeyAction.CommitText(it), row = 1, col = col)
        }
    }

    fun getSymbolsRow3(): List<KeyItem> {
        val list = mutableListOf<KeyItem>()
        list.add(
            KeyItem(
                primary = "=\\<",
                weight = 1.5f,
                action = KeyAction.SwitchToMoreSymbols,
                isActionKey = true,
                row = 2,
                col = 0
            )
        )
        val symbols = listOf("*", "\"", "'", ":", ";", "!", "?")
        symbols.forEachIndexed { idx, it ->
            list.add(KeyItem(primary = it, action = KeyAction.CommitText(it), row = 2, col = idx + 1))
        }
        list.add(
            KeyItem(
                primary = "⌫",
                weight = 1.5f,
                action = KeyAction.Backspace,
                isActionKey = true,
                row = 2,
                col = symbols.size + 1
            )
        )
        return list
    }

    fun getSymbolsRow4(enterLabel: String = "↵", spacebarLabel: String = "␣"): List<KeyItem> {
        return listOf(
            KeyItem(
                primary = "ABC",
                weight = 1.4f,
                action = KeyAction.SwitchToLetters,
                isActionKey = true,
                row = 3,
                col = 0
            ),
            KeyItem(
                primary = ",",
                weight = 1.0f,
                action = KeyAction.CommitText(","),
                row = 3,
                col = 1
            ),
            KeyItem(
                primary = spacebarLabel,
                weight = 5.0f,
                action = KeyAction.Space,
                row = 3,
                col = 2
            ),
            KeyItem(
                primary = ".",
                weight = 1.0f,
                action = KeyAction.CommitText("."),
                row = 3,
                col = 3
            ),
            KeyItem(
                primary = enterLabel,
                weight = 1.6f,
                action = KeyAction.Enter,
                isActionKey = true,
                row = 3,
                col = 4
            )
        )
    }

    // More symbols layout
    fun getMoreSymbolsRow1(): List<KeyItem> {
        val keys = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
        return keys.mapIndexed { col, it ->
            KeyItem(primary = it, action = KeyAction.CommitText(it), row = 0, col = col)
        }
    }

    fun getMoreSymbolsRow2(): List<KeyItem> {
        val keys = listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
        return keys.mapIndexed { col, it ->
            KeyItem(primary = it, action = KeyAction.CommitText(it), row = 1, col = col)
        }
    }

    fun getMoreSymbolsRow3(): List<KeyItem> {
        val list = mutableListOf<KeyItem>()
        list.add(
            KeyItem(
                primary = "?123",
                weight = 1.4f,
                action = KeyAction.SwitchToSymbols,
                isActionKey = true,
                row = 2,
                col = 0
            )
        )
        val symbols = listOf("%", "©", "®", "™", "[", "]", "<", ">")
        symbols.forEachIndexed { idx, it ->
            list.add(KeyItem(primary = it, action = KeyAction.CommitText(it), row = 2, col = idx + 1))
        }
        list.add(
            KeyItem(
                primary = "⌫",
                weight = 1.4f,
                action = KeyAction.Backspace,
                isActionKey = true,
                row = 2,
                col = symbols.size + 1
            )
        )
        return list
    }

    fun getMoreSymbolsRow4(enterLabel: String = "↵", spacebarLabel: String = "␣"): List<KeyItem> {
        return listOf(
            KeyItem(
                primary = "ABC",
                weight = 1.4f,
                action = KeyAction.SwitchToLetters,
                isActionKey = true,
                row = 3,
                col = 0
            ),
            KeyItem(
                primary = ",",
                weight = 1.0f,
                action = KeyAction.CommitText(","),
                row = 3,
                col = 1
            ),
            KeyItem(
                primary = spacebarLabel,
                weight = 5.0f,
                action = KeyAction.Space,
                row = 3,
                col = 2
            ),
            KeyItem(
                primary = "…",
                weight = 1.0f,
                action = KeyAction.CommitText("…"),
                row = 3,
                col = 3
            ),
            KeyItem(
                primary = enterLabel,
                weight = 1.6f,
                action = KeyAction.Enter,
                isActionKey = true,
                row = 3,
                col = 4
            )
        )
    }

    /**
     * Resolve layout-only actions against the active editor. Numeric editors
     * must never transition into a letter layout, even if an ABC key is tapped.
     */
    fun nextMode(
        currentMode: KeyboardLayoutMode,
        action: KeyAction,
        isNumberOnly: Boolean
    ): KeyboardLayoutMode? {
        return when (action) {
            is KeyAction.Shift -> if (isNumberOnly) null else when (currentMode) {
                KeyboardLayoutMode.QWERTY_LOWER -> KeyboardLayoutMode.QWERTY_UPPER
                KeyboardLayoutMode.QWERTY_UPPER -> KeyboardLayoutMode.CAPS_LOCK
                KeyboardLayoutMode.CAPS_LOCK -> KeyboardLayoutMode.QWERTY_LOWER
                else -> KeyboardLayoutMode.QWERTY_LOWER
            }
            is KeyAction.SwitchToSymbols -> KeyboardLayoutMode.NUMBERS_SYMBOLS
            is KeyAction.SwitchToMoreSymbols -> KeyboardLayoutMode.MORE_SYMBOLS
            is KeyAction.SwitchToLetters -> if (isNumberOnly) null else KeyboardLayoutMode.QWERTY_LOWER
            else -> null
        }
    }

}
