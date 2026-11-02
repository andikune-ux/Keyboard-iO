package dev.andikune.keyboardio.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import dev.andikune.keyboardio.model.SoundEffectType

class KeyboardAudioFeedback(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 60)
        } catch (_: Exception) {
        }
    }

    fun playSound(soundType: SoundEffectType) {
        try {
            when (soundType) {
                SoundEffectType.STANDARD -> {
                    audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK)
                }
                SoundEffectType.MECHANICAL -> {
                    // Deep crisp feedback click
                    audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1.0f)
                }
                SoundEffectType.TYPEWRITER -> {
                    // Classic sharp typewriter strike
                    audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 1.0f)
                }
                SoundEffectType.DIGITAL_POP -> {
                    // Futuristic digital pop tone
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
                }
            }
        } catch (_: Exception) {
            // Fallback
            try {
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK)
            } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
