package dev.andikune.keyboardio.data

import android.content.Context
import android.content.SharedPreferences
import dev.andikune.keyboardio.model.OneHandedMode
import dev.andikune.keyboardio.model.RgbThemeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeyboardPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("keyboard_io_prefs", Context.MODE_PRIVATE)

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

    private val _longPressDelay = MutableStateFlow(loadLongPressDelay())
    val longPressDelay: StateFlow<Long> = _longPressDelay.asStateFlow()

    fun setTheme(theme: RgbThemeStyle) {
        prefs.edit().putString("rgb_theme", theme.id).apply()
        _theme.value = theme
    }

    private fun loadTheme(): RgbThemeStyle {
        val id = prefs.getString("rgb_theme", RgbThemeStyle.RAINBOW.id)
        return RgbThemeStyle.entries.find { it.id == id } ?: RgbThemeStyle.RAINBOW
    }

    fun setHeightScale(scale: Float) {
        val clamped = scale.coerceIn(0.75f, 1.35f)
        prefs.edit().putFloat("height_scale", clamped).apply()
        _heightScale.value = clamped
    }

    private fun loadHeightScale(): Float = prefs.getFloat("height_scale", 1.0f)

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

    fun setLongPressDelay(delayMs: Long) {
        val clamped = delayMs.coerceIn(200L, 700L)
        prefs.edit().putLong("long_press_delay", clamped).apply()
        _longPressDelay.value = clamped
    }

    private fun loadLongPressDelay(): Long = prefs.getLong("long_press_delay", 300L)
}
