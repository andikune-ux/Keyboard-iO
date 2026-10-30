package dev.andikune.keyboardio.service

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.andikune.keyboardio.data.ClipboardRepository
import dev.andikune.keyboardio.data.KeyboardPreferences
import dev.andikune.keyboardio.model.KeyAction
import dev.andikune.keyboardio.ui.KeyboardScreen
import dev.andikune.keyboardio.ui.theme.KeyboardIOTheme
import java.util.Stack

class KeyboardInputMethodService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    lateinit var preferences: KeyboardPreferences
        private set
    lateinit var clipboardRepo: ClipboardRepository
        private set

    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null

    // Undo history stack
    private val undoStack = Stack<String>()

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkAndRecordClipboard()
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        preferences = KeyboardPreferences(applicationContext)
        clipboardRepo = ClipboardRepository(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        checkAndRecordClipboard()
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KeyboardInputMethodService)
            setViewTreeViewModelStoreOwner(this@KeyboardInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@KeyboardInputMethodService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                KeyboardIOTheme {
                    KeyboardScreen(
                        preferences = preferences,
                        clipboardRepo = clipboardRepo,
                        onKeyAction = { handleKeyAction(it) },
                        onSecondaryChar = { commitText(it) },
                        onPasteText = { commitText(it) },
                        onUndo = { performUndo() }
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        checkAndRecordClipboard()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    private fun checkAndRecordClipboard() {
        try {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString()
                if (!text.isNullOrBlank()) {
                    clipboardRepo.addClip(text)
                }
            }
        } catch (_: Exception) {
            // Some devices restrict clipboard access outside foreground
        }
    }

    private fun handleKeyAction(action: KeyAction) {
        provideFeedback()
        val ic = currentInputConnection ?: return

        when (action) {
            is KeyAction.CommitText -> {
                commitText(action.text)
            }
            is KeyAction.Backspace -> {
                val selected = ic.getSelectedText(0)
                if (selected.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
                if (undoStack.isNotEmpty()) {
                    undoStack.pop()
                }
            }
            is KeyAction.Space -> {
                commitText(" ")
            }
            is KeyAction.Enter -> {
                val editorInfo = currentInputEditorInfo
                val imeOptions = editorInfo?.imeOptions ?: 0
                val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
                if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId)
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
            is KeyAction.SwitchIme -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchToNextInputMethod(false)
                }
            }
            else -> {}
        }
    }

    private fun commitText(text: String) {
        provideFeedback()
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        undoStack.push(text)
    }

    private fun performUndo() {
        provideFeedback()
        val ic = currentInputConnection ?: return
        if (undoStack.isNotEmpty()) {
            val last = undoStack.pop()
            ic.deleteSurroundingText(last.length, 0)
        } else {
            // If no recorded stack, delete previous word
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun provideFeedback() {
        if (preferences.hapticFeedback.value) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(20L)
                }
            } catch (_: Exception) {
            }
        }

        if (preferences.keySound.value) {
            try {
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK)
            } catch (_: Exception) {
            }
        }
    }
}
