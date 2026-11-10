package dev.andikune.keyboardio.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardCapturePolicyTest {
    @Test fun allowsCaptureWhenEditorIsNormalAndPrivateModeIsOff() {
        assertTrue(ClipboardCapturePolicy.shouldCapture(false, false))
    }

    @Test fun blocksCaptureForPasswordFields() {
        assertFalse(ClipboardCapturePolicy.shouldCapture(true, false))
    }

    @Test fun blocksCaptureWhenPrivateModeIsEnabled() {
        assertFalse(ClipboardCapturePolicy.shouldCapture(false, true))
    }

    @Test fun blocksCaptureWhenPasswordAndPrivateModeAreBothActive() {
        assertFalse(ClipboardCapturePolicy.shouldCapture(true, true))
    }
}
