package dev.andikune.keyboardio.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.ui.theme.CyberBlack
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.NeonCyan
import dev.andikune.keyboardio.ui.theme.NeonMagenta
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary
import dev.andikune.keyboardio.ui.theme.TextTertiary

@Composable
fun VoiceInputOverlay(
    isListening: Boolean,
    liveSpokenText: String,
    rmsVolume: Float,
    errorMessage: String?,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    rgbTheme: RgbThemeStyle,
    onStopListening: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseMic")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(330.dp)
            .background(CyberDarkSurface)
            .padding(16.dp)
            .testTag("voice_input_overlay"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Input Suara / Dikte (Lokal)",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = TextSecondary
                )
            }
        }

        // Recognition language is explicit and persisted across keyboard sessions.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("SYSTEM" to "Otomatis", "id-ID" to "Indonesia", "en-US" to "English").forEach { (tag, label) ->
                Button(
                    onClick = { onLanguageSelected(tag) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedLanguage == tag) rgbTheme.primary else CyberCard,
                        contentColor = if (selectedLanguage == tag) CyberBlack else TextPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Animated glowing microphone
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(if (isListening) (1f + rmsVolume * 0.4f).coerceAtMost(1.4f) else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(rgbTheme.primary.copy(alpha = 0.4f), CyberCard)
                    )
                )
                .border(2.dp, rgbTheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Mikrofon Dikte",
                tint = if (isListening) NeonCyan else NeonMagenta,
                modifier = Modifier.size(38.dp)
            )
        }

        // Live recognized speech text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = NeonMagenta,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            } else if (liveSpokenText.isNotBlank()) {
                Text(
                    text = liveSpokenText,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            } else {
                Text(
                    text = if (isListening) "Mendengarkan... Silakan bicara dalam Bahasa Indonesia atau Inggris" else "Ketuk mikrofon untuk mulai bicara",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onStopListening,
                colors = ButtonDefaults.buttonColors(containerColor = rgbTheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = CyberBlack)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Selesai Dikte", color = CyberBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}
