package dev.andikune.keyboardio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import dev.andikune.keyboardio.model.KeyAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.andikune.keyboardio.model.AuroraWave
import dev.andikune.keyboardio.model.KeyItem
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.ui.theme.CyberBorder
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberKeyBg
import dev.andikune.keyboardio.ui.theme.CyberKeyPressed
import dev.andikune.keyboardio.ui.theme.CyberKeySpecial
import dev.andikune.keyboardio.ui.theme.NeonCyan
import dev.andikune.keyboardio.ui.theme.NeonGreen
import dev.andikune.keyboardio.ui.theme.NeonMagenta
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary
import dev.andikune.keyboardio.ui.theme.TextTertiary
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun KeyboardKey(
    keyItem: KeyItem,
    height: Dp,
    rgbTheme: RgbThemeStyle,
    isShiftActive: Boolean = false,
    activeWave: AuroraWave? = null,
    waveProgress: Float = 0f,
    isAuroraEnabled: Boolean = true,
    isPopupEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    // Aurora wave intensity for this specific key, driven by animated waveProgress (0f -> 1f)
    val auroraIntensity by remember(activeWave, waveProgress) {
        derivedStateOf {
            if (isAuroraEnabled && activeWave != null && waveProgress in 0.001f..0.999f) {
                val dx = (keyItem.col - activeWave.originCol).toFloat()
                val dy = (keyItem.row - activeWave.originRow).toFloat()
                val dist = sqrt(dx * dx + dy * dy)
                val currentRadius = waveProgress * 6.5f
                val diff = abs(dist - currentRadius)
                val waveWidth = 1.3f
                if (diff < waveWidth) {
                    val decay = 1f - waveProgress
                    val profile = 1f - (diff / waveWidth)
                    (decay * profile).coerceIn(0f, 1f)
                } else 0f
            } else 0f
        }
    }

    val keyBackground by animateColorAsState(
        targetValue = when {
            isPressed -> CyberKeyPressed
            auroraIntensity > 0.15f -> NeonCyan.copy(alpha = 0.25f * auroraIntensity)
            keyItem.isActionKey && isShiftActive && keyItem.primary == "⇧" -> rgbTheme.primary.copy(alpha = 0.35f)
            keyItem.isActionKey -> CyberKeySpecial
            else -> CyberKeyBg
        },
        animationSpec = tween(durationMillis = 70),
        label = "keyBg"
    )

    val neonBorderBrush = remember(isPressed, auroraIntensity, rgbTheme) {
        when {
            isPressed -> Brush.horizontalGradient(
                listOf(rgbTheme.primary, rgbTheme.secondary, rgbTheme.accent)
            )
            auroraIntensity > 0.1f -> Brush.horizontalGradient(
                listOf(NeonCyan, NeonGreen, NeonMagenta)
            )
            else -> Brush.verticalGradient(
                listOf(
                    rgbTheme.primary.copy(alpha = 0.25f),
                    CyberBorder.copy(alpha = 0.4f)
                )
            )
        }
    }

    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .zIndex(if (isPressed) 15f else 0f)
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .height(height)
            .shadow(
                elevation = when {
                    isPressed -> 8.dp
                    auroraIntensity > 0.1f -> (5.dp * auroraIntensity)
                    else -> 2.dp
                },
                shape = shape,
                spotColor = if (isPressed) rgbTheme.primary else if (auroraIntensity > 0.1f) NeonCyan else Color.Black
            )
            .clip(shape)
            .background(keyBackground)
            .border(
                width = when {
                    isPressed -> 1.5.dp
                    auroraIntensity > 0.1f -> 1.3.dp
                    else -> 0.8.dp
                },
                brush = neonBorderBrush,
                shape = shape
            )
            .pointerInput(keyItem) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onTap() },
                    onLongPress = {
                        if (onLongPress != null) {
                            onLongPress()
                        } else if (!keyItem.secondary.isNullOrEmpty()) {
                            onTap()
                        }
                    }
                )
            }
            .testTag("key_${keyItem.primary}"),
        contentAlignment = Alignment.Center
    ) {
        // Main primary label (Icon for spacebar, Text for other keys)
        val isSpaceKey = keyItem.action is KeyAction.Space || keyItem.primary == "spasi" || keyItem.primary == "␣"
        if (isSpaceKey && (keyItem.primary == "spasi" || keyItem.primary == "␣" || keyItem.primary.isEmpty())) {
            Icon(
                imageVector = Icons.Default.SpaceBar,
                contentDescription = "Spasi",
                tint = when {
                    auroraIntensity > 0.3f -> NeonCyan
                    isPressed -> rgbTheme.primary
                    else -> TextSecondary
                },
                modifier = Modifier.size(24.dp)
            )
        } else if (isSpaceKey) {
            // Custom branded spacebar with spacebar icon alongside text
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SpaceBar,
                    contentDescription = null,
                    tint = if (isPressed) rgbTheme.primary else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = keyItem.primary,
                    color = if (auroraIntensity > 0.3f) NeonCyan else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = keyItem.primary,
                color = when {
                    keyItem.isActionKey && keyItem.primary == "↵" -> rgbTheme.primary
                    auroraIntensity > 0.3f -> NeonCyan
                    else -> TextPrimary
                },
                fontSize = when {
                    keyItem.primary.length > 3 -> 12.sp
                    else -> 18.sp
                },
                fontWeight = if (keyItem.isActionKey) FontWeight.SemiBold else FontWeight.Medium
            )
        }

        // Secondary character hint in upper right
        if (!keyItem.secondary.isNullOrEmpty()) {
            Text(
                text = keyItem.secondary,
                color = if (isPressed) rgbTheme.secondary else TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        }

        // Floating Key Pop-up Overlay (Efek Pop-up Mengambang)
        if (isPressed && isPopupEnabled && keyItem.primary.length <= 2 && !keyItem.isActionKey) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-52).dp)
                    .size(width = 50.dp, height = 48.dp)
                    .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = rgbTheme.primary)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCard)
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(rgbTheme.primary, rgbTheme.secondary)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = keyItem.primary,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
