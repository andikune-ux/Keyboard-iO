package dev.andikune.keyboardio.data

/** Central gate for conservative autocorrection before a word is committed. */
object EditorAutoCorrectionPolicy {
    fun shouldCorrect(
        isEnabled: Boolean,
        editorAllowsCorrection: Boolean,
        wasManuallyEdited: Boolean,
        contextIsReliable: Boolean,
        hasSelection: Boolean,
        cursorAtWordEnd: Boolean,
        word: String
    ): Boolean =
        isEnabled &&
            editorAllowsCorrection &&
            !wasManuallyEdited &&
            contextIsReliable &&
            !hasSelection &&
            cursorAtWordEnd &&
            word.length >= 3
}
