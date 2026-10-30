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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.keyboardio.data.ClipboardRepository
import dev.andikune.keyboardio.data.KeyboardPreferences
import dev.andikune.keyboardio.model.ActivePanel
import dev.andikune.keyboardio.model.KeyAction
import dev.andikune.keyboardio.model.KeyItem
import dev.andikune.keyboardio.model.KeyboardLayoutMode
import dev.andikune.keyboardio.model.KeyboardLayouts
import dev.andikune.keyboardio.model.OneHandedMode
import dev.andikune.keyboardio.ui.theme.CyberBlack
import dev.andikune.keyboardio.ui.theme.CyberBorder
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary
import dev.andikune.keyboardio.ui.theme.TextTertiary
import kotlin.math.roundToInt

@Composable
fun KeyboardScreen(
    preferences: KeyboardPreferences,
    clipboardRepo: ClipboardRepository,
    onKeyAction: (KeyAction) -> Unit,
    onSecondaryChar: (String) -> Unit,
    onPasteText: (String) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rgbTheme by preferences.theme.collectAsState()
    val heightScale by preferences.heightScale.collectAsState()
    val oneHandedMode by preferences.oneHandedMode.collectAsState()
    val isFloating by preferences.isFloating.collectAsState()
    val clipboardItems by clipboardRepo.items.collectAsState()

    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.QWERTY_LOWER) }
    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }

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
                shape = androidx.compose.ui.graphics.RectangleShape
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

            // Top Quick Action Bar (Contains 4-dots Grid Menu Icon)
            TopActionBar(
                rgbTheme = rgbTheme,
                neonBrush = neonRainbowBrush,
                onOpenGridMenu = {
                    activePanel = if (activePanel == ActivePanel.GRID_MENU) ActivePanel.NONE else ActivePanel.GRID_MENU
                },
                onQuickEmoji = {
                    activePanel = if (activePanel == ActivePanel.EMOJI) ActivePanel.NONE else ActivePanel.EMOJI
                },
                onQuickClipboard = {
                    activePanel = if (activePanel == ActivePanel.CLIPBOARD) ActivePanel.NONE else ActivePanel.CLIPBOARD
                },
                onUndo = onUndo,
                onQuickResize = {
                    activePanel = if (activePanel == ActivePanel.RESIZE) ActivePanel.NONE else ActivePanel.RESIZE
                }
            )

            // Panel Area (Grid Menu, Emoji, Clipboard, Resize)
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
                    ActivePanel.NONE -> {}
                }
            }

            // Keyboard Layout Row (Handles One-Handed Mode)
            if (activePanel == ActivePanel.NONE) {
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

                    // Main Keyboard Area
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
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getQwertyRow2(isUpper),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getQwertyRow3(isUpper),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    isShiftActive = isUpper,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getQwertyRow4(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar,
                                    onOpenEmoji = { activePanel = ActivePanel.EMOJI }
                                )
                            }
                            KeyboardLayoutMode.NUMBERS_SYMBOLS -> {
                                KeyRow(
                                    keys = KeyboardLayouts.getSymbolsRow1(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getSymbolsRow2(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getSymbolsRow3(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getSymbolsRow4(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar,
                                    onOpenEmoji = { activePanel = ActivePanel.EMOJI }
                                )
                            }
                            KeyboardLayoutMode.MORE_SYMBOLS -> {
                                KeyRow(
                                    keys = KeyboardLayouts.getMoreSymbolsRow1(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getMoreSymbolsRow2(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getMoreSymbolsRow3(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar
                                )
                                KeyRow(
                                    keys = KeyboardLayouts.getMoreSymbolsRow4(),
                                    keyHeight = baseKeyHeight,
                                    rgbTheme = rgbTheme,
                                    onKeyClick = { handleKeyClick(it, layoutMode, onKeyAction) { next -> layoutMode = next } },
                                    onSecondaryClick = onSecondaryChar,
                                    onOpenEmoji = { activePanel = ActivePanel.EMOJI }
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

@Composable
private fun TopActionBar(
    rgbTheme: dev.andikune.keyboardio.model.RgbThemeStyle,
    neonBrush: Brush,
    onOpenGridMenu: () -> Unit,
    onQuickEmoji: () -> Unit,
    onQuickClipboard: () -> Unit,
    onUndo: () -> Unit,
    onQuickResize: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(CyberDarkSurface)
            .border(0.5.dp, CyberBorder)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Required 4-dots Grid Icon at top corner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenGridMenu)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("grid_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "Menu Grid 4 Titik",
                tint = rgbTheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = " iO",
                color = rgbTheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick shortcut icon row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onQuickEmoji, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.SentimentSatisfiedAlt,
                    contentDescription = "Emoji Cepat",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onQuickClipboard, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Papan Klip",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onUndo, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Urungkan",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onQuickResize, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = "Ubah Ukuran",
                    tint = TextSecondary,
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
    rgbTheme: dev.andikune.keyboardio.model.RgbThemeStyle,
    isShiftActive: Boolean = false,
    onKeyClick: (KeyItem) -> Unit,
    onSecondaryClick: (String) -> Unit,
    onOpenEmoji: (() -> Unit)? = null
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
                modifier = Modifier.weight(key.weight),
                onTap = {
                    if (key.action is KeyAction.OpenEmoji && onOpenEmoji != null) {
                        onOpenEmoji()
                    } else {
                        onKeyClick(key)
                    }
                },
                onLongPress = {
                    if (!key.secondary.isNullOrEmpty()) {
                        onSecondaryClick(key.secondary)
                    } else if (key.primary == "⌫") {
                        onKeyClick(key)
                    }
                }
            )
        }
    }
}

@Composable
private fun OneHandedSideBar(
    rgbTheme: dev.andikune.keyboardio.model.RgbThemeStyle,
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
