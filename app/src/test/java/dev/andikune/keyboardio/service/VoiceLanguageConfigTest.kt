package dev.andikune.keyboardio.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class VoiceLanguageConfigTest {
    @Test fun systemDefaultFollowsDeviceLocale() {
        assertEquals("id-ID", VoiceLanguageConfig.resolve("SYSTEM", Locale.forLanguageTag("id-ID")))
        assertEquals("en-US", VoiceLanguageConfig.resolve("SYSTEM", Locale.forLanguageTag("en-US")))
    }
    @Test fun explicitLanguageOverridesDeviceLocale() {
        assertEquals("id-ID", VoiceLanguageConfig.resolve("id-ID", Locale.US))
        assertEquals("en-US", VoiceLanguageConfig.resolve("en-US", Locale.forLanguageTag("id-ID")))
    }
    @Test fun invalidLanguageFallsBackToIndonesian() {
        assertEquals("id-ID", VoiceLanguageConfig.resolve("not a locale", Locale.US))
    }
}
