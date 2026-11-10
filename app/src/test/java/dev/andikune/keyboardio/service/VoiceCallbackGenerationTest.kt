package dev.andikune.keyboardio.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCallbackGenerationTest {
    @Test fun newSessionInvalidatesCallbacksFromPreviousSession() {
        val guard = VoiceCallbackGeneration()
        val first = guard.begin()
        val second = guard.begin()

        assertFalse(guard.isCurrent(first))
        assertTrue(guard.isCurrent(second))
    }

    @Test fun cancelInvalidatesCurrentCallbacks() {
        val guard = VoiceCallbackGeneration()
        val token = guard.begin()

        guard.invalidate()

        assertFalse(guard.isCurrent(token))
    }

    @Test fun callbackFromEarlierSessionCannotBecomeCurrentAgain() {
        val guard = VoiceCallbackGeneration()
        val first = guard.begin()
        guard.invalidate()
        val second = guard.begin()
        guard.invalidate()
        val third = guard.begin()

        assertFalse(guard.isCurrent(first))
        assertFalse(guard.isCurrent(second))
        assertTrue(guard.isCurrent(third))
    }
}
