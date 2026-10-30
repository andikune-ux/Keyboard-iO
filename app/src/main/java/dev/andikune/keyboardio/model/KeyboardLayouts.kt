package dev.andikune.keyboardio.model

object KeyboardLayouts {

    fun getQwertyRow1(isUpper: Boolean): List<KeyItem> {
        val letters = if (isUpper) {
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
        } else {
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        }
        val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        return letters.mapIndexed { index, char ->
            KeyItem(
                primary = char,
                secondary = digits.getOrNull(index),
                action = KeyAction.CommitText(char)
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
        return letters.mapIndexed { index, char ->
            KeyItem(
                primary = char,
                secondary = symbols.getOrNull(index),
                action = KeyAction.CommitText(char)
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
                weight = 1.4f,
                action = KeyAction.Shift,
                isActionKey = true
            )
        )
        // Letter keys
        letters.forEachIndexed { index, char ->
            list.add(
                KeyItem(
                    primary = char,
                    secondary = symbols.getOrNull(index),
                    action = KeyAction.CommitText(char)
                )
            )
        }
        // Backspace key
        list.add(
            KeyItem(
                primary = "⌫",
                weight = 1.4f,
                action = KeyAction.Backspace,
                isActionKey = true
            )
        )
        return list
    }

    fun getQwertyRow4(): List<KeyItem> {
        return listOf(
            KeyItem(
                primary = "?123",
                weight = 1.4f,
                action = KeyAction.SwitchToSymbols,
                isActionKey = true
            ),
            KeyItem(
                primary = "☺",
                weight = 1f,
                action = KeyAction.OpenEmoji,
                isActionKey = true
            ),
            KeyItem(
                primary = "spasi",
                secondary = null,
                weight = 4.2f,
                action = KeyAction.Space
            ),
            KeyItem(
                primary = ".",
                secondary = ",",
                weight = 1f,
                action = KeyAction.CommitText(".")
            ),
            KeyItem(
                primary = "↵",
                weight = 1.4f,
                action = KeyAction.Enter,
                isActionKey = true
            )
        )
    }

    // Number & Symbols layout
    fun getSymbolsRow1(): List<KeyItem> {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        return keys.map { KeyItem(primary = it, action = KeyAction.CommitText(it)) }
    }

    fun getSymbolsRow2(): List<KeyItem> {
        val keys = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
        return keys.map { KeyItem(primary = it, action = KeyAction.CommitText(it)) }
    }

    fun getSymbolsRow3(): List<KeyItem> {
        val list = mutableListOf<KeyItem>()
        list.add(
            KeyItem(
                primary = "=\\<",
                weight = 1.4f,
                action = KeyAction.SwitchToMoreSymbols,
                isActionKey = true
            )
        )
        val symbols = listOf("*", "\"", "'", ":", ";", "!", "?")
        symbols.forEach {
            list.add(KeyItem(primary = it, action = KeyAction.CommitText(it)))
        }
        list.add(
            KeyItem(
                primary = "⌫",
                weight = 1.4f,
                action = KeyAction.Backspace,
                isActionKey = true
            )
        )
        return list
    }

    fun getSymbolsRow4(): List<KeyItem> {
        return listOf(
            KeyItem(
                primary = "ABC",
                weight = 1.4f,
                action = KeyAction.SwitchToLetters,
                isActionKey = true
            ),
            KeyItem(
                primary = "☺",
                weight = 1f,
                action = KeyAction.OpenEmoji,
                isActionKey = true
            ),
            KeyItem(
                primary = "spasi",
                weight = 4.2f,
                action = KeyAction.Space
            ),
            KeyItem(
                primary = ",",
                weight = 1f,
                action = KeyAction.CommitText(",")
            ),
            KeyItem(
                primary = "↵",
                weight = 1.4f,
                action = KeyAction.Enter,
                isActionKey = true
            )
        )
    }

    // More symbols layout
    fun getMoreSymbolsRow1(): List<KeyItem> {
        val keys = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
        return keys.map { KeyItem(primary = it, action = KeyAction.CommitText(it)) }
    }

    fun getMoreSymbolsRow2(): List<KeyItem> {
        val keys = listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
        return keys.map { KeyItem(primary = it, action = KeyAction.CommitText(it)) }
    }

    fun getMoreSymbolsRow3(): List<KeyItem> {
        val list = mutableListOf<KeyItem>()
        list.add(
            KeyItem(
                primary = "?123",
                weight = 1.4f,
                action = KeyAction.SwitchToSymbols,
                isActionKey = true
            )
        )
        val symbols = listOf("%", "©", "®", "™", "[", "]", "<", ">")
        symbols.forEach {
            list.add(KeyItem(primary = it, action = KeyAction.CommitText(it)))
        }
        list.add(
            KeyItem(
                primary = "⌫",
                weight = 1.4f,
                action = KeyAction.Backspace,
                isActionKey = true
            )
        )
        return list
    }

    fun getMoreSymbolsRow4(): List<KeyItem> {
        return listOf(
            KeyItem(
                primary = "ABC",
                weight = 1.4f,
                action = KeyAction.SwitchToLetters,
                isActionKey = true
            ),
            KeyItem(
                primary = "☺",
                weight = 1f,
                action = KeyAction.OpenEmoji,
                isActionKey = true
            ),
            KeyItem(
                primary = "spasi",
                weight = 4.2f,
                action = KeyAction.Space
            ),
            KeyItem(
                primary = "…",
                weight = 1f,
                action = KeyAction.CommitText("…")
            ),
            KeyItem(
                primary = "↵",
                weight = 1.4f,
                action = KeyAction.Enter,
                isActionKey = true
            )
        )
    }
}
