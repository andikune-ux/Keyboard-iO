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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.keyboardio.data.QuickCalculator
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.CyberKeyBg
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary

@Composable
fun QuickCalculatorPanel(
    rgbTheme: RgbThemeStyle,
    onInsertResult: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expression by rememberSaveable { mutableStateOf("") }
    val result = QuickCalculator.evaluate(expression)
    val rows = listOf(
        listOf("C", "⌫", "(", ")"),
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "−"),
        listOf("0", ".", "%", "+")
    )
    fun append(token: String) {
        val value = when (token) {
            "÷" -> "/"
            "×" -> "*"
            "−" -> "-"
            else -> token
        }
        expression = when (token) {
            "C" -> ""
            "⌫" -> expression.dropLast(1)
            else -> (expression + value).take(160)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().background(CyberDarkSurface)
            .verticalScroll(rememberScrollState())
            .padding(12.dp).testTag("quick_calculator_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kalkulator Cepat", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = "Tutup kalkulator", tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(CyberCard).border(1.dp, rgbTheme.primary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text(if (expression.isBlank()) "Masukkan perhitungan" else expression,
                color = TextSecondary, fontSize = 17.sp, textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().testTag("calculator_expression"))
            Spacer(Modifier.height(6.dp))
            Text(result ?: "—", color = rgbTheme.primary, fontSize = 25.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().testTag("calculator_result"))
        }
        Spacer(Modifier.height(8.dp))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { token ->
                    val selected = token in listOf("÷", "×", "−", "+", "%")
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) rgbTheme.primary.copy(alpha = 0.18f) else CyberKeyBg)
                            .clickable { append(token) }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (token == "⌫") Icon(Icons.Default.Backspace, contentDescription = "Hapus satu karakter", tint = TextPrimary)
                        else Text(token, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(CyberKeyBg).clickable { append("C") }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("Bersihkan", color = TextPrimary, fontSize = 12.sp) }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(rgbTheme.primary).clickable { if (result != null) onInsertResult(result) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("Masukkan hasil", color = CyberDarkSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Text("Mendukung +, −, ×, ÷, %, desimal, dan tanda kurung.",
            color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
    }
}
