package dev.andikune.keyboardio.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.andikune.keyboardio.data.UpdateManager
import dev.andikune.keyboardio.service.UpdateService

@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val update by UpdateManager.pending.collectAsState()
    val info = update ?: return

    AlertDialog(
        onDismissRequest = { UpdateManager.dismissForNow() },
        title = { Text("Pembaruan tersedia") },
        text = {
            Text(
                buildString {
                    append(info.title)
                    if (info.notes.isNotBlank()) {
                        append("\n\n")
                        append(info.notes)
                    }
                    append("\n\nFile APK akan disimpan di folder Pembaruan aplikasi sebelum installer dibuka.")
                }
            )
        },
        confirmButton = {
            Button(onClick = {
                UpdateManager.dismissForNow()
                UpdateService.install(context)
            }) {
                Text("Update")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { UpdateManager.skip(info.version) }) {
                    Text("Skip")
                }
                TextButton(onClick = { UpdateManager.dismissForNow() }) {
                    Text("Nanti")
                }
            }
        }
    )
}
