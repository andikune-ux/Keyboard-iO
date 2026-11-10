package dev.andikune.keyboardio.data

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInputTypePolicyTest {
    @Test fun plainTextAllowsSuggestionsButDoesNotAssumeAutocorrection() {
        val result = EditorInputTypePolicy.resolve(InputType.TYPE_CLASS_TEXT)
        assertTrue(result.suggestionsAllowed)
        assertFalse(result.autoCorrectionAllowed)
    }

    @Test fun autoCorrectFlagEnablesAutocorrection() {
        val result = EditorInputTypePolicy.resolve(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        )
        assertTrue(result.suggestionsAllowed)
        assertTrue(result.autoCorrectionAllowed)
    }

    @Test fun noSuggestionsDisablesSuggestionsAndAutocorrection() {
        val result = EditorInputTypePolicy.resolve(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        )
        assertFalse(result.suggestionsAllowed)
        assertFalse(result.autoCorrectionAllowed)
    }

    @Test fun emailUriAndPasswordFieldsDisableSuggestions() {
        val variations = listOf(
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )
        variations.forEach { variation ->
            val result = EditorInputTypePolicy.resolve(
                InputType.TYPE_CLASS_TEXT or variation or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            )
            assertFalse(result.suggestionsAllowed)
            assertFalse(result.autoCorrectionAllowed)
        }
    }

    @Test fun nonTextFieldsNeverEnableTextSuggestions() {
        val result = EditorInputTypePolicy.resolve(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        )
        assertFalse(result.suggestionsAllowed)
        assertFalse(result.autoCorrectionAllowed)
    }
}
