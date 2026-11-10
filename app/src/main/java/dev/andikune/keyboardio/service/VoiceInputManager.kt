package dev.andikune.keyboardio.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Guards asynchronous recognizer callbacks. Every start/cancel invalidates the
 * previous generation so late callbacks cannot mutate a newer voice session.
 */
internal class VoiceCallbackGeneration {
    private var generation = 0L

    @Synchronized
    fun begin(): Long {
        generation += 1
        return generation
    }

    @Synchronized
    fun invalidate() {
        generation += 1
    }

    @Synchronized
    fun isCurrent(token: Long): Boolean = token == generation
}

class VoiceInputManager(
    private val context: Context,
    private val onResult: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val callbackGeneration = VoiceCallbackGeneration()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    private val _rmsVolume = MutableStateFlow(0f)
    val rmsVolume: StateFlow<Float> = _rmsVolume.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        cancelListening()
        if (!isAvailable()) {
            _errorMessage.value = "Pengenalan suara tidak didukung pada perangkat ini"
            return
        }

        val token = callbackGeneration.begin()
        _errorMessage.value = null
        _liveSpokenText.value = ""
        _rmsVolume.value = 0f

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                private fun isActive(): Boolean =
                    callbackGeneration.isCurrent(token) && speechRecognizer === recognizer

                override fun onReadyForSpeech(params: Bundle?) {
                    if (!isActive()) return
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    if (!isActive()) return
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (!isActive()) return
                    _rmsVolume.value = (rmsdB / 10f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    if (!isActive()) return
                }

                override fun onEndOfSpeech() {
                    if (!isActive()) return
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    if (!isActive()) return
                    _isListening.value = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara terdeteksi"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis"
                        SpeechRecognizer.ERROR_AUDIO -> "Kesalahan perekaman audio"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon diperlukan"
                        else -> "Gagal mendeteksi suara"
                    }
                    _errorMessage.value = message
                    releaseRecognizer(recognizer, token)
                }

                override fun onResults(results: Bundle?) {
                    if (!isActive()) return
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) {
                        _liveSpokenText.value = text
                        onResult(text)
                    }
                    releaseRecognizer(recognizer, token)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (!isActive()) return
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { _liveSpokenText.value = it }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    if (!isActive()) return
                }
            })

            val deviceLanguage = Locale.getDefault().toLanguageTag().ifBlank { "id-ID" }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, deviceLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, deviceLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
            if (callbackGeneration.isCurrent(token) && speechRecognizer === recognizer) {
                _isListening.value = true
            }
        } catch (exception: Exception) {
            if (callbackGeneration.isCurrent(token)) {
                _errorMessage.value = "Gagal memulai mikrofon: ${exception.message}"
                releaseRecognizer(speechRecognizer, token)
            }
        }
    }

    /** Stop capture but keep this generation valid for its final onResults callback. */
    fun stopListening() {
        val recognizer = speechRecognizer ?: return
        try {
            recognizer.stopListening()
        } catch (_: Exception) {
            cancelListening()
            return
        }
        _isListening.value = false
    }

    /** Cancel invalidates callbacks before destroying the recognizer. */
    fun cancelListening() {
        callbackGeneration.invalidate()
        val recognizer = speechRecognizer
        speechRecognizer = null
        _isListening.value = false
        _rmsVolume.value = 0f
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }
    }

    private fun releaseRecognizer(recognizer: SpeechRecognizer?, token: Long) {
        if (recognizer == null || !callbackGeneration.isCurrent(token) || speechRecognizer !== recognizer) return
        speechRecognizer = null
        _isListening.value = false
        _rmsVolume.value = 0f
        try {
            recognizer.destroy()
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        cancelListening()
    }
}
