package dev.andikune.keyboardio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val EMOJI_CATEGORIES = listOf(
    "Senyum" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥹", "😊", "😇", "🙂", "😉", "😌", "😍", "🥰", "😘", "😗", "😚", "😋", "😛", "😜", "🤪", "😎", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😮‍💨", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤔", "🫣", "🤫", "🫡", "🫢", "🫠", "🥱", "😴", "🤤"),
    "Gestur" to listOf("👋", "🤚", "🖐", "✋", "🖖", "👌", "🤌", "🤏", "✌", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍", "💅", "🤳", "💪"),
    "Cinta" to listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "💌"),
    "Aktivitas" to listOf("⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🎱", "🏓", "🏸", "🥊", "🥋", "🎯", "🎮", "🕹", "🎲", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖", "🎪", "🎨", "🎭", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸"),
    "Objek" to listOf("🔥", "⚡", "✨", "⭐", "🌟", "💫", "💥", "💯", "💢", "💬", "👁️‍🗨️", "📱", "💻", "⌨️", "🖥️", "🖨️", "🕹️", "💾", "💿", "💡", "🔦", "🕯️", "🧯", "💎", "🔮", "🚀", "🛸", "🛰️", "🛸")
)

@Composable
fun EmojiPicker(
    rgbTheme: RgbThemeStyle,
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val currentEmojis = remember(selectedCategoryIndex) {
        EMOJI_CATEGORIES[selectedCategoryIndex].second
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(CyberDarkSurface)
            .testTag("emoji_picker_panel")
    ) {
        // Category tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EMOJI_CATEGORIES.forEachIndexed { index, (title, _) ->
                val isSelected = selectedCategoryIndex == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) rgbTheme.primary else CyberKeyBg)
                        .clickable { selectedCategoryIndex = index }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) CyberDarkSurface else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Grid of emojis
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 42.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(currentEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }

        // Bottom control row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("close_emoji_picker")) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Kembali ke Keyboard",
                    tint = rgbTheme.primary
                )
            }

            IconButton(onClick = onBackspace, modifier = Modifier.testTag("emoji_backspace")) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Hapus",
                    tint = TextSecondary
                )
            }
        }
    }
}
