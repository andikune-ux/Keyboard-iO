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
import dev.andikune.keyboardio.data.ClipboardCapturePolicy
import dev.andikune.keyboardio.data.LongTextChunker
import dev.andikune.keyboardio.data.SmartDeleteEngine
import dev.andikune.keyboardio.data.KeyboardPreferences
import dev.andikune.keyboardio.data.TypingActivityStore
import dev.andikune.keyboardio.data.TypingInputPrivacy
import dev.andikune.keyboardio.data.UserWordHistory
import dev.andikune.keyboardio.data.WordSuggester
import dev.andikune.keyboardio.data.EditorTextContextSynchronizer
import dev.andikune.keyboardio.data.EditorInputConnectionContextReader
import dev.andikune.keyboardio.data.EditorInputTypePolicy
import dev.andikune.keyboardio.data.EditorWordReplacement
import dev.andikune.keyboardio.data.UnicodeBackspaceEngine
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
    private var editorContext = EditorTextContextSynchronizer.Snapshot.EMPTY

    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null

    private var audioFeedback: KeyboardAudioFeedback? = null
    private var voiceInputManager: VoiceInputManager? = null

    // Full text buffer for the visible keyboard session.
    private var sessionStartTime: Long = 0L
    private var sessionActive = false
    private var sessionCaptureAllowed = true
    private val sessionText = StringBuilder()

    private data class UndoOperation(val deletedText: String, val insertedText: String, val expectedTail: String, val expectedAfterCursor: String)
    private val undoStack = Stack<UndoOperation>()
    private companion object {
        const val MAX_UNDO_OPERATIONS = 100
        const val MAX_UNDO_TEXT_LENGTH = 4096
        const val UNDO_CONTEXT_LENGTH = 32
    }

    private var clipboardListenerRegistered = false
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkAndRecordClipboard()
    }

    private fun registerClipboardListener() {
        if (clipboardListenerRegistered) return
        (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.addPrimaryClipChangedListener(clipboardListener)
        clipboardListenerRegistered = true
    }

    private fun unregisterClipboardListener() {
        if (!clipboardListenerRegistered) return
        (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.removePrimaryClipChangedListener(clipboardListener)
        clipboardListenerRegistered = false
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        preferences = KeyboardPreferences(applicationContext)
        clipboardRepo = ClipboardRepository.getInstance(applicationContext)
        userWordHistory = UserWordHistory(applicationContext)
        typingActivityStore = TypingActivityStore(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioFeedback = KeyboardAudioFeedback(applicationContext)
        voiceInputManager = VoiceInputManager(applicationContext) { spokenText ->
            var previous = previousWord.value
            spokenText.split(Regex("\\s+"))
                .map { token -> token.trim().filter { it.isLetter() || it == '\'' } }
                .filter { it.length >= 2 }
                .forEach { token ->
                    userWordHistory.learn(token, previous)
                    previous = token
                }
            commitText("$spokenText ")
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

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
                        userWordBigramsFlow = userWordHistory.bigrams,
                        typingActivityStore = typingActivityStore,
                        voiceInputManager = voiceInputManager,
                        audioFeedback = audioFeedback,
                        onKeyAction = { handleKeyAction(it) },
                        onSecondaryChar = { commitText(it) },
                        onPasteText = { commitText(it) },
                        onSessionTextChanged = { oldText, newText -> replaceSessionText(oldText, newText) },
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

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // Recognition results belong to the editor that started the session.
        voiceInputManager?.cancelListening()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val inputType = info?.inputType ?: 0
        val classType = inputType and InputType.TYPE_MASK_CLASS

        val isPassword = TypingInputPrivacy.isPasswordInputType(inputType)

        val isNumberOnly = classType == InputType.TYPE_CLASS_PHONE ||
                classType == InputType.TYPE_CLASS_NUMBER || classType == InputType.TYPE_CLASS_DATETIME
        val inputCapabilities = EditorInputTypePolicy.resolve(inputType)

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
            isAutoCorrectionAllowed = inputCapabilities.autoCorrectionAllowed && !isPassword,
            isSuggestionsAllowed = inputCapabilities.suggestionsAllowed && !isPassword,
            enterAction = enterAction
        )
        composingWord.value = ""
        composingWordManuallyEdited = false
        undoStack.clear()
        updatePreviousWord()

        // Start timing only when a new visible keyboard session begins.
        // Input-view restarts and editor changes must not split one visible session.
        if (!sessionActive) {
            sessionStartTime = System.currentTimeMillis()
            sessionText.setLength(0)
            sessionActive = true
            typingActivityStore.beginSession(sessionStartTime)
        }
        // Never retain password-field content, even if the editor changes mid-session.
        sessionCaptureAllowed = !isPassword
        if (isPassword) unregisterClipboardListener() else registerClipboardListener()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        syncComposingWordFromEditor(hasSelection = newSelStart != newSelEnd)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        unregisterClipboardListener()
        // Prevent delayed callbacks from committing after the keyboard is hidden.
        voiceInputManager?.cancelListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        composingWord.value = ""

        finishTypingSession()
    }

    override fun onDestroy() {
        finishTypingSession()
        unregisterClipboardListener()

        audioFeedback?.release()
        audioFeedback = null
        voiceInputManager?.destroy()
        voiceInputManager = null

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    private fun checkAndRecordClipboard() {
        if (!ClipboardCapturePolicy.shouldCapture(
                isPasswordInput = editorState.value.isPassword,
                privateModeEnabled = preferences.isClipboardPrivateModeEnabled()
            )
        ) return
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
                    if (ic.commitText("", 1)) {
                        removeFromSessionSuffix(selected)
                        composingWordManuallyEdited = true
                        recordUndo(selected, "")
                    }
                } else {
                    // Read enough UTF-16 context to cover multi-code-point grapheme clusters
                    // such as family emoji, flags, skin-tone sequences and combining marks.
                    val before = ic.getTextBeforeCursor(128, 0)?.toString().orEmpty()
                    val deletion = UnicodeBackspaceEngine.previousGrapheme(before)
                    if (deletion != null) {
                        val removed = before.substring(deletion.utf16Start)
                        val deleted = runCatching {
                            ic.deleteSurroundingTextInCodePoints(deletion.codePointCount, 0)
                        }.getOrDefault(false)
                        if (deleted) {
                            removeFromSessionSuffix(removed)
                            composingWordManuallyEdited = true
                            recordUndo(removed, "")
                        }
                    }
                }
                syncComposingWordFromEditor()
            }
            is KeyAction.SmartDeleteWord -> {
                performSmartDeleteWord()
            }
            is KeyAction.Space -> {
                val currentWord = composingWord.value
                if (preferences.isAutoCorrectionEnabled() && editorState.value.isAutoCorrectionAllowed &&
                    !composingWordManuallyEdited &&
                    editorContext.isReliable && !editorContext.hasSelection &&
                    editorContext.cursorAtWordEnd && currentWord.length >= 3) {
                    val autoFixed = WordSuggester.findAutoCorrection(currentWord, userWordHistory.words.value)
                    if (autoFixed != null && autoFixed.isNotEmpty()) {
                        // Replace current typo with corrected word
                        val before = ic.getTextBeforeCursor(currentWord.length, 0)?.toString().orEmpty()
                        if (!before.equals(currentWord, ignoreCase = true)) {
                            syncComposingWordFromEditor()
                            return
                        }
                        val replacement = EditorTextContextSynchronizer.autoCorrectionCommitText(
                            autoFixed,
                            editorContext.textAfterWord
                        )
                        val result = EditorWordReplacement.replace(
                            connection = ic,
                            beforeCodePoints = currentWord.codePointCount(0, currentWord.length),
                            afterCodePoints = 0,
                            originalText = currentWord,
                            replacementText = replacement
                        )
                        if (result.succeeded) {
                            removeFromSessionSuffix(currentWord)
                            appendToSession(replacement)
                            recordUndo(currentWord, replacement)
                            userWordHistory.learn(autoFixed, previousWord.value)
                            composingWord.value = ""
                            previousWord.value = autoFixed.lowercase(Locale.ROOT)
                        } else if (!result.rollbackSucceeded) {
                            syncComposingWordFromEditor()
                        }
                        return
                    }
                }
                if (currentWord.isNotBlank()) {
                    userWordHistory.learn(currentWord, previousWord.value)
                }
                commitText(" ")
                composingWordManuallyEdited = false
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
                        if (ic.commitText("\n", 1)) {
                            appendToSession("\n")
                            recordUndo("", "\n")
                        }
                    } else {
                        ic.performEditorAction(actionId)
                    }
                } else if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId)
                } else {
                    if (ic.commitText("\n", 1)) {
                        appendToSession("\n")
                        recordUndo("", "\n")
                    } else {
                        val downSent = ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                        val upSent = ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                        if (downSent || upSent) appendToSession("\n")
                    }
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
            if (ic.deleteSurroundingTextInCodePoints(deleteCount, 0)) {
                removeFromSessionSuffix(deleted)
                recordUndo(deleted, "")
            }
        }
        syncComposingWordFromEditor()
    }

    private fun commitText(text: String) {
        provideFeedback()
        val ic = currentInputConnection ?: return

        // commitText replaces an active selection. Preserve that selection in the
        // inverse operation so Undo restores the user's original highlighted text.
        val selectedBefore = ic.getSelectedText(0)?.toString().orEmpty()
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

        // Large clipboard/paste payloads are sent in bounded transactions to avoid
        // Binder transaction limits. Chunk boundaries never split UTF-16 surrogate pairs.
        val chunks = LongTextChunker.split(toCommit)
        val inserted = StringBuilder(toCommit.length)
        for (chunk in chunks) {
            val committed = runCatching { ic.commitText(chunk, 1) }.getOrDefault(false)
            if (!committed) break
            inserted.append(chunk)
        }
        if (inserted.isNotEmpty()) {
            val insertedText = inserted.toString()
            appendToSession(insertedText)
            recordUndo(selectedBefore, insertedText)
        }

        // The editor's actual cursor/text is the source of truth; this also handles
        // selection changes, emoji, pasted text, and editor-side replacements.
        syncComposingWordFromEditor()
    }

    private fun handleSuggestionPicked(word: String) {
        provideFeedback()
        val ic = currentInputConnection ?: return
        val cleanWord = word.trim('"', ' ')
        if (cleanWord.isEmpty()) return

        // Read fresh context: the editor may have moved the cursor since the
        // suggestion strip was last recomposed.
        val context = readEditorContext() ?: return
        if (!context.isReliable || context.hasSelection) {
            syncComposingWordFromEditor()
            return
        }

        val beforeCount = context.prefixBeforeCursor.codePointCount(0, context.prefixBeforeCursor.length)
        val afterCount = context.suffixAfterCursor.codePointCount(0, context.suffixAfterCursor.length)
        val deletedText = context.composingWord
        // Replace the complete token in one batch. If insertion fails after deletion,
        // restore the original token before ending the editor transaction.
        val inserted = cleanWord + if (context.textAfterWord.isEmpty()) " " else ""
        val result = EditorWordReplacement.replace(
            connection = ic,
            beforeCodePoints = beforeCount,
            afterCodePoints = afterCount,
            originalText = deletedText,
            replacementText = inserted
        )
        if (result.succeeded) {
            composingWordManuallyEdited = false
            if (deletedText.isNotEmpty()) removeFromSessionSuffix(deletedText)
            appendToSession(inserted)
            recordUndo(deletedText, inserted)
            userWordHistory.learn(cleanWord, context.previousWord)
        } else if (!result.rollbackSucceeded) {
            syncComposingWordFromEditor()
            return
        }
        syncComposingWordFromEditor()
    }

    private fun updatePreviousWord() {
        syncComposingWordFromEditor()
    }

    private fun recordUndo(deletedText: String, insertedText: String) {
        val ic = currentInputConnection ?: return
        if (deletedText.length > MAX_UNDO_TEXT_LENGTH || insertedText.length > MAX_UNDO_TEXT_LENGTH) return
        val contextLength = (insertedText.length + UNDO_CONTEXT_LENGTH).coerceAtLeast(64)
            .coerceAtMost(MAX_UNDO_TEXT_LENGTH + UNDO_CONTEXT_LENGTH)
        val tail = ic.getTextBeforeCursor(contextLength, 0)?.toString().orEmpty()
        if (insertedText.isNotEmpty() && !tail.endsWith(insertedText)) return
        val afterCursor = ic.getTextAfterCursor(UNDO_CONTEXT_LENGTH, 0)?.toString().orEmpty()
        undoStack.push(UndoOperation(deletedText, insertedText, tail, afterCursor))
        while (undoStack.size > MAX_UNDO_OPERATIONS) undoStack.removeAt(0)
    }

    private fun readEditorContext(): EditorTextContextSynchronizer.Snapshot? =
        EditorInputConnectionContextReader.read(currentInputConnection)

    private fun syncComposingWordFromEditor(hasSelection: Boolean = false) {
        val context = readEditorContext()
        if (context == null) {
            editorContext = EditorTextContextSynchronizer.Snapshot.EMPTY
            composingWord.value = ""
            previousWord.value = ""
            return
        }
        editorContext = if (hasSelection) EditorTextContextSynchronizer.Snapshot.EMPTY else context
        composingWord.value = editorContext.composingWord
        previousWord.value = if (
            editorContext.composingWord.isNotEmpty() || editorContext.canPredictNextWord
        ) editorContext.previousWord else ""
    }

    private fun performUndo() {
        provideFeedback()
        val ic = currentInputConnection ?: return
        if (undoStack.isEmpty()) return
        val operation = undoStack.peek()
        val after = ic.getTextAfterCursor(UNDO_CONTEXT_LENGTH, 0)?.toString().orEmpty()
        if (after != operation.expectedAfterCursor) return
        if (operation.insertedText.isNotEmpty()) {
            val actual = ic.getTextBeforeCursor(operation.insertedText.length, 0)?.toString().orEmpty()
            if (actual != operation.insertedText) return
        } else {
            val actual = ic.getTextBeforeCursor(operation.expectedTail.length, 0)?.toString().orEmpty()
            if (actual != operation.expectedTail) return
        }
        undoStack.pop()
        ic.beginBatchEdit()
        var removedInsertion = false
        var succeeded = true
        try {
            if (operation.insertedText.isNotEmpty()) {
                succeeded = ic.deleteSurroundingTextInCodePoints(
                    operation.insertedText.codePointCount(0, operation.insertedText.length), 0
                )
                removedInsertion = succeeded
            }
            if (succeeded && operation.deletedText.isNotEmpty()) succeeded = ic.commitText(operation.deletedText, 1)
            if (!succeeded && removedInsertion && operation.insertedText.isNotEmpty()) {
                ic.commitText(operation.insertedText, 1)
            }
        } catch (_: Exception) {
            succeeded = false
            if (removedInsertion && operation.insertedText.isNotEmpty()) {
                runCatching { ic.commitText(operation.insertedText, 1) }
            }
        } finally {
            ic.endBatchEdit()
        }
        if (!succeeded) {
            undoStack.push(operation)
            return
        }
        if (operation.insertedText.isNotEmpty()) removeFromSessionSuffix(operation.insertedText)
        if (operation.deletedText.isNotEmpty()) appendToSession(operation.deletedText)
        syncComposingWordFromEditor()
    }

    private fun replaceSessionText(oldText: String, newText: String) {
        if (!sessionActive || !sessionCaptureAllowed) return
        if (oldText.isNotEmpty() && sessionText.endsWith(oldText)) {
            sessionText.setLength(sessionText.length - oldText.length)
        }
        if (newText.isNotEmpty()) sessionText.append(newText)
        typingActivityStore.updateActiveSession(sessionStartTime, sessionText.toString())
    }

    private fun appendToSession(text: String) {
        if (sessionActive && sessionCaptureAllowed && text.isNotEmpty()) {
            sessionText.append(text)
            typingActivityStore.updateActiveSession(sessionStartTime, sessionText.toString())
        }
    }

    private fun removeFromSessionSuffix(text: String) {
        if (!sessionActive || !sessionCaptureAllowed || text.isEmpty()) return
        if (sessionText.endsWith(text)) {
            sessionText.setLength(sessionText.length - text.length)
            typingActivityStore.updateActiveSession(sessionStartTime, sessionText.toString())
        }
    }

    private fun finishTypingSession() {
        if (!sessionActive) return
        val endedAt = System.currentTimeMillis()
        typingActivityStore.recordSession(
            text = sessionText.toString(),
            startedAt = sessionStartTime,
            endedAt = endedAt
        )
        sessionText.setLength(0)
        sessionActive = false
        sessionStartTime = 0L
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
