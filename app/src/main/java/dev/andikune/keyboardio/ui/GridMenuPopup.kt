package dev.andikune.keyboardio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.ui.theme.CyberBorder
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary

data class GridMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun GridMenuPopup(
    rgbTheme: RgbThemeStyle,
    isOneHandedActive: Boolean,
    isFloatingActive: Boolean,
    onResizeClick: () -> Unit,
    onOneHandedClick: () -> Unit,
    onFloatingClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onUndoClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        GridMenuItem(
            id = "resize",
            title = "Ubah Ukuran",
            subtitle = "Atur tinggi keyboard",
            icon = Icons.Default.FormatSize,
            color = rgbTheme.primary
        ),
        GridMenuItem(
            id = "one_handed",
            title = "Satu Tangan",
            subtitle = if (isOneHandedActive) "Aktif (Kiri/Kanan)" else "Rapatkan keyboard",
            icon = Icons.Default.PanTool,
            color = rgbTheme.secondary
        ),
        GridMenuItem(
            id = "floating",
            title = "Mengambang",
            subtitle = if (isFloatingActive) "Mode Mengambang Aktif" else "Pindah posisi bebas",
            icon = Icons.Default.ViewStream,
            color = rgbTheme.accent
        ),
        GridMenuItem(
            id = "emoji",
            title = "Emoji Cepat",
            subtitle = "Buka panel emoji",
            icon = Icons.Default.SentimentSatisfiedAlt,
            color = rgbTheme.primary
        ),
        GridMenuItem(
            id = "undo",
            title = "Urungkan (Undo)",
            subtitle = "Hapus ketikan terakhir",
            icon = Icons.Default.Undo,
            color = rgbTheme.secondary
        ),
        GridMenuItem(
            id = "clipboard",
            title = "Papan Klip",
            subtitle = "Riwayat & tempel teks",
            icon = Icons.Default.ContentPaste,
            color = rgbTheme.accent
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(CyberDarkSurface)
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(rgbTheme.primary, rgbTheme.secondary)),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(12.dp)
            .testTag("grid_menu_popup")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(rgbTheme.primary)
                )
                Text(
                    text = " Fitur Tambahan Keyboard iO",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(32.dp).testTag("close_grid_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Menu",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of 6 items
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                GridMenuCard(
                    item = item,
                    rgbTheme = rgbTheme,
                    onClick = {
                        when (item.id) {
                            "resize" -> onResizeClick()
                            "one_handed" -> onOneHandedClick()
                            "floating" -> onFloatingClick()
                            "emoji" -> onEmojiClick()
                            "undo" -> onUndoClick()
                            "clipboard" -> onClipboardClick()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GridMenuCard(
    item: GridMenuItem,
    rgbTheme: RgbThemeStyle,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(CyberCard)
            .border(0.8.dp, CyberBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag("menu_item_${item.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.15f))
                .border(1.dp, item.color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = item.subtitle,
            color = TextSecondary,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}
