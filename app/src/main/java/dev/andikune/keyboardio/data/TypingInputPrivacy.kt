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
