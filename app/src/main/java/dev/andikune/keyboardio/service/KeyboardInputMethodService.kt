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
import dev.andikune.keyboardio.data.TypingActivityStore
import dev.andikune.keyboardio.data.UserWordHistory
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
    lateinit var userWordHistory: UserWordHistory
        private set
    lateinit var typingActivityStore: TypingActivityStore
        private set

    val editorState = MutableStateFlow(KeyboardEditorState())
    val composingWord = MutableStateFlow("")
    val previousWord = MutableStateFlow("")

    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null

    private var audioFeedback: KeyboardAudioFeedback? = null
    private var voiceInputManager: VoiceInputManager? = null

    // Session typing activity metrics (counted from keyboard show to hide, auto-purged > 24h)
    private var sessionStartTime: Long = 0L
    private var sessionCharCount: Int = 0
    private var sessionWordCount: Int = 0

    private data class UndoOperation(val deletedText: String, val insertedText: String, val expectedTail: String)
    private val undoStack = Stack<UndoOperation>()

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkAndRecordClipboard()
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        preferences = KeyboardPreferences(applicationContext)
        clipboardRepo = ClipboardRepository(applicationContext)
        userWordHistory = UserWordHistory(applicationContext)
        typingActivityStore = TypingActivityStore(applicationContext)
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
                        userWordHistoryFlow = userWordHistory.words,
                        typingActivityStore = typingActivityStore,
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

        val isNumberOnly = classType == InputType.TYPE_CLASS_PHONE ||
                classType == InputType.TYPE_CLASS_NUMBER || classType == InputType.TYPE_CLASS_DATETIME
        val isAutoCorrectionAllowed = classType == InputType.TYPE_CLASS_TEXT &&
                variation != InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS &&
                variation != InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS &&
                variation != InputType.TYPE_TEXT_VARIATION_URI &&
                variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
                variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
                variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD &&
                (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0

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
            isAutoCorrectionAllowed = isAutoCorrectionAllowed && !isPassword,
            enterAction = enterAction
        )
        composingWord.value = ""
        undoStack.clear()
        updatePreviousWord()

        // Reset and begin tracking new typing session
        sessionStartTime = System.currentTimeMillis()
        sessionCharCount = 0
        sessionWordCount = 0

        if (!isPassword) {
            checkAndRecordClipboard()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        composingWord.value = ""

        // Complete session and save to store (auto-purged after 24 hours)
        val duration = (System.currentTimeMillis() - sessionStartTime).coerceAtLeast(0L)
        if (sessionCharCount > 0 || sessionWordCount > 0) {
            typingActivityStore.recordSession(
                charCount = sessionCharCount,
                wordCount = sessionWordCount.coerceAtLeast(1),
                durationMs = duration
            )
        }
        sessionCharCount = 0
        sessionWordCount = 0
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
                val selected = ic.getSelectedText(0)?.toString().orEmpty()
                if (selected.isNotEmpty()) {
                    ic.commitText("", 1)
                    recordUndo(selected, "")
                } else {
                    val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
                    if (before.isNotEmpty()) {
                        val removed = String(Character.toChars(Character.codePointBefore(before, before.length)))
                        ic.deleteSurroundingTextInCodePoints(1, 0)
                        recordUndo(removed, "")
                    }
                }
                syncComposingWordFromEditor()
            }
            is KeyAction.SmartDeleteWord -> {
                performSmartDeleteWord()
            }
            is KeyAction.Space -> {
                val currentWord = composingWord.value
                if (preferences.isAutoCorrectionEnabled() && editorState.value.isAutoCorrectionAllowed && currentWord.length >= 3) {
                    val autoFixed = WordSuggester.findAutoCorrection(currentWord, userWordHistory.words.value)
                    if (autoFixed != null && autoFixed.isNotEmpty()) {
                        // Replace current typo with corrected word
                        val before = ic.getTextBeforeCursor(currentWord.length, 0)?.toString().orEmpty()
                        if (!before.equals(currentWord, ignoreCase = true)) {
                            syncComposingWordFromEditor()
                            return
                        }
                        ic.deleteSurroundingTextInCodePoints(currentWord.codePointCount(0, currentWord.length), 0)
                        val replacement = "$autoFixed "
                        ic.commitText(replacement, 1)
                        recordUndo(currentWord, replacement)
                        userWordHistory.learn(autoFixed)
                        composingWord.value = ""
                        previousWord.value = autoFixed.lowercase(Locale.ROOT)
                        return
                    }
                }
                if (currentWord.isNotBlank()) {
                    userWordHistory.learn(currentWord)
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
                if (isMultiline && (noEnterAction || actionId == EditorInfo.IME_ACTION_NONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_DONE)) {
                    recordUndo("", "\n")
                }
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
        val textBefore = ic.getTextBeforeCursor(250, 0)?.toString().orEmpty()
        if (textBefore.isEmpty()) {
            syncComposingWordFromEditor()
            return
        }
        val deleteCount = SmartDeleteEngine.calculateSmartDeleteCount(textBefore)
            .coerceIn(0, textBefore.codePointCount(0, textBefore.length))
        if (deleteCount > 0) {
            val start = textBefore.offsetByCodePoints(textBefore.length, -deleteCount)
            val deleted = textBefore.substring(start)
            ic.deleteSurroundingTextInCodePoints(deleteCount, 0)
            recordUndo(deleted, "")
        }
        syncComposingWordFromEditor()
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
        recordUndo("", toCommit)

        sessionCharCount += toCommit.length
        if (toCommit.any { it == ' ' || it == '\n' || it == '.' || it == '?' || it == '!' || it == ',' }) {
            sessionWordCount++
        }

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
        val currentPrefix = composingWord.value
        if (currentPrefix.isNotEmpty()) {
            ic.deleteSurroundingTextInCodePoints(currentPrefix.codePointCount(0, currentPrefix.length), 0)
        }
        val inserted = "$cleanWord "
        ic.commitText(inserted, 1)
        recordUndo(currentPrefix, inserted)
        userWordHistory.learn(cleanWord)

        sessionCharCount += cleanWord.length + 1
        sessionWordCount++

        composingWord.value = ""
        previousWord.value = cleanWord.substringAfterLast(' ').lowercase(Locale.ROOT)
    }

    private fun updatePreviousWord() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(60, 0)?.toString() ?: ""
        val trimmed = textBefore.trimEnd()
        val lastWord = trimmed.substringAfterLast(' ', "").substringAfterLast('\n', "")
            .filter { it.isLetter() }
        previousWord.value = lastWord.lowercase(Locale.ROOT)
    }

    private fun recordUndo(deletedText: String, insertedText: String) {
        val ic = currentInputConnection ?: return
        val tail = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        undoStack.push(UndoOperation(deletedText, insertedText, tail))
        while (undoStack.size > 100) undoStack.removeAt(0)
    }

    private fun syncComposingWordFromEditor() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        composingWord.value = before.takeLastWhile { it.isLetter() || it == '\'' }
        updatePreviousWord()
    }

    private fun performUndo() {
        provideFeedback()
        val ic = currentInputConnection ?: return
        if (undoStack.isEmpty()) return
        val operation = undoStack.pop()
        val currentTail = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        if (!currentTail.endsWith(operation.expectedTail)) {
            undoStack.push(operation)
            return
        }
        if (operation.insertedText.isNotEmpty()) {
            ic.deleteSurroundingTextInCodePoints(operation.insertedText.codePointCount(0, operation.insertedText.length), 0)
        }
        if (operation.deletedText.isNotEmpty()) ic.commitText(operation.deletedText, 1)
        syncComposingWordFromEditor()
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
