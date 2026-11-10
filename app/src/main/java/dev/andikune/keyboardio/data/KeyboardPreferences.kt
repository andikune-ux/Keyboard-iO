package dev.andikune.keyboardio.data

import android.content.Context
import android.content.SharedPreferences
import dev.andikune.keyboardio.model.OneHandedMode
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.model.SoundEffectType
import dev.andikune.keyboardio.model.SpacebarStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeyboardPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("keyboard_io_prefs", Context.MODE_PRIVATE)

    init {
        val settingsVersion = prefs.getInt("custom_bg_settings_version", 0)
        val editor = prefs.edit()
        if (settingsVersion < 2) {
            if (prefs.getFloat("custom_bg_opacity", 0.45f) == 0.45f) {
                editor.putFloat("custom_bg_opacity", 0.92f)
            }
            if (prefs.getFloat("custom_bg_dim", 0.5f) == 0.5f) {
                editor.putFloat("custom_bg_dim", 0.10f)
            }
        }
        if (settingsVersion < 3) {
            // Only migrate the legacy defaults when their origin is unambiguous.
            // Version 2 may contain a user's intentional 0.92 / 0.10 customization,
            // so preserve that pair rather than silently overwriting user settings.
            val hasKnownLegacyDefaults =
                settingsVersion < 2 &&
                    prefs.getFloat("custom_bg_opacity", 0.45f) == 0.45f &&
                    prefs.getFloat("custom_bg_dim", 0.5f) == 0.5f
            if (hasKnownLegacyDefaults) {
                editor.putFloat("custom_bg_opacity", 1f)
                editor.putFloat("custom_bg_dim", 0f)
            }
        }
        editor.putInt("custom_bg_settings_version", 3).apply()
    }

    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<RgbThemeStyle> = _theme.asStateFlow()

    private val _heightScale = MutableStateFlow(loadHeightScale())
    val heightScale: StateFlow<Float> = _heightScale.asStateFlow()

    private val _oneHandedMode = MutableStateFlow(loadOneHandedMode())
    val oneHandedMode: StateFlow<OneHandedMode> = _oneHandedMode.asStateFlow()

    private val _isFloating = MutableStateFlow(loadIsFloating())
    val isFloating: StateFlow<Boolean> = _isFloating.asStateFlow()

    private val _hapticFeedback = MutableStateFlow(loadHaptic())
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    private val _keySound = MutableStateFlow(loadSound())
    val keySound: StateFlow<Boolean> = _keySound.asStateFlow()

    private val _soundType = MutableStateFlow(loadSoundType())
    val soundType: StateFlow<SoundEffectType> = _soundType.asStateFlow()

    private val _longPressDelay = MutableStateFlow(loadLongPressDelay())
    val longPressDelay: StateFlow<Long> = _longPressDelay.asStateFlow()

    private val _smartBackspace = MutableStateFlow(loadSmartBackspace())
    val smartBackspace: StateFlow<Boolean> = _smartBackspace.asStateFlow()

    private val _autoCapitalize = MutableStateFlow(loadAutoCapitalize())
    val autoCapitalize: StateFlow<Boolean> = _autoCapitalize.asStateFlow()

    private val _spacebarStyle = MutableStateFlow(loadSpacebarStyle())
    val spacebarStyle: StateFlow<SpacebarStyle> = _spacebarStyle.asStateFlow()

    private val _voiceInputLanguage = MutableStateFlow(loadVoiceInputLanguage())
    val voiceInputLanguage: StateFlow<String> = _voiceInputLanguage.asStateFlow()

    private val _autoCorrection = MutableStateFlow(loadAutoCorrection())
    val autoCorrection: StateFlow<Boolean> = _autoCorrection.asStateFlow()

    private val _swipeTyping = MutableStateFlow(loadSwipeTyping())
    val swipeTyping: StateFlow<Boolean> = _swipeTyping.asStateFlow()

    private val _auroraWaveEffect = MutableStateFlow(loadAuroraWave())
    val auroraWaveEffect: StateFlow<Boolean> = _auroraWaveEffect.asStateFlow()

    private val _keyPopupEffect = MutableStateFlow(loadKeyPopup())
    val keyPopupEffect: StateFlow<Boolean> = _keyPopupEffect.asStateFlow()

    private val _clipboardPrivateMode = MutableStateFlow(loadClipboardPrivateMode())
    val clipboardPrivateMode: StateFlow<Boolean> = _clipboardPrivateMode.asStateFlow()

    fun setTheme(theme: RgbThemeStyle) {
        prefs.edit().putString("rgb_theme", theme.id).apply()
        _theme.value = theme
    }

    private fun loadTheme(): RgbThemeStyle {
        val id = prefs.getString("rgb_theme", RgbThemeStyle.RAINBOW.id)
        return RgbThemeStyle.entries.find { it.id == id } ?: RgbThemeStyle.RAINBOW
    }

    fun setHeightScale(scale: Float) {
        val clamped = scale.coerceIn(1.0f, 1.35f)
        prefs.edit().putFloat("height_scale", clamped).apply()
        _heightScale.value = clamped
    }

    private fun loadHeightScale(): Float = prefs.getFloat("height_scale", 1.0f).coerceIn(1.0f, 1.35f)

    fun setOneHandedMode(mode: OneHandedMode) {
        prefs.edit().putString("one_handed_mode", mode.name).apply()
        _oneHandedMode.value = mode
    }

    private fun loadOneHandedMode(): OneHandedMode {
        val raw = prefs.getString("one_handed_mode", OneHandedMode.OFF.name)
        return try {
            OneHandedMode.valueOf(raw ?: OneHandedMode.OFF.name)
        } catch (_: Exception) {
            OneHandedMode.OFF
        }
    }

    fun setFloating(floating: Boolean) {
        prefs.edit().putBoolean("is_floating", floating).apply()
        _isFloating.value = floating
    }

    private fun loadIsFloating(): Boolean = prefs.getBoolean("is_floating", false)

    fun setHaptic(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
        _hapticFeedback.value = enabled
    }

    private fun loadHaptic(): Boolean = prefs.getBoolean("haptic_feedback", true)

    fun setSound(enabled: Boolean) {
        prefs.edit().putBoolean("key_sound", enabled).apply()
        _keySound.value = enabled
    }

    private fun loadSound(): Boolean = prefs.getBoolean("key_sound", false)

    fun setSoundType(type: SoundEffectType) {
        prefs.edit().putString("sound_type", type.id).apply()
        _soundType.value = type
    }

    private fun loadSoundType(): SoundEffectType {
        val raw = prefs.getString("sound_type", SoundEffectType.MECHANICAL.id)
        return SoundEffectType.entries.find { it.id == raw } ?: SoundEffectType.MECHANICAL
    }

    fun setLongPressDelay(delayMs: Long) {
        val clamped = delayMs.coerceIn(200L, 700L)
        prefs.edit().putLong("long_press_delay", clamped).apply()
        _longPressDelay.value = clamped
    }

    private fun loadLongPressDelay(): Long = prefs.getLong("long_press_delay", 300L)

    fun setSmartBackspace(enabled: Boolean) {
        prefs.edit().putBoolean("smart_backspace", enabled).apply()
        _smartBackspace.value = enabled
    }

    private fun loadSmartBackspace(): Boolean = prefs.getBoolean("smart_backspace", true)

    fun setAutoCapitalize(enabled: Boolean) {
        prefs.edit().putBoolean("auto_capitalize", enabled).apply()
        _autoCapitalize.value = enabled
    }

    private fun loadAutoCapitalize(): Boolean = prefs.getBoolean("auto_capitalize", true)

    fun setSpacebarStyle(style: SpacebarStyle) {
        prefs.edit().putString("spacebar_style", style.id).apply()
        _spacebarStyle.value = style
    }

    private fun loadSpacebarStyle(): SpacebarStyle {
        val raw = prefs.getString("spacebar_style", SpacebarStyle.DEFAULT.id)
        return SpacebarStyle.entries.find { it.id == raw } ?: SpacebarStyle.DEFAULT
    }

    fun setVoiceInputLanguage(languageTag: String) {
        val normalized = when (languageTag) {
            "id-ID", "en-US", "SYSTEM" -> languageTag
            else -> "SYSTEM"
        }
        prefs.edit().putString("voice_input_language", normalized).apply()
        _voiceInputLanguage.value = normalized
    }

    private fun loadVoiceInputLanguage(): String = when (prefs.getString("voice_input_language", "SYSTEM")) {
        "id-ID" -> "id-ID"
        "en-US" -> "en-US"
        else -> "SYSTEM"
    }

    fun setAutoCorrection(enabled: Boolean) {
        prefs.edit().putBoolean("auto_correction", enabled).apply()
        _autoCorrection.value = enabled
    }

    private fun loadAutoCorrection(): Boolean = prefs.getBoolean("auto_correction", true)

    /** Read persisted value directly so an IME instance observes changes made in Settings. */
    fun isAutoCorrectionEnabled(): Boolean = prefs.getBoolean("auto_correction", true)

    fun setSwipeTyping(enabled: Boolean) {
        prefs.edit().putBoolean("swipe_typing", enabled).apply()
        _swipeTyping.value = enabled
    }

    private fun loadSwipeTyping(): Boolean = prefs.getBoolean("swipe_typing", true)

    fun setAuroraWaveEffect(enabled: Boolean) {
        prefs.edit().putBoolean("aurora_wave", enabled).apply()
        _auroraWaveEffect.value = enabled
    }

    private fun loadAuroraWave(): Boolean = prefs.getBoolean("aurora_wave", true)

    fun setKeyPopupEffect(enabled: Boolean) {
        prefs.edit().putBoolean("key_popup", enabled).apply()
        _keyPopupEffect.value = enabled
    }

    private fun loadKeyPopup(): Boolean = prefs.getBoolean("key_popup", true)

    fun setClipboardPrivateMode(enabled: Boolean) {
        prefs.edit().putBoolean("clipboard_private_mode", enabled).apply()
        _clipboardPrivateMode.value = enabled
    }

    private fun loadClipboardPrivateMode(): Boolean =
        prefs.getBoolean("clipboard_private_mode", false)

    /** Read the persisted value directly so an active IME observes Settings changes. */
    fun isClipboardPrivateModeEnabled(): Boolean =
        prefs.getBoolean("clipboard_private_mode", false)

    // Custom Background Settings (Photo / Video from Gallery)
    private val _customBgType = MutableStateFlow(loadCustomBgType())
    val customBgType: StateFlow<String> = _customBgType.asStateFlow() // "NONE", "IMAGE", "VIDEO"

    private val _customBgPath = MutableStateFlow(loadCustomBgPath())
    val customBgPath: StateFlow<String> = _customBgPath.asStateFlow()

    private val _customBgOpacity = MutableStateFlow(loadCustomBgOpacity())
    val customBgOpacity: StateFlow<Float> = _customBgOpacity.asStateFlow()

    private val _customBgDim = MutableStateFlow(loadCustomBgDim())
    val customBgDim: StateFlow<Float> = _customBgDim.asStateFlow()

    // Keep separate settings-screen and IME instances synchronized through SharedPreferences.
    private val wallpaperPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "custom_bg_type" -> _customBgType.value =
                    sharedPreferences.getString(key, "NONE") ?: "NONE"
                "custom_bg_path" -> _customBgPath.value =
                    sharedPreferences.getString(key, "") ?: ""
                "custom_bg_opacity" -> _customBgOpacity.value =
                    sharedPreferences.getFloat(key, 1f).coerceIn(0f, 1f)
                "custom_bg_dim" -> _customBgDim.value =
                    sharedPreferences.getFloat(key, 0f).coerceIn(0f, 0.75f)
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(wallpaperPreferenceListener)
    }

    fun setCustomBackground(type: String, path: String, opacity: Float = 1f, dim: Float = 0f) {
        prefs.edit()
            .putString("custom_bg_type", type)
            .putString("custom_bg_path", path)
            .putFloat("custom_bg_opacity", opacity.coerceIn(0f, 1f))
            .putFloat("custom_bg_dim", dim.coerceIn(0f, 0.75f))
            .apply()
        _customBgType.value = type
        _customBgPath.value = path
        _customBgOpacity.value = opacity.coerceIn(0f, 1f)
        _customBgDim.value = dim.coerceIn(0f, 0.75f)
    }

    fun setCustomBgOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        prefs.edit().putFloat("custom_bg_opacity", clamped).apply()
        _customBgOpacity.value = clamped
    }

    fun setCustomBgDim(dim: Float) {
        val clamped = dim.coerceIn(0f, 0.75f)
        prefs.edit().putFloat("custom_bg_dim", clamped).apply()
        _customBgDim.value = clamped
    }

    fun clearCustomBackground() {
        prefs.edit()
            .putString("custom_bg_type", "NONE")
            .putString("custom_bg_path", "")
            .apply()
        _customBgType.value = "NONE"
        _customBgPath.value = ""
    }

    private fun loadCustomBgType(): String = prefs.getString("custom_bg_type", "NONE") ?: "NONE"
    private fun loadCustomBgPath(): String = prefs.getString("custom_bg_path", "") ?: ""
    private fun loadCustomBgOpacity(): Float = prefs.getFloat("custom_bg_opacity", 1f).coerceIn(0f, 1f)
    private fun loadCustomBgDim(): Float = prefs.getFloat("custom_bg_dim", 0f).coerceIn(0f, 0.75f)
}
