package dev.andikune.keyboardio

import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.rememberCoroutineScope
import dev.andikune.keyboardio.data.TypingActivityStore
import dev.andikune.keyboardio.data.TypingSessionRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import dev.andikune.keyboardio.data.CrashReportStore
import dev.andikune.keyboardio.data.KeyboardPreferences
import dev.andikune.keyboardio.data.NicknameFontGenerator
import dev.andikune.keyboardio.data.OfflineTranslator
import dev.andikune.keyboardio.model.OneHandedMode
import dev.andikune.keyboardio.model.RgbThemeStyle
import dev.andikune.keyboardio.model.SoundEffectType
import dev.andikune.keyboardio.model.SpacebarStyle
import dev.andikune.keyboardio.service.KeyboardAudioFeedback
import dev.andikune.keyboardio.service.UpdateService
import dev.andikune.keyboardio.ui.UpdatePrompt
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

    private companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var preferences: KeyboardPreferences
    private lateinit var clipboardRepo: ClipboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = KeyboardPreferences(applicationContext)
        clipboardRepo = ClipboardRepository.getInstance(applicationContext)

        setContent {
            KeyboardIOTheme {
                MainScreen(
                    preferences = preferences,
                    clipboardRepo = clipboardRepo,
                    onOpenSettings = {
                        runCatching {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        }.onFailure { exception ->
                            Log.e(TAG, "Gagal membuka pengaturan input method", exception)
                        }
                    },
                    onOpenPicker = {
                        runCatching {
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        }.onFailure { exception ->
                            Log.e(TAG, "Gagal membuka picker input method", exception)
                        }
                    }
                )
            }
        }

        UpdateService.check(this)

        showLastCrashReportIfPresent()
    }

    private fun showLastCrashReportIfPresent() {
        val report = CrashReportStore(this).read() ?: return
        window.decorView.post {
            val copyButton = Button(this).apply {
                text = "Salin log cepat"
                isAllCaps = false
            }
            val clearButton = Button(this).apply {
                text = "Hapus log tersimpan"
                isAllCaps = false
            }
            val textView = TextView(this).apply {
                text = report
                setTextIsSelectable(true)
                setPadding(32, 0, 32, 0)
            }
            val scrollView = ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                addView(textView)
            }
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 16, 0)
                addView(copyButton)
                addView(clearButton)
                addView(scrollView)
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle("Crash terakhir terdeteksi")
                .setMessage("Salin log ini lalu kirimkan untuk dianalisis. Teks juga dapat ditekan lama untuk memilih sebagian.")
                .setView(content)
                .setNegativeButton("Tutup", null)
                .create()

            copyButton.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Keyboard iO crash report", report))
                Toast.makeText(this, "Crash log sudah disalin", Toast.LENGTH_SHORT).show()
            }
            clearButton.setOnClickListener {
                CrashReportStore(this).clear()
                dialog.dismiss()
            }
            dialog.show()
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
    val longPressDelay by preferences.longPressDelay.collectAsState()
    val clipboardList by clipboardRepo.items.collectAsState()

    fun checkKeyboardStatus() {
        runCatching {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                ?: return@runCatching
            val myPackage = context.packageName
            isImeEnabled = imm.enabledInputMethodList.any { it.packageName == myPackage }

            val currentDefault = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            isImeSelected = currentDefault?.substringBefore('/') == myPackage
        }.onFailure { exception ->
            // Beberapa OEM dapat menolak query ini; layar tetap dapat digunakan.
            Log.e("MainScreen", "Gagal membaca status Keyboard iO", exception)
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
                    rgbTheme = rgbTheme
                )
                2 -> ClipboardTab(
                    items = clipboardList,
                    rgbTheme = rgbTheme,
                    onTogglePin = { clipboardRepo.togglePin(it) },
                    onDeleteItem = { clipboardRepo.removeItem(it) },
                    onClearAll = { clipboardRepo.clearUnpinned() }
                )
            }

            UpdatePrompt()
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
    rgbTheme: RgbThemeStyle
) {
    val heightScale by preferences.heightScale.collectAsState()
    val oneHandedMode by preferences.oneHandedMode.collectAsState()
    val isFloating by preferences.isFloating.collectAsState()
    val hapticEnabled by preferences.hapticFeedback.collectAsState()
    val soundEnabled by preferences.keySound.collectAsState()
    val longPressDelay by preferences.longPressDelay.collectAsState()
    val soundType by preferences.soundType.collectAsState()
    val spacebarStyle by preferences.spacebarStyle.collectAsState()
    val smartBackspace by preferences.smartBackspace.collectAsState()
    val autoCapitalize by preferences.autoCapitalize.collectAsState()
    val autoCorrection by preferences.autoCorrection.collectAsState()
    val swipeTyping by preferences.swipeTyping.collectAsState()
    val auroraWaveEffect by preferences.auroraWaveEffect.collectAsState()
    val keyPopupEffect by preferences.keyPopupEffect.collectAsState()
    val customBgType by preferences.customBgType.collectAsState()
    val customBgPath by preferences.customBgPath.collectAsState()
    val customBgOpacity by preferences.customBgOpacity.collectAsState()
    val customBgDim by preferences.customBgDim.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val typingActivityStore = remember { TypingActivityStore(context) }
    val typingSessions by typingActivityStore.sessions.collectAsState()
    val typingStats = remember(typingSessions) { typingActivityStore.getStats() }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                var pendingTempFile: File? = null
                var pendingDestinationFile: File? = null
                runCatching {
                    val mimeType = context.contentResolver.getType(uri)
                        ?.substringBefore(';')
                        ?.trim()
                        ?.lowercase(Locale.ROOT)
                        .orEmpty()
                    val isVideo = when {
                        mimeType.startsWith("video/") -> true
                        mimeType.startsWith("image/") -> false
                        else -> throw IllegalArgumentException(
                            "Format wallpaper tidak dikenali. Pilih file gambar atau video yang didukung."
                        )
                    }
                    // Unique path forces Compose to reload replaced wallpaper media.
                    val fileId = java.util.UUID.randomUUID().toString()
                    val extension = if (isVideo) "mp4" else "img"
                    val fileName = "custom_keyboard_bg_$fileId.$extension"
                    val destFile = File(context.filesDir, fileName)
                    val tempFile = File(context.filesDir, "$fileName.tmp")
                    pendingTempFile = tempFile
                    pendingDestinationFile = destFile
                    val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                            output.fd.sync()
                        }
                        true
                    } ?: false
                    if (!copied || !tempFile.exists() || tempFile.length() == 0L) {
                        tempFile.delete()
                        throw IllegalStateException("File media kosong atau tidak dapat dibaca.")
                    }
                    if (!tempFile.renameTo(destFile)) {
                        tempFile.copyTo(destFile, overwrite = true)
                        tempFile.delete()
                    }
                    preferences.setCustomBackground(
                        type = if (isVideo) "VIDEO" else "IMAGE",
                        path = destFile.absolutePath,
                        opacity = 1f,
                        dim = 0f
                    )
                    pendingTempFile = null
                    pendingDestinationFile = null
                }.onFailure {
                    pendingTempFile?.delete()
                    pendingDestinationFile?.delete()
                    Log.e("MainActivity", "Gagal menyimpan latar belakang kustom", it)
                }
            }
        }
    }

    val audioFeedback = remember { KeyboardAudioFeedback(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioFeedback.release()
        }
    }

    // Test text state for translator & nickname fonts showcase
    var sampleNickInput by remember { mutableStateOf("Sultan Pro") }
    var sampleTranslateInput by remember { mutableStateOf("Selamat pagi") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Theme selection
        item {
            Text(
                text = "Tema & Efek Cahaya Neon RGB",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pilih kombinasi warna neon futuristik untuk papan tombol Anda.",
                color = TextSecondary,
                fontSize = 11.sp
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

        // 1b. Custom Background (Foto & Video dari Galeri)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Latar Belakang Kustom",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gunakan foto atau video dari galeri HP dengan efek peredup",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (customBgType != "NONE") {
                            Text(
                                text = "Hapus",
                                color = NeonMagenta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { preferences.clearCustomBackground() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (customBgType == "IMAGE") rgbTheme.primary else CyberKeyBg,
                                contentColor = if (customBgType == "IMAGE") CyberBlack else TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (customBgType == "VIDEO") rgbTheme.secondary else CyberKeyBg,
                                contentColor = if (customBgType == "VIDEO") CyberBlack else TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (customBgType != "NONE" && customBgPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Status: Aktif (${if (customBgType == "VIDEO") "Video Loop" else "Foto Statis"})",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Keterlihatan Foto/Video: ${(customBgOpacity * 100).toInt()}%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = customBgOpacity,
                            onValueChange = { preferences.setCustomBgOpacity(it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = rgbTheme.primary,
                                activeTrackColor = rgbTheme.primary
                            )
                        )
                        Text(
                            text = "100% = media terlihat penuh; 0% = media tidak terlihat.",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )

                        Text(
                            text = "Lapisan Gelap: ${(customBgDim * 100).toInt()}%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = customBgDim,
                            onValueChange = { preferences.setCustomBgDim(it) },
                            valueRange = 0.0f..0.75f,
                            colors = SliderDefaults.colors(
                                thumbColor = rgbTheme.secondary,
                                activeTrackColor = rgbTheme.secondary
                            )
                        )
                        Text(
                            text = "0% = tanpa lapisan gelap. Gunakan nilai kecil agar foto/video tetap jelas.",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Preset cepat", color = TextSecondary, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    preferences.setCustomBgOpacity(1f)
                                    preferences.setCustomBgDim(0f)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberKeyBg)
                            ) { Text("Terang", fontSize = 10.sp) }
                            Button(
                                onClick = {
                                    preferences.setCustomBgOpacity(0.95f)
                                    preferences.setCustomBgDim(0.03f)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberKeyBg)
                            ) { Text("Seimbang", fontSize = 10.sp) }
                            Button(
                                onClick = {
                                    preferences.setCustomBgOpacity(0.85f)
                                    preferences.setCustomBgDim(0.18f)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberKeyBg)
                            ) { Text("Redup", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }

        // 1c. Riwayat Aktivitas Mengetik (Auto-reset 24 Jam)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = rgbTheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Riwayat Aktivitas Mengetik",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (typingSessions.isNotEmpty()) {
                            Text(
                                text = "Bersihkan",
                                color = rgbTheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { typingActivityStore.clearAll() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Statistik 24 jam terakhir dari saat keyboard dibuka hingga ditutup. Otomatis terhapus setelah 24 jam.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${typingStats.totalCharacters}",
                                    color = rgbTheme.primary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Karakter", color = TextTertiary, fontSize = 10.sp)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${typingStats.totalWords}",
                                    color = rgbTheme.secondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Kata", color = TextTertiary, fontSize = 10.sp)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${typingStats.totalSessions}",
                                    color = rgbTheme.accent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Sesi", color = TextTertiary, fontSize = 10.sp)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val seconds = typingStats.totalDurationMs / 1000
                                val timeStr = if (seconds < 60) "${seconds}d" else "${seconds / 60}m"
                                Text(
                                    text = timeStr,
                                    color = NeonGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Durasi", color = TextTertiary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Visual Effects & Animations (Aurora Wave, Key Popup, Swipe Typing)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Efek Visual & Animasi Neon",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Aurora Wave Effect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Efek Gelombang Aurora (Aurora Wave)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Menyinari tombol tetangga secara bergelombang saat tombol ditekan (tanpa latar belakang)",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = auroraWaveEffect,
                            onCheckedChange = { preferences.setAuroraWaveEffect(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Popup Overlay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Key Pop-up (Overlay Mengambang)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Gelembung huruf neon mengambang tepat di atas tombol yang sedang ditekan",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = keyPopupEffect,
                            onCheckedChange = { preferences.setKeyPopupEffect(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Swipe Typing Trail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Ketik Geser (Gesture / Swipe Typing)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Mengetik cepat dengan menggeser jari antar huruf disertai jejak cahaya neon",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = swipeTyping,
                            onCheckedChange = { preferences.setSwipeTyping(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }
        }

        // 3. Kustomisasi Ikon & Tampilan Tombol Spasi (Spacebar)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Kustomisasi Tombol Spasi (Spacebar)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pilih label atau ikon futuristik pada tombol spasi",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpacebarStyle.entries.forEach { style ->
                            val isSelected = spacebarStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) rgbTheme.primary else CyberKeyBg)
                                    .clickable { preferences.setSpacebarStyle(style) }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style.displayLabel,
                                    color = if (isSelected) CyberBlack else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Haptic & Audio Feedback with Sound Variety
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Pengaturan Getar & Suara (Haptic & Audio Feedback)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Suara Ketikan Tombol",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = {
                                preferences.setSound(it)
                                if (it) {
                                    audioFeedback.playSound(soundType)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }

                    if (soundEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Pilihan Variasi Jenis Suara Ketikan:",
                            color = rgbTheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                SoundEffectType.MECHANICAL to "Mekanikal (Tactile Click)",
                                SoundEffectType.TYPEWRITER to "Mesin Tik (Classic Strike)",
                                SoundEffectType.DIGITAL_POP to "Digital Pop (Futuristic Beep)",
                                SoundEffectType.STANDARD to "Standard Click"
                            ).forEach { (type, label) ->
                                val isSelected = soundType == type
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberDarkSurface else CyberKeyBg)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) rgbTheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            preferences.setSoundType(type)
                                            audioFeedback.playSound(type)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) rgbTheme.primary else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• Aktif",
                                                color = rgbTheme.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) rgbTheme.primary.copy(alpha = 0.2f) else CyberDarkSurface)
                                            .clickable { audioFeedback.playSound(type) }
                                            .padding(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Uji Suara",
                                            tint = rgbTheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Smart Typing Features (Smart Backspace, Auto-Capitalize, Auto-Correct)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Kecerdasan Mengetik (Smart Typing)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Capitalization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Auto Kapitalisasi",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Kapitalisasi otomatis di awal kalimat atau setelah tanda baca titik",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoCapitalize,
                            onCheckedChange = { preferences.setAutoCapitalize(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Smart Auto-Correction
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Koreksi Otomatis & Typo (Smart Auto-Correction)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Mendeteksi dan mengoreksi typo secara cerdas menggunakan jarak Levenshtein saat tombol spasi ditekan",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoCorrection,
                            onCheckedChange = { preferences.setAutoCorrection(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Smart Backspace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Hapus Otomatis Pintar (Smart Backspace)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Menghapus kata per kata atau karakter dengan swipe/hold cerdas",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = smartBackspace,
                            onCheckedChange = { preferences.setSmartBackspace(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = rgbTheme.primary,
                                checkedTrackColor = rgbTheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Long-Press delay adjustment
                    Text(
                        text = "Jeda Tahan Tombol (Long-Press): ${longPressDelay}ms",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Kecepatan munculnya karakter sekunder saat tombol huruf ditahan.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = longPressDelay.toFloat(),
                        onValueChange = { preferences.setLongPressDelay(it.toLong()) },
                        valueRange = 200f..600f,
                        colors = SliderDefaults.colors(
                            thumbColor = rgbTheme.primary,
                            activeTrackColor = rgbTheme.primary,
                            inactiveTrackColor = CyberKeyBg
                        )
                    )
                }
            }
        }

        // 6. Layout & Sizing (Height, One-Handed, Floating)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tata Letak & Ukuran Keyboard",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Tinggi Keyboard: ${(heightScale * 100).toInt()}%",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = heightScale,
                        onValueChange = { preferences.setHeightScale(it) },
                        valueRange = 1.0f..1.35f,
                        colors = SliderDefaults.colors(
                            thumbColor = rgbTheme.primary,
                            activeTrackColor = rgbTheme.primary,
                            inactiveTrackColor = CyberKeyBg
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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

        // 7. Showcase & Live Previews for Voice, Translator & Fancy Nickname Fonts
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Fitur Cerdas Unggulan Keyboard iO",
                        color = rgbTheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Speech-to-Text highlight
                    Text(
                        text = "🎙️ Input Suara (Dikte Lokal On-Device):",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Terletak tepat di samping teks 'iO' pada baris atas keyboard. Berfungsi secara lokal tanpa memerlukan API Key eksternal.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Offline translation test
                    Text(
                        text = "🌐 Terjemahan Lokal (Offline ID ↔ EN):",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Coba kata: \"${sampleTranslateInput}\" -> Hasil: \"${OfflineTranslator.translateIdToEn(sampleTranslateInput)}\"",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fancy font nickname showcase
                    Text(
                        text = "✨ Variasi Font & Gaya Nickname Game Sultan:",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val sultanPreview = NicknameFontGenerator.convertText(sampleNickInput, "gamer_wing")
                    val gothicPreview = NicknameFontGenerator.convertText(sampleNickInput, "gothic")
                    Text(
                        text = "Sultan: $sultanPreview\nGothic: $gothicPreview",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
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
