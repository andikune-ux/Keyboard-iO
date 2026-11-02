package dev.andikune.keyboardio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
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
    voiceInputManager: VoiceInputManager? = null,
    onKeyAction: (KeyAction) -> Unit,
    onSecondaryChar: (String) -> Unit,
    onPasteText: (String) -> Unit,
    onUndo: () -> Unit,
    onSmartDeleteWord: () -> Unit,
    onSuggestionPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rgbTheme by preferences.theme.collectAsState()
    val heightScale by preferences.heightScale.collectAsState()
    val oneHandedMode by preferences.oneHandedMode.collectAsState()
    val isFloating by preferences.isFloating.collectAsState()
    val spacebarStyle by preferences.spacebarStyle.collectAsState()
    val isAuroraEnabled by preferences.auroraWaveEffect.collectAsState()
    val isPopupEnabled by preferences.keyPopupEffect.collectAsState()
    val isSwipeTypingEnabled by preferences.swipeTyping.collectAsState()

    val clipboardItems by clipboardRepo.items.collectAsState()
    val editorState by editorStateFlow.collectAsState()
    val composingWord by composingWordFlow.collectAsState()

    // Active voice recognition state if available
    val isVoiceListening by (voiceInputManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) })
    val liveSpokenText by (voiceInputManager?.liveSpokenText?.collectAsState() ?: remember { mutableStateOf("") })
    val rmsVolume by (voiceInputManager?.rmsVolume?.collectAsState() ?: remember { mutableFloatStateOf(0f) })
    val voiceErrorMessage by (voiceInputManager?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) })

    val suggestions = remember(composingWord, editorState.isPassword) {
        if (editorState.isPassword) emptyList() else WordSuggester.getSuggestions(composingWord)
    }

    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.QWERTY_LOWER) }
    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }
    var activeWave by remember { mutableStateOf<AuroraWave?>(null) }

    // Swipe typing touch points
    val swipeTrail = remember { mutableStateListOf<Offset>() }

    // If input requires number only, switch layout automatically
    LaunchedEffect(editorState.isNumberOnly) {
        if (editorState.isNumberOnly) {
            layoutMode = KeyboardLayoutMode.NUMBERS_SYMBOLS
        }
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

    val baseKeyHeight = 44.dp * heightScale
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
                onSuggestionPicked = onSuggestionPicked,
                onOpenGridMenu = {
                    activePanel = if (activePanel == ActivePanel.GRID_MENU) ActivePanel.NONE else ActivePanel.GRID_MENU
                },
                onOpenVoiceInput = {
                    if (activePanel == ActivePanel.VOICE_INPUT) {
                        voiceInputManager?.stopListening()
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
                    activePanel = if (activePanel == ActivePanel.TRANSLATE) ActivePanel.NONE else ActivePanel.TRANSLATE
                },
                onOpenNicknameFont = {
                    activePanel = if (activePanel == ActivePanel.NICKNAME_FONT) ActivePanel.NONE else ActivePanel.NICKNAME_FONT
                },
                onQuickClipboard = {
                    activePanel = if (activePanel == ActivePanel.CLIPBOARD) ActivePanel.NONE else ActivePanel.CLIPBOARD
                },
                onUndo = onUndo
            )

            // Panel Area (Grid Menu, Emoji, Clipboard, Resize, Translate, Nickname, Voice Input)
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
                            onTranslateClick = { activePanel = ActivePanel.TRANSLATE },
                            onNicknameFontClick = { activePanel = ActivePanel.NICKNAME_FONT },
                            onVoiceInputClick = {
                                activePanel = ActivePanel.VOICE_INPUT
                                voiceInputManager?.startListening()
                            },
                            onCloseClick = { activePanel = ActivePanel.NONE }
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
                        TranslatePanel(
                            rgbTheme = rgbTheme,
                            onInsertTranslation = { text ->
                                onPasteText(text)
                            },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
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
                                voiceInputManager?.stopListening()
                                activePanel = ActivePanel.NONE
                            }
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
                                    KeyRow(
                                        keys = KeyboardLayouts.getQwertyRow1(isUpper),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getQwertyRow2(isUpper),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getQwertyRow3(isUpper),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        isShiftActive = isUpper,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar,
                                        onSmartDeleteWord = onSmartDeleteWord
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getQwertyRow4(enterLabel, spaceLabel),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
                                    )
                                }
                                KeyboardLayoutMode.NUMBERS_SYMBOLS -> {
                                    KeyRow(
                                        keys = KeyboardLayouts.getSymbolsRow1(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getSymbolsRow2(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getSymbolsRow3(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar,
                                        onSmartDeleteWord = onSmartDeleteWord
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getSymbolsRow4(enterLabel, spaceLabel),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
                                    )
                                }
                                KeyboardLayoutMode.MORE_SYMBOLS -> {
                                    KeyRow(
                                        keys = KeyboardLayouts.getMoreSymbolsRow1(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getMoreSymbolsRow2(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getMoreSymbolsRow3(),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar,
                                        onSmartDeleteWord = onSmartDeleteWord
                                    )
                                    KeyRow(
                                        keys = KeyboardLayouts.getMoreSymbolsRow4(enterLabel, spaceLabel),
                                        keyHeight = baseKeyHeight,
                                        rgbTheme = rgbTheme,
                                        activeWave = activeWave,
                                        isAuroraEnabled = isAuroraEnabled,
                                        isPopupEnabled = isPopupEnabled,
                                        onKeyClick = { key ->
                                            activeWave = AuroraWave(key.row, key.col)
                                            handleKeyClick(key, layoutMode, onKeyAction) { next -> layoutMode = next }
                                        },
                                        onSecondaryClick = onSecondaryChar
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
        // Group Left: [iO + Menu] -> [Mic STT] -> [Emoji]
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

        // Center Area: Password Guard OR Word Suggestions Strip (Saran Pengetikan Cerdas)
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

        // Group Right: Translate 🌐, Font Nickname 🎨, Clipboard 📋, Undo ↩
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenTranslate, modifier = Modifier.size(27.dp)) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Terjemahan",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = onOpenNicknameFont, modifier = Modifier.size(27.dp)) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Font Nickname",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = onQuickClipboard, modifier = Modifier.size(27.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Papan Klip",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = onUndo, modifier = Modifier.size(27.dp)) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Urungkan",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
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
    isAuroraEnabled: Boolean = true,
    isPopupEnabled: Boolean = true,
    isShiftActive: Boolean = false,
    onKeyClick: (KeyItem) -> Unit,
    onSecondaryClick: (String) -> Unit,
    onSmartDeleteWord: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEach { key ->
            KeyboardKey(
                keyItem = key,
                height = keyHeight,
                rgbTheme = rgbTheme,
                isShiftActive = isShiftActive,
                activeWave = activeWave,
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
                }
            )
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
