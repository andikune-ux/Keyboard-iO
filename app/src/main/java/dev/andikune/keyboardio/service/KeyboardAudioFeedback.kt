package dev.andikune.keyboardio.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import dev.andikune.keyboardio.model.SoundEffectType

class KeyboardAudioFeedback(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var toneGeneratorSystem: ToneGenerator? = null
    private var toneGeneratorMusic: ToneGenerator? = null

    init {
        try {
            toneGeneratorSystem = ToneGenerator(AudioManager.STREAM_SYSTEM, 75)
        } catch (_: Exception) {}
        try {
            toneGeneratorMusic = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        } catch (_: Exception) {}
    }

    fun playSound(soundType: SoundEffectType) {
        try {
            when (soundType) {
                SoundEffectType.STANDARD -> {
                    audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
                }
                SoundEffectType.MECHANICAL -> {
                    // Tactile mechanical switch click
                    audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1.0f)
                    // Short low crisp thock tone for real mechanical feel
                    val tone = toneGeneratorMusic ?: toneGeneratorSystem
                    tone?.startTone(ToneGenerator.TONE_PROP_PROMPT, 18)
                }
                SoundEffectType.TYPEWRITER -> {
                    // Sharp typewriter keypress strike
                    audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 1.0f)
                    val tone = toneGeneratorMusic ?: toneGeneratorSystem
                    tone?.startTone(ToneGenerator.TONE_PROP_ACK, 22)
                }
                SoundEffectType.DIGITAL_POP -> {
                    // Futuristic cyber pop bubble sound
                    val tone = toneGeneratorMusic ?: toneGeneratorSystem
                    tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 24)
                }
            }
        } catch (_: Exception) {
            fallbackClick()
        }
    }

    private fun fallbackClick() {
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK)
        } catch (_: Exception) {
            try {
                (toneGeneratorMusic ?: toneGeneratorSystem)?.startTone(ToneGenerator.TONE_PROP_BEEP, 20)
            } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            toneGeneratorSystem?.release()
            toneGeneratorSystem = null
        } catch (_: Exception) {}
        try {
            toneGeneratorMusic?.release()
            toneGeneratorMusic = null
        } catch (_: Exception) {}
    }
}
