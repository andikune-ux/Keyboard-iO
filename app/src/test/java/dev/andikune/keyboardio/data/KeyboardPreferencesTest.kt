package dev.andikune.keyboardio.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class KeyboardPreferencesTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("keyboard_io_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun clipboardPrivateModeDefaultsOffAndPersistsChanges() {
        val preferences = KeyboardPreferences(context)
        assertEquals(false, preferences.clipboardPrivateMode.value)
        preferences.setClipboardPrivateMode(true)
        assertEquals(true, preferences.clipboardPrivateMode.value)
        assertEquals(true, KeyboardPreferences(context).clipboardPrivateMode.value)
        preferences.setClipboardPrivateMode(false)
        assertEquals(false, KeyboardPreferences(context).clipboardPrivateMode.value)
    }

    @Test
    fun clipboardPrivateModeChangesAreObservedAcrossPreferencesInstances() {
        val imePreferences = KeyboardPreferences(context)
        val settingsPreferences = KeyboardPreferences(context)

        settingsPreferences.setClipboardPrivateMode(true)
        assertEquals(true, imePreferences.clipboardPrivateMode.value)

        settingsPreferences.setClipboardPrivateMode(false)
        assertEquals(false, imePreferences.clipboardPrivateMode.value)
    }

    @Test
    fun voiceLanguageDefaultsToSystemAndPersistsExplicitChoice() {
        val preferences = KeyboardPreferences(context)
        assertEquals("SYSTEM", preferences.voiceInputLanguage.value)
        preferences.setVoiceInputLanguage("id-ID")
        assertEquals("id-ID", KeyboardPreferences(context).voiceInputLanguage.value)
        preferences.setVoiceInputLanguage("en-US")
        assertEquals("en-US", KeyboardPreferences(context).voiceInputLanguage.value)
    }

    @Test
    fun invalidVoiceLanguageFallsBackToSystem() {
        val preferences = KeyboardPreferences(context)
        preferences.setVoiceInputLanguage("xx-invalid")
        assertEquals("SYSTEM", preferences.voiceInputLanguage.value)
    }

    @Test
    fun newInstallUsesVisibleWallpaperDefaults() {
        val preferences = KeyboardPreferences(context)
        assertEquals(1f, preferences.customBgOpacity.value, 0.001f)
        assertEquals(0f, preferences.customBgDim.value, 0.001f)
    }

    @Test
    fun migratesPreviousDarkDefaultsToReadableDefaults() {
        context.getSharedPreferences("keyboard_io_prefs", Context.MODE_PRIVATE)
            .edit()
            .putFloat("custom_bg_opacity", 0.45f)
            .putFloat("custom_bg_dim", 0.5f)
            .commit()

        val preferences = KeyboardPreferences(context)
        assertEquals(1f, preferences.customBgOpacity.value, 0.001f)
        assertEquals(0f, preferences.customBgDim.value, 0.001f)
    }

    @Test
    fun preservesPreviouslyCustomizedWallpaperValues() {
        context.getSharedPreferences("keyboard_io_prefs", Context.MODE_PRIVATE)
            .edit()
            .putFloat("custom_bg_opacity", 0.7f)
            .putFloat("custom_bg_dim", 0.3f)
            .commit()

        val preferences = KeyboardPreferences(context)
        assertEquals(0.7f, preferences.customBgOpacity.value, 0.001f)
        assertEquals(0.3f, preferences.customBgDim.value, 0.001f)
    }

    @Test
    fun clampsAndPersistsDimValue() {
        val preferences = KeyboardPreferences(context)
        preferences.setCustomBgDim(0.95f)

        assertEquals(0.75f, preferences.customBgDim.value, 0.001f)
        val restored = KeyboardPreferences(context)
        assertEquals(0.75f, restored.customBgDim.value, 0.001f)
    }
    @Test
    fun allowsFullyTransparentWallpaperAndPersistsIt() {
        val preferences = KeyboardPreferences(context)
        preferences.setCustomBgOpacity(0f)

        assertEquals(0f, preferences.customBgOpacity.value, 0.001f)
        assertEquals(0f, KeyboardPreferences(context).customBgOpacity.value, 0.001f)
    }

    @Test
    fun preservesVersionTwoWallpaperValuesThatMayBeUserCustomized() {
        context.getSharedPreferences("keyboard_io_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("custom_bg_settings_version", 2)
            .putFloat("custom_bg_opacity", 0.92f)
            .putFloat("custom_bg_dim", 0.10f)
            .commit()

        val preferences = KeyboardPreferences(context)
        assertEquals(0.92f, preferences.customBgOpacity.value, 0.001f)
        assertEquals(0.10f, preferences.customBgDim.value, 0.001f)
    }

    @Test
    fun wallpaperChangesFromAnotherPreferencesInstanceAreObserved() {
        val keyboardPreferences = KeyboardPreferences(context)
        val settingsPreferences = KeyboardPreferences(context)

        settingsPreferences.setCustomBackground(
            type = "IMAGE",
            path = "/tmp/new-wallpaper.jpg",
            opacity = 0.65f,
            dim = 0.2f
        )

        assertEquals("IMAGE", keyboardPreferences.customBgType.value)
        assertEquals("/tmp/new-wallpaper.jpg", keyboardPreferences.customBgPath.value)
        assertEquals(0.65f, keyboardPreferences.customBgOpacity.value, 0.001f)
        assertEquals(0.2f, keyboardPreferences.customBgDim.value, 0.001f)
    }

    @Test
    fun newBackgroundKeepsFullyTransparentOpacityInStateAndStorage() {
        val preferences = KeyboardPreferences(context)
        preferences.setCustomBackground(type = "IMAGE", path = "/tmp/wallpaper.jpg", opacity = 0f)

        assertEquals(0f, preferences.customBgOpacity.value, 0.001f)
        assertEquals(0f, KeyboardPreferences(context).customBgOpacity.value, 0.001f)
    }

    @Test
    fun keyboardHeightScaleNeverShrinksBelowFortyEightDp() {
        val preferences = KeyboardPreferences(context)
        preferences.setHeightScale(0.75f)
        assertEquals(1.0f, preferences.heightScale.value, 0.001f)
        assertEquals(1.0f, KeyboardPreferences(context).heightScale.value, 0.001f)
    }

    @Test
    fun keyboardHeightScaleClampsToMaximumSupportedValue() {
        val preferences = KeyboardPreferences(context)
        preferences.setHeightScale(2.0f)
        assertEquals(1.35f, preferences.heightScale.value, 0.001f)
        assertEquals(1.35f, KeyboardPreferences(context).heightScale.value, 0.001f)
    }

}
