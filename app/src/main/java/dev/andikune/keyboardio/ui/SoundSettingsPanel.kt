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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.keyboardio.data.KeyboardPreferences
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.model.SoundEffectType
import dev.andikune.keyboardio.service.KeyboardAudioFeedback
import dev.andikune.keyboardio.ui.theme.CyberBlack
import dev.andikune.keyboardio.ui.theme.CyberBorder
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.CyberKeyBg
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary
import dev.andikune.keyboardio.ui.theme.TextTertiary

@Composable
fun SoundSettingsPanel(
    preferences: KeyboardPreferences,
    audioFeedback: KeyboardAudioFeedback?,
    rgbTheme: RgbThemeStyle,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val soundEnabled by preferences.keySound.collectAsState()
    val currentSoundType by preferences.soundType.collectAsState()

    val soundOptions = listOf(
        Triple(SoundEffectType.MECHANICAL, "Mekanikal (Tactile Click)", "Ketukan mantap dan renyah khas switch mekanikal gaming"),
        Triple(SoundEffectType.TYPEWRITER, "Mesin Tik (Classic Strike)", "Ketukan logam khas mesin tik antik klasik"),
        Triple(SoundEffectType.DIGITAL_POP, "Digital Pop (Futuristic Beep)", "Bunyi gelembung pop neon futuristik"),
        Triple(SoundEffectType.STANDARD, "Standard Click", "Suara klik tombol standar bawaan sistem")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(CyberDarkSurface)
            .border(1.dp, rgbTheme.primary.copy(alpha = 0.5f))
            .padding(12.dp)
            .testTag("sound_settings_panel")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = rgbTheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengaturan Suara Ketikan",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Toggle Switch
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Efek Suara Ketikan Tombol",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (soundEnabled) "Status: Suara AKTIF" else "Status: Suara NONAKTIF",
                        color = if (soundEnabled) rgbTheme.primary else TextTertiary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        preferences.setSound(it)
                        if (it) {
                            audioFeedback?.playSound(currentSoundType)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = rgbTheme.primary,
                        checkedTrackColor = rgbTheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("sound_toggle_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Theme selection list
        Text(
            text = "Pilihan Variasi Jenis Suara:",
            color = if (soundEnabled) TextSecondary else TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(soundOptions) { (type, title, desc) ->
                val isSelected = currentSoundType == type
                val itemBg = if (isSelected) CyberCard else CyberBlack
                val itemBorder = if (isSelected) rgbTheme.primary else CyberBorder

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemBg)
                        .border(if (isSelected) 1.2.dp else 0.6.dp, itemBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            preferences.setSoundType(type)
                            if (soundEnabled) {
                                audioFeedback?.playSound(type)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) rgbTheme.primary else TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = title,
                                color = if (isSelected) rgbTheme.primary else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = desc,
                                color = TextTertiary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Test sound button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CyberKeyBg)
                            .clickable {
                                audioFeedback?.playSound(type)
                            }
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Uji Suara",
                            tint = rgbTheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
