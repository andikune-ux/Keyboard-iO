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

class VoiceInputManager(
    private val context: Context,
    private val onResult: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

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
        if (!isAvailable()) {
            _errorMessage.value = "Pengenalan suara tidak didukung pada perangkat ini"
            return
        }

        cancelListening()
        _errorMessage.value = null
        _liveSpokenText.value = ""

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsVolume.value = (rmsdB / 10f).coerceIn(0f, 1f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara terdeteksi"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis"
                            SpeechRecognizer.ERROR_AUDIO -> "Kesalahan perekaman audio"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon diperlukan"
                            else -> "Gagal mendeteksi suara"
                        }
                        _errorMessage.value = msg
                        releaseRecognizer()
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            _liveSpokenText.value = text
                            onResult(text)
                        }
                        releaseRecognizer()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _liveSpokenText.value = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val deviceLanguage = Locale.getDefault().toLanguageTag().ifBlank { "id-ID" }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, deviceLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, deviceLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            releaseRecognizer()
            _errorMessage.value = "Gagal memulai mikrofon: ${e.message}"
        }
    }

    /** Stop capture and allow SpeechRecognizer to deliver its final onResults callback. */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
            releaseRecognizer()
        }
        _isListening.value = false
    }

    /** Cancel recognition when the user dismisses the voice panel or starts a new session. */
    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {
        }
        releaseRecognizer()
    }

    private fun releaseRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        _isListening.value = false
        _rmsVolume.value = 0f
    }

    fun destroy() {
        cancelListening()
    }
}
