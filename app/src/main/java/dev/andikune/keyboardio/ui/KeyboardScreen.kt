package dev.andikune.keyboardio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import dev.andikune.keyboardio.service.KeyboardAudioFeedback
import dev.andikune.keyboardio.data.OfflineTranslator
import dev.andikune.keyboardio.data.SmartDeleteEngine
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.keyboardio.data.ClipboardRepository
import dev.andikune.keyboardio.data.KeyboardPreferences
import dev.andikune.keyboardio.data.WordSuggester
import dev.andikune.keyboardio.model.ActivePanel
import dev.andikune.keyboardio.model.AuroraWave
import dev.andikune.keyboardio.model.KeyAction
import dev.andikune.keyboardio.model.KeyItem
import dev.andikune.keyboardio.model.KeyboardEditorState
import dev.andikune.keyboardio.model.KeyboardLayoutMode
import dev.andikune.keyboardio.model.KeyboardLayouts
import dev.andikune.keyboardio.model.OneHandedMode
import dev.andikune.keyboardio.model.RgbThemeStyle
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.andikune.keyboardio.MainActivity
import dev.andikune.keyboardio.data.TypingActivityStats
import dev.andikune.keyboardio.data.TypingActivityStore
import java.io.File
import dev.andikune.keyboardio.service.VoiceInputManager
import dev.andikune.keyboardio.ui.theme.CyberBlack
import dev.andikune.keyboardio.ui.theme.CyberBorder
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.NeonCyan
import dev.andikune.keyboardio.ui.theme.NeonGreen
import dev.andikune.keyboardio.ui.theme.NeonMagenta
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary
import dev.andikune.keyboardio.ui.theme.TextTertiary
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

@Composable
fun KeyboardScreen(
    preferences: KeyboardPreferences,
    clipboardRepo: ClipboardRepository,
    editorStateFlow: StateFlow<KeyboardEditorState>,
    composingWordFlow: StateFlow<String>,
    previousWordFlow: StateFlow<String>? = null,
    userWordHistoryFlow: StateFlow<Map<String, Int>>? = null,
    typingActivityStore: TypingActivityStore? = null,
    voiceInputManager: VoiceInputManager? = null,
    audioFeedback: KeyboardAudioFeedback? = null,
    onKeyAction: (KeyAction) -> Unit,
    onSecondaryChar: (String) -> Unit,
    onPasteText: (String) -> Unit,
    onUndo: () -> Unit,
    onSmartDeleteWord: () -> Unit,
    onSuggestionPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rgbTheme by preferences.theme.collectAsState()
    val heightScale by preferences.heightScale.collectAsState()
    val oneHandedMode by preferences.oneHandedMode.collectAsState()
    val isFloating by preferences.isFloating.collectAsState()
    val spacebarStyle by preferences.spacebarStyle.collectAsState()
    val isAuroraEnabled by preferences.auroraWaveEffect.collectAsState()
    val isPopupEnabled by preferences.keyPopupEffect.collectAsState()
    val isSwipeTypingEnabled by preferences.swipeTyping.collectAsState()

    // Custom background states
    val customBgType by preferences.customBgType.collectAsState()
    val customBgPath by preferences.customBgPath.collectAsState()
    val customBgOpacity by preferences.customBgOpacity.collectAsState()
    val customBgDim by preferences.customBgDim.collectAsState()

    val clipboardItems by clipboardRepo.items.collectAsState()
    val editorState by editorStateFlow.collectAsState()
    val composingWord by composingWordFlow.collectAsState()
    val previousWord by (previousWordFlow?.collectAsState() ?: remember { mutableStateOf("") })
    val learnedWords by (userWordHistoryFlow?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })

    // Active voice recognition state if available
    val isVoiceListening by (voiceInputManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) })
    val liveSpokenText by (voiceInputManager?.liveSpokenText?.collectAsState() ?: remember { mutableStateOf("") })
    val rmsVolume by (voiceInputManager?.rmsVolume?.collectAsState() ?: remember { mutableFloatStateOf(0f) })
    val voiceErrorMessage by (voiceInputManager?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) })

    var isTranslateMode by remember { mutableStateOf(false) }
    var translateSourceText by remember { mutableStateOf("") }
    var isIdToEn by remember { mutableStateOf(true) }

    val translateResultText = remember(translateSourceText, isIdToEn) {
        if (translateSourceText.isBlank()) ""
        else OfflineTranslator.translate(translateSourceText, isIdToEn)
    }

    val currentWordForSuggestions = if (isTranslateMode) {
        translateSourceText.substringAfterLast(' ')
    } else {
        composingWord
    }

    val suggestions = remember(currentWordForSuggestions, previousWord, learnedWords, editorState.isPassword, isTranslateMode) {
        if (editorState.isPassword) {
            emptyList()
        } else if (currentWordForSuggestions.trim().isNotEmpty()) {
            WordSuggester.getWordCompletionsAndCorrections(currentWordForSuggestions, learnedWords = learnedWords)
        } else if (previousWord.isNotBlank() && !editorState.isNumberOnly) {
            WordSuggester.getNextWordPredictions(previousWord, learnedWords = learnedWords)
        } else {
            emptyList()
        }
    }

    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.QWERTY_LOWER) }
    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }
    var isToolsOpen by remember { mutableStateOf(false) }
    var activeWave by remember { mutableStateOf<AuroraWave?>(null) }
    val waveAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val effectiveKeyAction: (KeyAction) -> Unit = { action ->
        if (isTranslateMode) {
            when (action) {
                is KeyAction.CommitText -> {
                    translateSourceText += action.text
                }
                is KeyAction.Space -> {
                    translateSourceText += " "
                }
                is KeyAction.Backspace -> {
                    if (translateSourceText.isNotEmpty()) {
                        translateSourceText = translateSourceText.dropLast(1)
                    }
                }
                is KeyAction.SmartDeleteWord -> {
                    val count = SmartDeleteEngine.calculateSmartDeleteCount(translateSourceText)
                    translateSourceText = if (count > 0 && count <= translateSourceText.length) {
                        translateSourceText.dropLast(count)
                    } else ""
                }
                is KeyAction.Enter -> {
                    if (translateResultText.isNotEmpty()) {
                        onPasteText(translateResultText)
                        translateSourceText = ""
                    } else if (translateSourceText.isNotEmpty()) {
                        onPasteText(translateSourceText)
                        translateSourceText = ""
                    }
                }
                else -> onKeyAction(action)
            }
        } else {
            onKeyAction(action)
        }
    }

    val effectiveSecondaryChar: (String) -> Unit = { char ->
        if (isTranslateMode) {
            translateSourceText += char
        } else {
            onSecondaryChar(char)
        }
    }

    val effectiveSmartDelete: () -> Unit = {
        if (isTranslateMode) {
            val count = SmartDeleteEngine.calculateSmartDeleteCount(translateSourceText)
            translateSourceText = if (count > 0 && count <= translateSourceText.length) {
                translateSourceText.dropLast(count)
            } else ""
        } else {
            onSmartDeleteWord()
        }
    }

    val effectiveSuggestionPicked: (String) -> Unit = { rawWord ->
        val word = rawWord.trim('"', ' ')
        if (isTranslateMode) {
            val lastSpace = translateSourceText.lastIndexOf(' ')
            translateSourceText = if (lastSpace >= 0) {
                translateSourceText.substring(0, lastSpace + 1) + "$word "
            } else {
                "$word "
            }
        } else {
            onSuggestionPicked(word)
        }
    }

    var userHasTypedInSession by remember { mutableStateOf(false) }
    var dismissedClipId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(composingWord) {
        if (composingWord.isNotEmpty()) {
            userHasTypedInSession = true
        }
    }

    val latestClipItem = clipboardItems.firstOrNull()
    val clipboardSuggestion = remember(latestClipItem, dismissedClipId, userHasTypedInSession, composingWord, editorState.isPassword) {
        if (!editorState.isPassword &&
            !userHasTypedInSession &&
            composingWord.isEmpty() &&
            latestClipItem != null &&
            latestClipItem.text.isNotBlank() &&
            latestClipItem.id != dismissedClipId
        ) {
            latestClipItem.text
        } else null
    }

    fun onKeyTapped(key: KeyItem) {
        userHasTypedInSession = true
        if (isAuroraEnabled) {
            coroutineScope.launch {
                activeWave = AuroraWave(key.row, key.col)
                waveAnim.snapTo(0f)
                waveAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 380, easing = LinearEasing)
                )
                activeWave = null
            }
        }
        handleKeyClick(key, layoutMode, effectiveKeyAction) { next -> layoutMode = next }
    }

    // Swipe typing touch points
    val swipeTrail = remember { mutableStateListOf<Offset>() }

    // If input requires number only, switch layout automatically
    LaunchedEffect(editorState.isNumberOnly) {
        layoutMode = if (editorState.isNumberOnly) KeyboardLayoutMode.NUMBERS_SYMBOLS else KeyboardLayoutMode.QWERTY_LOWER
    }

    // Floating drag offset
    var floatingOffsetX by remember { mutableFloatStateOf(0f) }
    var floatingOffsetY by remember { mutableFloatStateOf(0f) }

    // Dynamic RGB wave animation for neon border
    val infiniteTransition = rememberInfiniteTransition(label = "rgbAnim")
    val rgbPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rgbPhase"
    )

    val neonRainbowBrush = remember(rgbPhase, rgbTheme) {
        val colors = listOf(
            rgbTheme.primary,
            rgbTheme.secondary,
            rgbTheme.accent,
            rgbTheme.primary
        )
        Brush.horizontalGradient(
            colors = colors,
            startX = rgbPhase * 1000f,
            endX = (rgbPhase + 1f) * 1000f
        )
    }

    val baseKeyHeight = 48.dp * heightScale
    val enterLabel = editorState.enterAction.displayLabel
    val spaceLabel = spacebarStyle.displayLabel

    val containerModifier = if (isFloating) {
        modifier
            .offset { IntOffset(floatingOffsetX.roundToInt(), floatingOffsetY.roundToInt()) }
            .padding(12.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = rgbTheme.primary)
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, neonRainbowBrush, RoundedCornerShape(18.dp))
            .background(CyberBlack)
    } else {
        modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(rgbTheme.primary.copy(alpha = 0.5f), Color.Transparent)
                ),
                shape = RectangleShape
            )
    }

    Box(
        modifier = containerModifier.testTag("keyboard_main_container")
    ) {
        // Custom background layer (Photo / Video from Gallery)
        if (customBgType != "NONE" && customBgPath.isNotBlank()) {
            val bgFile = remember(customBgPath) { File(customBgPath) }
            if (bgFile.exists() && bgFile.canRead()) {
                Box(modifier = Modifier.matchParentSize()) {
                    if (customBgType == "IMAGE") {
                        val bitmap = remember(customBgPath) {
                            runCatching {
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeFile(customBgPath, opts)
                                var sampleSize = 1
                                while ((opts.outWidth / sampleSize) > 1280 || (opts.outHeight / sampleSize) > 720) {
                                    sampleSize *= 2
                                }
                                val decodeOpts = BitmapFactory.Options().apply {
                                    inSampleSize = sampleSize
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                }
                                BitmapFactory.decodeFile(customBgPath, decodeOpts)
                            }.getOrNull()
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(customBgOpacity)
                            )
                        }
                    } else if (customBgType == "VIDEO") {
                        SafeVideoBackground(
                            videoPath = customBgPath,
                            opacity = customBgOpacity,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    // Contrast Dim Scrim to ensure keys and neon stay 100% visible
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = customBgDim))
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // Floating Drag Handle Bar (only in floating mode)
            if (isFloating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberCard)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                floatingOffsetX += dragAmount.x
                                floatingOffsetY += dragAmount.y
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Geser Keyboard",
                            tint = rgbTheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = " Keyboard iO (Mengambang)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { preferences.setFloating(false) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Kembali Penuh",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Top Header Bar:
            // "iO" + Grid Menu -> Speech to text mic -> Emoji icon -> Suggestions Strip -> Quick Action buttons
            TopHeaderBar(
                rgbTheme = rgbTheme,
                isPassword = editorState.isPassword,
                suggestions = suggestions,
                clipboardSuggestion = clipboardSuggestion,
                onPasteClipboardSuggestion = { text ->
                    onPasteText(text)
                    dismissedClipId = latestClipItem?.id
                },
                onDismissClipboardSuggestion = {
                    dismissedClipId = latestClipItem?.id
                },
                isToolsOpen = isToolsOpen,
                isTranslateMode = isTranslateMode,
                onToggleTools = { isToolsOpen = !isToolsOpen },
                onSuggestionPicked = effectiveSuggestionPicked,
                onOpenGridMenu = {
                    activePanel = if (activePanel == ActivePanel.GRID_MENU) ActivePanel.NONE else ActivePanel.GRID_MENU
                },
                onOpenVoiceInput = {
                    if (activePanel == ActivePanel.VOICE_INPUT) {
                        voiceInputManager?.cancelListening()
                        activePanel = ActivePanel.NONE
                    } else {
                        activePanel = ActivePanel.VOICE_INPUT
                        voiceInputManager?.startListening()
                    }
                },
                onOpenEmoji = {
                    activePanel = if (activePanel == ActivePanel.EMOJI) ActivePanel.NONE else ActivePanel.EMOJI
                },
                onOpenTranslate = {
                    isTranslateMode = !isTranslateMode
                    if (activePanel == ActivePanel.TRANSLATE) activePanel = ActivePanel.NONE
                },
                onOpenNicknameFont = {
                    activePanel = if (activePanel == ActivePanel.NICKNAME_FONT) ActivePanel.NONE else ActivePanel.NICKNAME_FONT
                },
                onQuickClipboard = {
                    activePanel = if (activePanel == ActivePanel.CLIPBOARD) ActivePanel.NONE else ActivePanel.CLIPBOARD
                },
                onUndo = onUndo
            )

            // In-Keyboard Translate Bar (Displayed directly above keyboard keys, Gboard style)
            if (isTranslateMode) {
                TranslateBar(
                    rgbTheme = rgbTheme,
                    sourceText = translateSourceText,
                    translatedText = translateResultText,
                    isIdToEn = isIdToEn,
                    onSwitchLanguage = { isIdToEn = !isIdToEn },
                    onClearText = { translateSourceText = "" },
                    onInsertTranslation = { text ->
                        onPasteText(text)
                        translateSourceText = ""
                    },
                    onPasteFromClipboard = {
                        val clip = clipboardItems.firstOrNull()?.text
                        if (!clip.isNullOrBlank()) translateSourceText = clip
                    },
                    onClose = {
                        isTranslateMode = false
                        translateSourceText = ""
                    }
                )
            }

            // Panel Area (Grid Menu, Emoji, Clipboard, Resize, Nickname, Voice Input, Typing Activity)
            AnimatedVisibility(
                visible = activePanel != ActivePanel.NONE,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                when (activePanel) {
                    ActivePanel.GRID_MENU -> {
                        GridMenuPopup(
                            rgbTheme = rgbTheme,
                            isOneHandedActive = oneHandedMode != OneHandedMode.OFF,
                            isFloatingActive = isFloating,
                            onResizeClick = { activePanel = ActivePanel.RESIZE },
                            onOneHandedClick = {
                                val next = when (oneHandedMode) {
                                    OneHandedMode.OFF -> OneHandedMode.RIGHT
                                    OneHandedMode.RIGHT -> OneHandedMode.LEFT
                                    OneHandedMode.LEFT -> OneHandedMode.OFF
                                }
                                preferences.setOneHandedMode(next)
                                activePanel = ActivePanel.NONE
                            },
                            onFloatingClick = {
                                preferences.setFloating(!isFloating)
                                activePanel = ActivePanel.NONE
                            },
                            onEmojiClick = { activePanel = ActivePanel.EMOJI },
                            onUndoClick = {
                                onUndo()
                                activePanel = ActivePanel.NONE
                            },
                            onClipboardClick = { activePanel = ActivePanel.CLIPBOARD },
                            onTranslateClick = {
                                isTranslateMode = true
                                activePanel = ActivePanel.NONE
                            },
                            onNicknameFontClick = { activePanel = ActivePanel.NICKNAME_FONT },
                            onVoiceInputClick = {
                                activePanel = ActivePanel.VOICE_INPUT
                                voiceInputManager?.startListening()
                            },
                            onSoundClick = {
                                activePanel = ActivePanel.SOUND_SETTINGS
                            },
                            onSettingsClick = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                activePanel = ActivePanel.NONE
                            },
                            onTypingActivityClick = {
                                activePanel = ActivePanel.TYPING_ACTIVITY
                            },
                            onCloseClick = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.TYPING_ACTIVITY -> {
                        val stats = typingActivityStore?.getStats() ?: TypingActivityStats(0, 0, 0, 0L, emptyList())
                        TypingActivityPanel(
                            stats = stats,
                            rgbTheme = rgbTheme,
                            onClearHistory = { typingActivityStore?.clearAll() },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.EMOJI -> {
                        EmojiPicker(
                            rgbTheme = rgbTheme,
                            onEmojiSelected = { emoji ->
                                onKeyAction(KeyAction.CommitText(emoji))
                            },
                            onBackspace = { onKeyAction(KeyAction.Backspace) },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.CLIPBOARD -> {
                        ClipboardPanel(
                            items = clipboardItems,
                            rgbTheme = rgbTheme,
                            onPasteText = { text ->
                                onPasteText(text)
                                activePanel = ActivePanel.NONE
                            },
                            onTogglePin = { id -> clipboardRepo.togglePin(id) },
                            onDeleteItem = { id -> clipboardRepo.removeItem(id) },
                            onClearAll = { clipboardRepo.clearUnpinned() },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.RESIZE -> {
                        ResizePanel(
                            currentScale = heightScale,
                            rgbTheme = rgbTheme,
                            onScaleChanged = { preferences.setHeightScale(it) },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.TRANSLATE -> {
                        isTranslateMode = true
                        activePanel = ActivePanel.NONE
                    }
                    ActivePanel.NICKNAME_FONT -> {
                        NicknameFontPanel(
                            rgbTheme = rgbTheme,
                            initialText = composingWord,
                            onInsertText = { text ->
                                onPasteText(text)
                            },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.VOICE_INPUT -> {
                        VoiceInputOverlay(
                            isListening = isVoiceListening,
                            liveSpokenText = liveSpokenText,
                            rmsVolume = rmsVolume,
                            errorMessage = voiceErrorMessage,
                            rgbTheme = rgbTheme,
                            onStopListening = {
                                voiceInputManager?.stopListening()
                                activePanel = ActivePanel.NONE
                            },
                            onClose = {
                                voiceInputManager?.cancelListening()
                                activePanel = ActivePanel.NONE
                            }
                        )
                    }
                    ActivePanel.SOUND_SETTINGS -> {
                        SoundSettingsPanel(
                            preferences = preferences,
                            audioFeedback = audioFeedback,
                            rgbTheme = rgbTheme,
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.NONE -> {}
                }
            }

            // Keyboard Layout Row (Handles One-Handed Mode)
            if (activePanel == ActivePanel.NONE) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left sidebar in One-Handed Mode (when keyboard is pushed to the right)
                        if (oneHandedMode == OneHandedMode.RIGHT) {
                            OneHandedSideBar(
                                rgbTheme = rgbTheme,
                                isLeft = true,
                                onSwitchSide = { preferences.setOneHandedMode(OneHandedMode.LEFT) },
                                onExpand = { preferences.setOneHandedMode(OneHandedMode.OFF) },
                                modifier = Modifier.width(54.dp)
                            )
                        }

                        // Main Keyboard Key Rows
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                        ) {
                            when (layoutMode) {
                                KeyboardLayoutMode.QWERTY_LOWER,
                                KeyboardLayoutMode.QWERTY_UPPER,
                                KeyboardLayoutMode.CAPS_LOCK -> {
                                    val isUpper = layoutMode != KeyboardLayoutMode.QWERTY_LOWER
                                    val currentWaveProgress = if (activeWave != null) waveAnim.value else 0f
                                    KeyRow(
                                        keys = KeyboardLayouts.getQwertyRow1(isUpper),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getQwertyRow2(isUpper),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        startSpacerWeight = 0.5f,
                                        endSpacerWeight = 0.5f,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getQwertyRow3(isUpper),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        isShiftActive = isUpper,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar,
                                        onSmartDeleteWord = effectiveSmartDelete
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getQwertyRow4(enterLabel, spaceLabel),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                }
                                KeyboardLayoutMode.NUMBERS_SYMBOLS -> {
                                    val currentWaveProgress = if (activeWave != null) waveAnim.value else 0f
                                    KeyRow(
                                        keys = KeyboardLayouts.getSymbolsRow1(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getSymbolsRow2(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getSymbolsRow3(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar,
                                        onSmartDeleteWord = effectiveSmartDelete
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getSymbolsRow4(enterLabel, spaceLabel),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                }
                                KeyboardLayoutMode.MORE_SYMBOLS -> {
                                    val currentWaveProgress = if (activeWave != null) waveAnim.value else 0f
                                    KeyRow(
                                        keys = KeyboardLayouts.getMoreSymbolsRow1(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getMoreSymbolsRow2(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getMoreSymbolsRow3(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar,
                                        onSmartDeleteWord = effectiveSmartDelete
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getMoreSymbolsRow4(enterLabel, spaceLabel),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        waveProgress = currentWaveProgress,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { onKeyTapped(it) },
                                        onSecondaryClick = effectiveSecondaryChar
                                    )
                                }
                            }
                        }

                        // Right sidebar in One-Handed Mode (when keyboard is pushed to the left)
                        if (oneHandedMode == OneHandedMode.LEFT) {
                            OneHandedSideBar(
                                rgbTheme = rgbTheme,
                                isLeft = false,
                                onSwitchSide = { preferences.setOneHandedMode(OneHandedMode.RIGHT) },
                                onExpand = { preferences.setOneHandedMode(OneHandedMode.OFF) },
                                modifier = Modifier.width(54.dp)
                            )
                        }
                    }

                    // Swipe Typing Glowing Neon Trail Overlay
                    if (isSwipeTypingEnabled && swipeTrail.size > 1) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val path = Path()
                            path.moveTo(swipeTrail.first().x, swipeTrail.first().y)
                            for (i in 1 until swipeTrail.size) {
                                path.lineTo(swipeTrail[i].x, swipeTrail[i].y)
                            }
                            drawPath(
                                path = path,
                                brush = Brush.linearGradient(
                                    listOf(rgbTheme.primary, rgbTheme.secondary, NeonCyan)
                                ),
                                style = Stroke(
                                    width = 6.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleKeyClick(
    keyItem: KeyItem,
    currentMode: KeyboardLayoutMode,
    onKeyAction: (KeyAction) -> Unit,
    setMode: (KeyboardLayoutMode) -> Unit
) {
    when (val action = keyItem.action) {
        is KeyAction.Shift -> {
            when (currentMode) {
                KeyboardLayoutMode.QWERTY_LOWER -> setMode(KeyboardLayoutMode.QWERTY_UPPER)
                KeyboardLayoutMode.QWERTY_UPPER -> setMode(KeyboardLayoutMode.CAPS_LOCK)
                KeyboardLayoutMode.CAPS_LOCK -> setMode(KeyboardLayoutMode.QWERTY_LOWER)
                else -> setMode(KeyboardLayoutMode.QWERTY_LOWER)
            }
        }
        is KeyAction.SwitchToSymbols -> setMode(KeyboardLayoutMode.NUMBERS_SYMBOLS)
        is KeyAction.SwitchToMoreSymbols -> setMode(KeyboardLayoutMode.MORE_SYMBOLS)
        is KeyAction.SwitchToLetters -> setMode(KeyboardLayoutMode.QWERTY_LOWER)
        is KeyAction.CommitText -> {
            onKeyAction(action)
            if (currentMode == KeyboardLayoutMode.QWERTY_UPPER) {
                setMode(KeyboardLayoutMode.QWERTY_LOWER)
            }
        }
        else -> onKeyAction(action)
    }
}

/**
 * Top Header Bar matching user specifications:
 * - Top Left: "iO" + Grid Menu icon
 * - Beside it: Speech-to-Text Button (Input Suara / Dikte 🎤)
 * - Beside Speech-to-Text: Main Emoji Icon (☺)
 * - Middle: Word Suggestions Strip (Saran Pengetikan Cerdas)
 * - Right: Translate 🌐, Font Nickname 🎨, Clipboard 📋, Undo ↩
 */
@Composable
private fun TopHeaderBar(
    rgbTheme: RgbThemeStyle,
    isPassword: Boolean,
    suggestions: List<String>,
    clipboardSuggestion: String? = null,
    onPasteClipboardSuggestion: (String) -> Unit = {},
    onDismissClipboardSuggestion: () -> Unit = {},
    isToolsOpen: Boolean,
    isTranslateMode: Boolean,
    onToggleTools: () -> Unit,
    onSuggestionPicked: (String) -> Unit,
    onOpenGridMenu: () -> Unit,
    onOpenVoiceInput: () -> Unit,
    onOpenEmoji: () -> Unit,
    onOpenTranslate: () -> Unit,
    onOpenNicknameFont: () -> Unit,
    onQuickClipboard: () -> Unit,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(CyberDarkSurface)
            .border(0.5.dp, CyberBorder)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Group Left (tetap): [iO + Menu] -> [Mic STT] -> [Emoji]
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Text "iO" & Grid Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenGridMenu)
                    .padding(horizontal = 5.dp, vertical = 3.dp)
                    .testTag("grid_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Menu Fitur",
                    tint = rgbTheme.primary,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = " iO",
                    color = rgbTheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Speech-to-Text / Voice Input Button (Tepat di samping teks iO)
            IconButton(
                onClick = onOpenVoiceInput,
                modifier = Modifier.size(28.dp).testTag("speech_to_text_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Input Suara / Dikte",
                    tint = NeonCyan,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Main Emoji Icon (Samping tombol Speech-to-Text)
            IconButton(
                onClick = onOpenEmoji,
                modifier = Modifier.size(28.dp).testTag("main_emoji_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SentimentSatisfiedAlt,
                    contentDescription = "Ikon Emoji Utama",
                    tint = TextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Center Area: Password Guard OR Clipboard Suggestion Chip OR Word Suggestions Strip (Saran Pengetikan Cerdas)
        if (isPassword) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberCard)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Sandi (Aman)",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (clipboardSuggestion != null) {
            // Saran Tempel Hasil Salin (Gboard clipboard chip with one-time dismiss)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCard)
                        .border(0.8.dp, rgbTheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onPasteClipboardSuggestion(clipboardSuggestion) }
                            .testTag("clipboard_suggestion_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Tempel",
                            tint = rgbTheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val cleanDisplay = clipboardSuggestion.take(15).replace('\n', ' ')
                        Text(
                            text = "Tempel: \"$cleanDisplay${if (clipboardSuggestion.length > 15) "…" else ""}\"",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Abaikan saran tempel",
                        tint = TextTertiary,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable { onDismissClipboardSuggestion() }
                            .testTag("dismiss_clipboard_suggestion")
                    )
                }
            }
        } else if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                suggestions.take(3).forEach { word ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCard)
                            .border(0.6.dp, rgbTheme.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onSuggestionPicked(word) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = word,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Group Right: Single Door Toggle Button ("Tombol Pintu Buka Tutup")
        // Triggers showing / hiding secondary tools (Terjemahan, Font Unik, Clipboard, Kembali/Undo)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(
                visible = isToolsOpen,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenTranslate,
                        modifier = Modifier.size(27.dp).testTag("header_translate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Terjemahan",
                            tint = if (isTranslateMode) rgbTheme.primary else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenNicknameFont,
                        modifier = Modifier.size(27.dp).testTag("header_nickname_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Font Nickname",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onQuickClipboard,
                        modifier = Modifier.size(27.dp).testTag("header_clipboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Papan Klip",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onUndo,
                        modifier = Modifier.size(27.dp).testTag("header_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Urungkan",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Tombol Pintu Buka Tutup (Pemicu sembunyikan/tampilkan tombol peralatan sebelah kanan)
            IconButton(
                onClick = onToggleTools,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("tools_door_toggle_button")
            ) {
                Icon(
                    imageVector = if (isToolsOpen) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.MoreHoriz,
                    contentDescription = if (isToolsOpen) "Tutup Menu Peralatan" else "Buka Menu Peralatan",
                    tint = if (isToolsOpen) rgbTheme.primary else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<KeyItem>,
    keyHeight: androidx.compose.ui.unit.Dp,
    rgbTheme: RgbThemeStyle,
    activeWave: AuroraWave? = null,
    waveProgress: Float = 0f,
    isAuroraEnabled: Boolean = true,
    isPopupEnabled: Boolean = true,
    isShiftActive: Boolean = false,
    startSpacerWeight: Float = 0f,
    endSpacerWeight: Float = 0f,
    onKeyClick: (KeyItem) -> Unit,
    onSecondaryClick: (String) -> Unit,
    onSmartDeleteWord: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (startSpacerWeight > 0f) {
            Spacer(modifier = Modifier.weight(startSpacerWeight))
        }
        keys.forEach { key ->
            KeyboardKey(
                keyItem = key,
                height = keyHeight,
                rgbTheme = rgbTheme,
                isShiftActive = isShiftActive,
                activeWave = activeWave,
                waveProgress = waveProgress,
                isAuroraEnabled = isAuroraEnabled,
                isPopupEnabled = isPopupEnabled,
                modifier = Modifier.weight(key.weight),
                onTap = {
                    onKeyClick(key)
                },
                onLongPress = {
                    if (key.primary == "⌫" && onSmartDeleteWord != null) {
                        onSmartDeleteWord()
                    } else if (!key.secondary.isNullOrEmpty()) {
                        onSecondaryClick(key.secondary)
                    } else {
                        onKeyClick(key)
                    }
                },
                onSwipeLeft = {
                    if (key.primary == "⌫" && onSmartDeleteWord != null) {
                        onSmartDeleteWord()
                    }
                },
                onSmartDeleteWord = onSmartDeleteWord
            )
        }
        if (endSpacerWeight > 0f) {
            Spacer(modifier = Modifier.weight(endSpacerWeight))
        }
    }
}

@Composable
private fun OneHandedSideBar(
    rgbTheme: RgbThemeStyle,
    isLeft: Boolean,
    onSwitchSide: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CyberCard)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            onClick = onExpand,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Kembali Ukuran Penuh",
                tint = rgbTheme.primary
            )
        }

        IconButton(
            onClick = onSwitchSide,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isLeft) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Pindah Sisi",
                tint = rgbTheme.secondary
            )
        }
    }
}

@Composable
private fun SafeVideoBackground(
    videoPath: String,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    var mediaPlayer: MediaPlayer? = null
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        runCatching {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(videoPath)
                                setSurface(Surface(surface))
                                isLooping = true
                                setVolume(0f, 0f) // Completely muted, 0% audio noise
                                setOnErrorListener { _, _, _ -> true }
                                prepareAsync()
                                setOnPreparedListener { start() }
                            }
                        }
                    }
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        runCatching {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                        }
                        return true
                    }
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier.alpha(opacity)
    )
}
