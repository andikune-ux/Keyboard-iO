package dev.andikune.keyboardio.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.andikune.keyboardio.model.KeyItem
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.ui.theme.CyberBorder
import dev.andikune.keyboardio.ui.theme.CyberKeyBg
import dev.andikune.keyboardio.ui.theme.CyberKeyPressed
import dev.andikune.keyboardio.ui.theme.CyberKeySpecial
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary
import dev.andikune.keyboardio.ui.theme.TextTertiary

@Composable
fun KeyboardKey(
    keyItem: KeyItem,
    height: Dp,
    rgbTheme: RgbThemeStyle,
    isShiftActive: Boolean = false,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val keyBackground by animateColorAsState(
        targetValue = when {
            isPressed -> CyberKeyPressed
            keyItem.isActionKey && isShiftActive && keyItem.primary == "⇧" -> rgbTheme.primary.copy(alpha = 0.35f)
            keyItem.isActionKey -> CyberKeySpecial
            else -> CyberKeyBg
        },
        animationSpec = tween(durationMillis = 80),
        label = "keyBg"
    )

    val neonBorderBrush = remember(isPressed, rgbTheme) {
        if (isPressed) {
            Brush.horizontalGradient(
                listOf(rgbTheme.primary, rgbTheme.secondary, rgbTheme.accent)
            )
        } else {
            Brush.verticalGradient(
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
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .height(height)
            .shadow(
                elevation = if (isPressed) 6.dp else 2.dp,
                shape = shape,
                spotColor = if (isPressed) rgbTheme.primary else Color.Black
            )
            .clip(shape)
            .background(keyBackground)
            .border(
                width = if (isPressed) 1.5.dp else 0.8.dp,
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
        // Main primary label
        Text(
            text = keyItem.primary,
            color = if (keyItem.isActionKey && keyItem.primary == "↵") rgbTheme.primary else TextPrimary,
            fontSize = when {
                keyItem.primary.length > 2 -> 13.sp
                keyItem.primary == "spasi" -> 12.sp
                else -> 19.sp
            },
            fontWeight = if (keyItem.isActionKey) FontWeight.SemiBold else FontWeight.Medium
        )

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
    }
}
