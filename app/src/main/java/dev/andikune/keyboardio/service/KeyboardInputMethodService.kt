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
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
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
import dev.andikune.keyboardio.data.WordSuggester
import dev.andikune.keyboardio.model.EnterActionType
import dev.andikune.keyboardio.model.KeyAction
import dev.andikune.keyboardio.model.KeyboardEditorState
import dev.andikune.keyboardio.ui.KeyboardScreen
import dev.andikune.keyboardio.ui.theme.KeyboardIOTheme
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
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

    val editorState = MutableStateFlow(KeyboardEditorState())
    val composingWord = MutableStateFlow("")
    val previousWord = MutableStateFlow("")

    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null

    private var audioFeedback: KeyboardAudioFeedback? = null
    private var voiceInputManager: VoiceInputManager? = null

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
        audioFeedback = KeyboardAudioFeedback(applicationContext)
        voiceInputManager = VoiceInputManager(applicationContext) { spokenText ->
            commitText("$spokenText ")
        }

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

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        setOwnersRecursively(win.decorView)
    }

    override fun onCreateInputView(): View {
        attachOwnersToWindow()

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KeyboardInputMethodService)
            setViewTreeViewModelStoreOwner(this@KeyboardInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@KeyboardInputMethodService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    attachOwnersToWindow()
                    setOwnersRecursively(v.rootView)
                    (v.parent as? View)?.let { setOwnersRecursively(it) }
                }
                override fun onViewDetachedFromWindow(v: View) {}
            })

            setContent {
                KeyboardIOTheme {
                    KeyboardScreen(
                        preferences = preferences,
                        clipboardRepo = clipboardRepo,
                        editorStateFlow = editorState,
                        composingWordFlow = composingWord,
                        previousWordFlow = previousWord,
                        voiceInputManager = voiceInputManager,
                        audioFeedback = audioFeedback,
                        onKeyAction = { handleKeyAction(it) },
                        onSecondaryChar = { commitText(it) },
                        onPasteText = { commitText(it) },
                        onUndo = { performUndo() },
                        onSmartDeleteWord = { performSmartDeleteWord() },
                        onSuggestionPicked = { word -> handleSuggestionPicked(word) }
                    )
                }
            }
        }
    }

    private fun attachOwnersToWindow() {
        runCatching {
            val win = window?.window ?: return
            setOwnersRecursively(win.decorView)
        }
    }

    private fun setOwnersRecursively(view: View?) {
        if (view == null) return
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setOwnersRecursively(view.getChildAt(i))
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val inputType = info?.inputType ?: 0
        val classType = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION

        val isPassword = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                (classType == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)

        val isNumberOnly = classType == InputType.TYPE_CLASS_PHONE || classType == InputType.TYPE_CLASS_NUMBER

        val isMultiline = (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) &&
                ((inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0)
        val noEnterAction = ((info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

        val imeAction = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val enterAction = if (isMultiline && (noEnterAction || imeAction == EditorInfo.IME_ACTION_NONE || imeAction == EditorInfo.IME_ACTION_UNSPECIFIED || imeAction == EditorInfo.IME_ACTION_DONE)) {
            EnterActionType.RETURN
        } else {
            when (imeAction) {
                EditorInfo.IME_ACTION_SEARCH -> EnterActionType.SEARCH
                EditorInfo.IME_ACTION_SEND -> EnterActionType.SEND
                EditorInfo.IME_ACTION_GO -> EnterActionType.GO
                EditorInfo.IME_ACTION_NEXT -> EnterActionType.NEXT
                EditorInfo.IME_ACTION_DONE -> if (isMultiline) EnterActionType.RETURN else EnterActionType.DONE
                else -> EnterActionType.RETURN
            }
        }

        editorState.value = KeyboardEditorState(
            isPassword = isPassword,
            isNumberOnly = isNumberOnly,
            enterAction = enterAction
        )
        composingWord.value = ""
        updatePreviousWord()

        if (!isPassword) {
            checkAndRecordClipboard()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        composingWord.value = ""
    }

    override fun onDestroy() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)

        audioFeedback?.release()
        audioFeedback = null
        voiceInputManager?.destroy()
        voiceInputManager = null

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    private fun checkAndRecordClipboard() {
        if (editorState.value.isPassword) return // Incognito protection
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
                val curr = composingWord.value
                if (curr.isNotEmpty()) {
                    composingWord.value = curr.dropLast(1)
                }
                updatePreviousWord()
            }
            is KeyAction.SmartDeleteWord -> {
                performSmartDeleteWord()
            }
            is KeyAction.Space -> {
                val currentWord = composingWord.value
                if (preferences.autoCorrection.value && currentWord.length >= 2) {
                    val autoFixed = WordSuggester.findAutoCorrection(currentWord)
                    if (autoFixed != null && autoFixed.isNotEmpty()) {
                        // Replace current typo with corrected word
                        ic.deleteSurroundingText(currentWord.length, 0)
                        ic.commitText("$autoFixed ", 1)
                        undoStack.push("$autoFixed ")
                        composingWord.value = ""
                        previousWord.value = autoFixed.lowercase(Locale.ROOT)
                        return
                    }
                }
                commitText(" ")
                composingWord.value = ""
                updatePreviousWord()
            }
            is KeyAction.Enter -> {
                val editorInfo = currentInputEditorInfo
                val inputType = editorInfo?.inputType ?: 0
                val isMultiline = (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) &&
                        ((inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0)
                val imeOptions = editorInfo?.imeOptions ?: 0
                val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
                val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

                if (isMultiline) {
                    if (noEnterAction || actionId == EditorInfo.IME_ACTION_NONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_DONE) {
                        ic.commitText("\n", 1)
                    } else {
                        ic.performEditorAction(actionId)
                    }
                } else if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId)
                } else {
                    if (!ic.commitText("\n", 1)) {
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                    }
                }
                undoStack.push("\n")
                composingWord.value = ""
            }
            is KeyAction.SwitchIme -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchToNextInputMethod(false)
                }
            }
            else -> {}
        }
    }

    private fun performSmartDeleteWord() {
        provideFeedback()
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(250, 0)?.toString() ?: ""
        if (textBefore.isEmpty()) {
            ic.deleteSurroundingText(1, 0)
            composingWord.value = ""
            return
        }

        val deleteCount = dev.andikune.keyboardio.data.SmartDeleteEngine.calculateSmartDeleteCount(textBefore)
        val count = if (deleteCount > 0) deleteCount else 1
        ic.deleteSurroundingText(count, 0)
        composingWord.value = ""
    }

    private fun commitText(text: String) {
        provideFeedback()
        val ic = currentInputConnection ?: return

        var toCommit = text
        // Smart Auto-Capitalization: Capitalize start of sentence if enabled
        if (preferences.autoCapitalize.value &&
            text.length == 1 &&
            text[0].isLetter() &&
            composingWord.value.isEmpty()
        ) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString()
            val shouldCapitalize = before.isNullOrEmpty() ||
                    before.endsWith(". ") ||
                    before.endsWith("? ") ||
                    before.endsWith("! ") ||
                    before.endsWith("\n") ||
                    before == "." ||
                    before == "\n"
            if (shouldCapitalize) {
                toCommit = text.uppercase()
            }
        }

        ic.commitText(toCommit, 1)
        undoStack.push(toCommit)

        if (toCommit.length == 1 && toCommit[0].isLetter()) {
            composingWord.value += toCommit
        } else {
            composingWord.value = ""
            updatePreviousWord()
        }
    }

    private fun handleSuggestionPicked(word: String) {
        provideFeedback()
        val ic = currentInputConnection ?: return
        val cleanWord = word.trim('"', ' ')
        val currentPrefixLen = composingWord.value.length
        if (currentPrefixLen > 0) {
            ic.deleteSurroundingText(currentPrefixLen, 0)
        }
        ic.commitText("$cleanWord ", 1)
        undoStack.push("$cleanWord ")
        composingWord.value = ""
        previousWord.value = cleanWord.lowercase(Locale.ROOT)
    }

    private fun updatePreviousWord() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(60, 0)?.toString() ?: ""
        val trimmed = textBefore.trimEnd()
        val lastWord = trimmed.substringAfterLast(' ', "").substringAfterLast('\n', "")
            .filter { it.isLetter() }
        previousWord.value = lastWord.lowercase(Locale.ROOT)
    }

    private fun performUndo() {
        provideFeedback()
        val ic = currentInputConnection ?: return
        if (undoStack.isNotEmpty()) {
            val last = undoStack.pop()
            ic.deleteSurroundingText(last.length, 0)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        composingWord.value = ""
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
            audioFeedback?.playSound(preferences.soundType.value)
        }
    }
}
