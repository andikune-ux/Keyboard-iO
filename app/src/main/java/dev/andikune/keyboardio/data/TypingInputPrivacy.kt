package dev.andikune.keyboardio.data

import android.text.InputType

/**
 * Identifies editor input types whose contents must never enter typing history.
 * Ordinary numeric fields remain eligible; only the explicit numeric password
 * variation is treated as sensitive.
 */
object TypingInputPrivacy {
    fun isPasswordInputType(inputType: Int): Boolean {
        val classType = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            (classType == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }
}

/** Clipboard capture is disabled for password editors and while user privacy mode is on. */
object ClipboardCapturePolicy {
    fun shouldCapture(isPasswordInput: Boolean, privateModeEnabled: Boolean): Boolean =
        !isPasswordInput && !privateModeEnabled
}

/** Separates showing useful suggestions from silently changing text in the editor. */
data class EditorInputCapabilities(
    val suggestionsAllowed: Boolean,
    val autoCorrectionAllowed: Boolean
)

object EditorInputTypePolicy {
    fun resolve(inputType: Int): EditorInputCapabilities {
        val isText = inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val unsupportedVariation = variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_URI ||
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        val suggestionsAllowed = isText &&
            !unsupportedVariation &&
            !TypingInputPrivacy.isPasswordInputType(inputType) &&
            (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0
        val autoCorrectionAllowed = suggestionsAllowed &&
            (inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT) != 0
        return EditorInputCapabilities(suggestionsAllowed, autoCorrectionAllowed)
    }
}
