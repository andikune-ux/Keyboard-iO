package dev.andikune.keyboardio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.andikune.keyboardio.ui.theme.CyberBlack
import dev.andikune.keyboardio.ui.theme.CyberBorder
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.NeonCyan
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary
import dev.andikune.keyboardio.ui.theme.TextTertiary

/**
 * Modern In-Keyboard Translate Bar displayed directly above the keyboard keys
 * (similar to Gboard / standard IME translation bars).
 * Allows users to type with the keyboard, preview the translation in real-time,
 * and insert/paste into the target input field when confirmed.
 */
@Composable
fun TranslateBar(
    rgbTheme: RgbThemeStyle,
    sourceText: String,
    translatedText: String,
    isIdToEn: Boolean,
    onSwitchLanguage: () -> Unit,
    onClearText: () -> Unit,
    onInsertTranslation: (String) -> Unit,
    onPasteFromClipboard: (() -> Unit)? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberDarkSurface)
            .border(0.8.dp, rgbTheme.primary.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("translate_bar")
    ) {
        // Control Bar: Icon + Language switcher + Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = rgbTheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Terjemahan",
                    color = rgbTheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Language direction pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCard)
                        .border(0.5.dp, CyberBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onSwitchLanguage)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIdToEn) "ID" else "EN",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Tukar Bahasa",
                        tint = rgbTheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isIdToEn) "EN" else "ID",
                        color = rgbTheme.secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp).testTag("close_translate_bar")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Terjemahan",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Live Translate Display Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberBlack)
                .border(0.8.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Input text being typed
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (sourceText.isNotEmpty()) sourceText else "Ketik dengan keyboard di bawah...",
                        color = if (sourceText.isNotEmpty()) TextPrimary else TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Real-time offline translated result
                Text(
                    text = if (translatedText.isNotEmpty()) translatedText else "Hasil terjemahan muncul di sini...",
                    color = if (translatedText.isNotEmpty()) NeonCyan else TextTertiary,
                    fontSize = 13.sp,
                    fontWeight = if (translatedText.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            // Quick actions: Clear input & Insert to target
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sourceText.isNotEmpty()) {
                    IconButton(
                        onClick = onClearText,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Hapus Teks",
                            tint = TextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (onPasteFromClipboard != null) {
                    IconButton(
                        onClick = onPasteFromClipboard,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Tempel dari Clipboard",
                            tint = rgbTheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Button(
                    onClick = {
                        if (translatedText.isNotEmpty()) {
                            onInsertTranslation(translatedText)
                        } else if (sourceText.isNotEmpty()) {
                            onInsertTranslation(sourceText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (translatedText.isNotEmpty()) rgbTheme.primary else CyberCard
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp).testTag("insert_translation_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = if (translatedText.isNotEmpty()) CyberBlack else TextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tempel",
                        color = if (translatedText.isNotEmpty()) CyberBlack else TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
