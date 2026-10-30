package dev.andikune.keyboardio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.CyberKeyBg
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary

@Composable
fun ResizePanel(
    currentScale: Float,
    rgbTheme: RgbThemeStyle,
    onScaleChanged: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        "Ringkas" to 0.85f,
        "Normal" to 1.0f,
        "Tinggi" to 1.15f,
        "Ekstra" to 1.30f
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberDarkSurface)
            .padding(16.dp)
            .testTag("resize_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sesuaikan Tinggi Keyboard (${(currentScale * 100).toInt()}%)",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(
                    onClick = { onScaleChanged(1.0f) },
                    modifier = Modifier.testTag("reset_keyboard_height")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Tinggi",
                        tint = TextSecondary
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("confirm_keyboard_height")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Simpan",
                        tint = rgbTheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (label, scale) ->
                val isSelected = kotlin.math.abs(currentScale - scale) < 0.05f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) rgbTheme.primary else CyberCard)
                        .clickable { onScaleChanged(scale) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) CyberDarkSurface else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Slider
        Slider(
            value = currentScale,
            onValueChange = onScaleChanged,
            valueRange = 0.75f..1.35f,
            colors = SliderDefaults.colors(
                thumbColor = rgbTheme.primary,
                activeTrackColor = rgbTheme.primary,
                inactiveTrackColor = CyberKeyBg
            ),
            modifier = Modifier.fillMaxWidth().testTag("height_slider")
        )
    }
}
