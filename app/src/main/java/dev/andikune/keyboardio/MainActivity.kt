package dev.andikune.keyboardio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.andikune.keyboardio.data.ClipboardRepository
import dev.andikune.keyboardio.data.KeyboardPreferences
import dev.andikune.keyboardio.model.OneHandedMode
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.ui.theme.CyberBlack
import dev.andikune.keyboardio.ui.theme.CyberBorder
import dev.andikune.keyboardio.ui.theme.CyberCard
import dev.andikune.keyboardio.ui.theme.CyberDarkSurface
import dev.andikune.keyboardio.ui.theme.CyberKeyBg
import dev.andikune.keyboardio.ui.theme.KeyboardIOTheme
import dev.andikune.keyboardio.ui.theme.NeonCyan
import dev.andikune.keyboardio.ui.theme.NeonGreen
import dev.andikune.keyboardio.ui.theme.NeonMagenta
import dev.andikune.keyboardio.ui.theme.TextPrimary
import dev.andikune.keyboardio.ui.theme.TextSecondary
import dev.andikune.keyboardio.ui.theme.TextTertiary

class MainActivity : ComponentActivity() {

    private lateinit var preferences: KeyboardPreferences
    private lateinit var clipboardRepo: ClipboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = KeyboardPreferences(applicationContext)
        clipboardRepo = ClipboardRepository(applicationContext)

        setContent {
            KeyboardIOTheme {
                MainScreen(
                    preferences = preferences,
                    clipboardRepo = clipboardRepo,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        startActivity(intent)
                    },
                    onOpenPicker = {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    preferences: KeyboardPreferences,
    clipboardRepo: ClipboardRepository,
    onOpenSettings: () -> Unit,
    onOpenPicker: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isImeEnabled by remember { mutableStateOf(false) }
    var isImeSelected by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var testText by remember { mutableStateOf("") }

    val rgbTheme by preferences.theme.collectAsState()
    val heightScale by preferences.heightScale.collectAsState()
    val oneHandedMode by preferences.oneHandedMode.collectAsState()
    val isFloating by preferences.isFloating.collectAsState()
    val hapticEnabled by preferences.hapticFeedback.collectAsState()
    val soundEnabled by preferences.keySound.collectAsState()
    val clipboardList by clipboardRepo.items.collectAsState()

    fun checkKeyboardStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null) {
            val enabledMethods = imm.enabledInputMethodList
            val myPackage = context.packageName
            isImeEnabled = enabledMethods.any { it.packageName == myPackage }

            val currentDefault = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            isImeSelected = currentDefault != null && currentDefault.contains(myPackage)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkKeyboardStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkKeyboardStatus()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(rgbTheme.primary, rgbTheme.secondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "iO",
                                color = CyberBlack,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Keyboard iO",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "RGB Neon Mechanical Typing",
                                color = rgbTheme.primary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDarkSurface
                )
            )
        },
        containerColor = CyberBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberDarkSurface,
                contentColor = rgbTheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = rgbTheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Aktivasi & Tes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = rgbTheme.primary,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Kustomisasi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = rgbTheme.primary,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Papan Klip", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = rgbTheme.primary,
                    unselectedContentColor = TextSecondary
                )
            }

            when (selectedTab) {
                0 -> ActivationAndTestTab(
                    isImeEnabled = isImeEnabled,
                    isImeSelected = isImeSelected,
                    rgbTheme = rgbTheme,
                    testText = testText,
                    onTestTextChange = { testText = it },
                    onOpenSettings = onOpenSettings,
                    onOpenPicker = onOpenPicker
                )
                1 -> CustomizationTab(
                    preferences = preferences,
                    rgbTheme = rgbTheme,
                    heightScale = heightScale,
                    oneHandedMode = oneHandedMode,
                    isFloating = isFloating,
                    hapticEnabled = hapticEnabled,
                    soundEnabled = soundEnabled
                )
                2 -> ClipboardTab(
                    items = clipboardList,
                    rgbTheme = rgbTheme,
                    onTogglePin = { clipboardRepo.togglePin(it) },
                    onDeleteItem = { clipboardRepo.removeItem(it) },
                    onClearAll = { clipboardRepo.clearUnpinned() }
                )
            }
        }
    }
}

@Composable
private fun ActivationAndTestTab(
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    rgbTheme: RgbThemeStyle,
    testText: String,
    onTestTextChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPicker: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Panduan Aktivasi Keyboard iO",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aktifkan keyboard dalam 2 langkah mudah untuk mulai menikmati ketikan RGB Neon.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Step 1 Card
        item {
            StatusStepCard(
                stepNumber = "1",
                title = "Aktifkan di Pengaturan Sistem",
                description = "Izinkan Keyboard iO di daftar metode masukan perangkat Anda.",
                isCompleted = isImeEnabled,
                actionLabel = if (isImeEnabled) "Sudah Aktif" else "Buka Pengaturan",
                rgbTheme = rgbTheme,
                onAction = onOpenSettings,
                testTag = "step_1_activate_button"
            )
        }

        // Step 2 Card
        item {
            StatusStepCard(
                stepNumber = "2",
                title = "Pilih Keyboard iO sebagai Metode Default",
                description = "Ubah metode masukan aktif saat ini ke Keyboard iO.",
                isCompleted = isImeSelected,
                actionLabel = if (isImeSelected) "Keyboard iO Aktif" else "Pilih Keyboard",
                rgbTheme = rgbTheme,
                onAction = onOpenPicker,
                testTag = "step_2_select_button"
            )
        }

        // Live Typing Tester
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Uji Coba Ketikan Langsung",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(rgbTheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${testText.length} karakter",
                                color = rgbTheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ketuk kotak di bawah untuk memunculkan Keyboard iO dan mencoba fitur QWERTY dinamis, tekan lama (long-press), dan efek RGB.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = testText,
                        onValueChange = onTestTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("test_input_field"),
                        placeholder = {
                            Text(
                                text = "Ketik sesuatu di sini...",
                                color = TextTertiary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = rgbTheme.primary,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = rgbTheme.primary,
                            focusedContainerColor = CyberBlack,
                            unfocusedContainerColor = CyberBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (testText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Bersihkan",
                                color = rgbTheme.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onTestTextChange("") }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // App Signature & Identity Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = rgbTheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Identitas & Keystore",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Package: dev.andikune.keyboardio • v1.0.0 (1)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Signer Alias: Andik Une • GitHub Actions CI/CD Terintegrasi",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    actionLabel: String,
    rgbTheme: RgbThemeStyle,
    onAction: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) CyberCard else CyberDarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCompleted) 1.2.dp else 0.8.dp,
            color = if (isCompleted) NeonGreen.copy(alpha = 0.7f) else CyberBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) NeonGreen.copy(alpha = 0.2f) else CyberKeyBg
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selesai",
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
                        color = rgbTheme.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) CyberKeyBg else rgbTheme.primary,
                        contentColor = if (isCompleted) TextPrimary else CyberBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag(testTag)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomizationTab(
    preferences: KeyboardPreferences,
    rgbTheme: RgbThemeStyle,
    heightScale: Float,
    oneHandedMode: OneHandedMode,
    isFloating: Boolean,
    hapticEnabled: Boolean,
    soundEnabled: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme selection
        item {
            Text(
                text = "Tema & Efek Cahaya Neon RGB",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(RgbThemeStyle.entries) { theme ->
                    val isSelected = theme == rgbTheme
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CyberCard else CyberDarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 0.8.dp,
                            color = if (isSelected) theme.primary else CyberBorder
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .clickable { preferences.setTheme(theme) }
                            .testTag("theme_card_${theme.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(theme.primary, theme.secondary, theme.accent)
                                        )
                                    )
                            ) {}
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = theme.displayName,
                                color = if (isSelected) theme.primary else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Height adjustment
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tinggi Keyboard: ${(heightScale * 100).toInt()}%",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = heightScale,
                        onValueChange = { preferences.setHeightScale(it) },
                        valueRange = 0.75f..1.35f,
                        colors = SliderDefaults.colors(
                            thumbColor = rgbTheme.primary,
                            activeTrackColor = rgbTheme.primary,
                            inactiveTrackColor = CyberKeyBg
                        )
                    )
                }
            }
        }

        // Layout Modes: One-Handed & Floating
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Mode Tata Letak",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // One-handed buttons
                    Text(
                        text = "Mode Satu Tangan (One-Handed)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            OneHandedMode.OFF to "Mati",
                            OneHandedMode.LEFT to "Kiri",
                            OneHandedMode.RIGHT to "Kanan"
                        ).forEach { (mode, label) ->
                            val isSelected = oneHandedMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) rgbTheme.primary else CyberKeyBg)
                                    .clickable { preferences.setOneHandedMode(mode) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) CyberBlack else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Floating toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Keyboard Mengambang (Floating)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Keyboard dapat digeser bebas di atas layar",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isFloating,
                            onCheckedChange = { preferences.setFloating(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }
        }

        // Feedback Toggles: Haptic & Sound
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Umpan Balik Ketikan (Feedback)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Getar (Haptic Feedback)",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { preferences.setHaptic(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Suara Tombol Mekanikal",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { preferences.setSound(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipboardTab(
    items: List<dev.andikune.keyboardio.model.ClipboardItem>,
    rgbTheme: RgbThemeStyle,
    onTogglePin: (Long) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Riwayat Papan Klip (${items.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Teks yang disalin otomatis tersimpan untuk ditempel cepat.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (items.any { !it.isPinned }) {
                Button(
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberKeyBg,
                        contentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Bersihkan", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada item di papan klip.\nSalin teks dari aplikasi mana pun untuk melihatnya di sini.",
                    color = TextTertiary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (item.isPinned) 1.2.dp else 0.6.dp,
                            color = if (item.isPinned) rgbTheme.primary else CyberBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.text,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (item.isPinned) "Tersemat" else "Sematkan",
                                color = if (item.isPinned) rgbTheme.primary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onTogglePin(item.id) }
                                    .padding(6.dp)
                            )
                            Text(
                                text = "Hapus",
                                color = NeonMagenta,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onDeleteItem(item.id) }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
