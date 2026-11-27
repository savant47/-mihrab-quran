package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.example.data.local.AdhkarCustomizationManager
import com.example.data.local.UnifiedDhikrItem
import com.example.ui.components.AddOrEditDhikrDialog
import com.example.ui.components.ComprehensiveSettingsAndGuideTab
import com.example.ui.components.DhikrOrderArrowsPill
import com.example.ui.components.FavoriteHeartCrimson
import com.example.ui.components.QuranIntentionRenewalButton
import com.example.ui.components.QuranIntentionRenewalDialog
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.ContextCompat
import com.example.ai.MushafCameraRecognizer
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.runtime.collectAsState
import com.example.data.KhatmaMetrics
import com.example.data.SpiritualMessageItem
import com.example.data.local.LocalKhatmaStorage
import com.example.ui.components.AlarmTriggerChip
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.DoveAndMessageIcon
import com.example.ui.components.MihrabiLogo
import com.example.ui.components.MorningEveningAdhkarBox
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Color
import com.example.ui.components.SpiritualStartupMessageDialog
import com.example.ui.components.WAQF_LINE_1
import com.example.ui.components.WAQF_LINE_2
import com.example.ui.components.WaqfDedicationDialog
import com.example.ui.components.shareSpiritualMessage
import com.example.data.QuranDataHelper
import com.example.data.model.KhatmaTrack
import com.example.data.model.ReadingLog
import com.example.data.model.User
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    user: User,
    activeTrack: KhatmaTrack,
    tracks: List<KhatmaTrack>,
    metrics: KhatmaMetrics,
    latestReflection: String,
    readingLogs: List<ReadingLog>,
    isGeneratingReflection: Boolean,
    showCompletionDialog: Boolean,
    postLogReflectionData: com.example.ui.viewmodel.ReflectionDialogData? = null,
    onDismissPostLogReflectionDialog: () -> Unit = {},
    onSelectTrack: (String) -> Unit,
    onCreateTrack: (String) -> Unit,
    onCreateFixedSurahTrack: (trackName: String, surahName: String, startPage: Int) -> Unit = { _, _, _ -> },
    onDeleteTrack: (String) -> Unit = {},
    onRenameTrack: (String, String) -> Unit,
    onAutoSaveStopPoint: (surah: String, ayah: Int, page: Int) -> Unit = { _, _, _ -> },
    onLogProgress: (surah: String, ayah: Int, page: Int, pagesReadToday: Int) -> Unit,
    onStartNextKhatma: () -> Unit,
    onDismissCompletionDialog: () -> Unit
) {
    val context = LocalContext.current
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }
    val spiritualMessages by localStorage.spiritualMessagesFlow.collectAsState()
    val cumulativeAdhkarTasbeehTotal by localStorage.cumulativeAdhkarTasbeehFlow.collectAsState()
    var activeStartupMessage by remember { mutableStateOf<SpiritualMessageItem?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var selectedHistorySubTab by remember { mutableIntStateOf(0) } // 0: قراءات المصحف, 1: رسائل وأدعية الفتح

    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: الورد, 1: المصحف, 2: مواقيت الصلاة والقبلة, 3: السجل
    val quranPrefs = remember { context.getSharedPreferences("quran_reader_prefs", Context.MODE_PRIVATE) }
    var quranTargetPage by remember { mutableIntStateOf(activeTrack.currentPage) }

    var lastReadPage by remember(activeTrack) { mutableIntStateOf(activeTrack.currentPage) }
    var lastReadSurah by remember(activeTrack) { mutableStateOf(activeTrack.currentSurah) }
    var lastReadAyah by remember(activeTrack) { mutableIntStateOf(activeTrack.currentAyah) }

    LaunchedEffect(Unit) {
        com.example.alarm.PrayerTimesAndAdhanManager.scheduleAllFivePrayers(context)
        localStorage.consumeStartupMessageForSessionIfNeeded()?.let { msg ->
            activeStartupMessage = msg
        }
    }

    LaunchedEffect(selectedBottomTab, activeTrack.trackId, activeTrack.currentPage, activeTrack.currentSurah, activeTrack.currentAyah) {
        if (activeTrack.isFixedSurahWerd) {
            val fixedPage = activeTrack.fixedSurahStartPage.coerceIn(1, 604)
            val fixedSurah = activeTrack.fixedSurahName.ifBlank { activeTrack.currentSurah }
            lastReadPage = fixedPage
            lastReadSurah = fixedSurah
            lastReadAyah = 1
            quranTargetPage = fixedPage
        } else {
            val tId = activeTrack.trackId
            val authoritativePage = activeTrack.currentPage.coerceIn(1, 604)
            val authoritativeSurah = activeTrack.currentSurah.ifBlank { "الفاتحة" }
            val authoritativeAyah = activeTrack.currentAyah.coerceAtLeast(1)
            lastReadPage = authoritativePage
            lastReadSurah = authoritativeSurah
            lastReadAyah = authoritativeAyah
            quranTargetPage = authoritativePage
            localStorage.syncTrackToQuranReaderPrefs(
                trackId = tId,
                surah = authoritativeSurah,
                ayah = authoritativeAyah,
                page = authoritativePage
            )
        }
    }

    var showInputDialog by remember { mutableStateOf(false) }
    var autoTriggerCameraInDialog by remember { mutableStateOf(false) }
    var isMushafFullScreen by remember { mutableStateOf(false) }
    var showCreateTrackDialog by remember { mutableStateOf(false) }
    var showRenameTrackDialog by remember { mutableStateOf(false) }
    var showWaqfDedicationDialog by remember { mutableStateOf(false) }
    var showIntentionRenewalDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedBottomTab == 1 && isMushafFullScreen) {
        isMushafFullScreen = false
    }

    Scaffold(
        topBar = {
            if (selectedBottomTab != 1) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        // Row 1: App Emblem & Title + Quick Actions (Spiritual Message + Backup)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                MihrabiLogo(
                                    size = 34.dp,
                                    showIntegratedText = false,
                                    animatedGlow = false
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "محرابي",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldGreen,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• وِردُكَ ومحرابُك",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Quick Startup Spiritual Message Button (🕊️✉️)
                                IconButton(
                                    onClick = {
                                        val latest = spiritualMessages.firstOrNull()
                                            ?: localStorage.recordNextStartupMessage()
                                        activeStartupMessage = latest
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("top_spiritual_message_button")
                                ) {
                                    DoveAndMessageIcon(modifier = Modifier.size(21.dp))
                                }

                                // Backup & Restore Progress Button
                                OutlinedButton(
                                    onClick = { showBackupDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldAccent),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFFFDF8EC),
                                        contentColor = EmeraldGreen
                                    ),
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("top_backup_restore_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = "نسخ احتياطي واستعادة",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "نسخ احتياطي",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // Row 2: Full-Width Unclipped Light Mushaf Waqf & Sadaqah Jariyah Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF3E9D2),
                            border = BorderStroke(0.9.dp, GoldBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showWaqfDedicationDialog = true }
                                .testTag("waqf_dedication_banner")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = WAQF_LINE_1,
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    lineHeight = 14.5.sp,
                                    color = EmeraldGreen,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = WAQF_LINE_2,
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    color = Color(0xFF1A110B),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!(selectedBottomTab == 1 && isMushafFullScreen)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = selectedBottomTab == 0,
                        onClick = { selectedBottomTab = 0 },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                        label = { Text("الورد والتدبر", fontSize = 11.sp, maxLines = 1) }
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 1,
                        onClick = { selectedBottomTab = 1 },
                        icon = { Icon(imageVector = Icons.Default.MenuBook, contentDescription = null) },
                        label = { Text("المصحف الشريف", fontSize = 11.sp, maxLines = 1) }
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 2,
                        onClick = { selectedBottomTab = 2 },
                        icon = { Icon(imageVector = Icons.Default.Explore, contentDescription = null) },
                        label = { Text("مواقيت الصلاة والقبلة", fontSize = 10.5.sp, maxLines = 1) }
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 3,
                        onClick = { selectedBottomTab = 3 },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "إعدادات") },
                        label = { Text("⚙️ إعدادات", fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedBottomTab) {
                0 -> {
                    // Dashboard View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Prominent Button: دعاء تجديد نوايا قراءة القرآن الكريم
                        QuranIntentionRenewalButton(
                            onClick = { showIntentionRenewalDialog = true }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Multi-Track Selector Row (Concise & High Contrast)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الأوراد المخصصة",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showCreateTrackDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("add_track_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إضافة ورد",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tracks) { track ->
                                val isSelected = track.trackId == activeTrack.trackId
                                val targetStartPage = if (track.isFixedSurahWerd) {
                                    track.fixedSurahStartPage.coerceIn(1, 604)
                                } else {
                                    track.currentPage.coerceIn(1, 604)
                                }
                                val targetSurahLabel = if (track.isFixedSurahWerd) {
                                    "سورة ${track.fixedSurahName.ifBlank { track.currentSurah }} • ص $targetStartPage"
                                } else {
                                    "ص ${track.currentPage} • ${track.currentSurah}"
                                }

                                Card(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onSelectTrack(track.trackId) }
                                        .border(
                                            width = if (isSelected) 1.8.dp else 0.8.dp,
                                            color = if (isSelected) EmeraldGreen else GoldBorder,
                                            shape = RoundedCornerShape(18.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFEBE1C9) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = track.trackName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (track.isFixedSurahWerd) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = GoldAccent.copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = "سورة ثابتة",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = GoldAccent,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = targetSurahLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.5.sp,
                                                color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Quick Jump Button to open this Track directly in the Quran
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldGreen,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    onSelectTrack(track.trackId)
                                                    quranTargetPage = targetStartPage
                                                    lastReadPage = targetStartPage
                                                    lastReadSurah = track.currentSurah
                                                    lastReadAyah = track.currentAyah
                                                    if (!track.isFixedSurahWerd) {
                                                        localStorage.syncTrackToQuranReaderPrefs(
                                                            trackId = track.trackId,
                                                            surah = track.currentSurah,
                                                            ayah = track.currentAyah,
                                                            page = targetStartPage
                                                        )
                                                    }
                                                    selectedBottomTab = 1
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.MenuBook,
                                                    contentDescription = "فتح في المصحف",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل الورد",
                                                tint = GoldAccent,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .clickable { showRenameTrackDialog = true }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        AlarmTriggerChip(
                                            targetId = "werd_${track.trackId}",
                                            defaultTitle = "منبه ورد: ${track.trackName}",
                                            defaultSubtitle = "حان الآن موعد قراءة وردك القرآني (${track.trackName})",
                                            defaultHour = 20,
                                            defaultMinute = 0,
                                            compact = true
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (activeTrack.isFixedSurahWerd) {
                            // Dedicated Quick-Surah Werd Card (e.g., سورة الكهف كل جمعة - بدون احتسابها كتقدم ختمة)
                            val fixedSurah = activeTrack.fixedSurahName.ifBlank { activeTrack.currentSurah }
                            val fixedPage = activeTrack.fixedSurahStartPage.coerceIn(1, 604)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.2.dp, GoldAccent, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFEBE1C9),
                                        border = BorderStroke(1.dp, GoldAccent)
                                    ) {
                                        Text(
                                            text = "📖 ورد سورة مخصصة (وصول سريع)",
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                        )
                                    }

                                    Text(
                                        text = "${activeTrack.trackName} • سورة $fixedSurah",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = EmeraldGreen,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "يفتح المصحف دائماً على بداية سورة $fixedSurah (صفحة $fixedPage) دون تغيير موضعها أو احتسابها كختمة",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )

                                    Button(
                                        onClick = {
                                            quranTargetPage = fixedPage
                                            selectedBottomTab = 1
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("resume_track_primary_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldGreen,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "قراءة سورة $fixedSurah الآن (ص $fixedPage)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }

                                    AlarmTriggerChip(
                                        targetId = "werd_${activeTrack.trackId}",
                                        defaultTitle = "منبه ${activeTrack.trackName} (سورة $fixedSurah)",
                                        defaultSubtitle = "تذكير بقراءة سورة $fixedSurah المباركة (صفحة $fixedPage)",
                                        defaultHour = 10,
                                        defaultMinute = 0,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            // Unified Circular Progress & Telemetry Card for Full Khatma Track
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GoldBorder, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${activeTrack.trackName} • الختمة ${activeTrack.khatmaNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Unified Row: Right Capsule (المعدل اليومي) + Progress Circle (with المتبقي للختم) + Left Capsule (ختام المصحف)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Side Capsule 1: المعدل اليومي للقراءة
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(122.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFF3E9D2),
                                            border = BorderStroke(1.dp, GoldBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 5.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Speed,
                                                        contentDescription = null,
                                                        tint = GoldAccent,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    KhatmaCapsuleSingleLineText(
                                                        text = "المعدل اليومي",
                                                        initialFontSizeSp = 11f,
                                                        minFontSizeSp = 8.5f,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldGreen
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFDF8EC),
                                                    border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.65f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 4.dp, vertical = 5.dp)
                                                    ) {
                                                        KhatmaCapsuleSingleLineText(
                                                            text = "${metrics.actualDailyPacePages} صفحة/يوم",
                                                            initialFontSizeSp = 11.8f,
                                                            minFontSizeSp = 8.5f,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF1A110B)
                                                        )
                                                    }
                                                }

                                                KhatmaCapsuleSingleLineText(
                                                    text = "~1 ربع/يوم",
                                                    initialFontSizeSp = 10.5f,
                                                    minFontSizeSp = 8.5f,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1A110B).copy(alpha = 0.75f)
                                                )
                                            }
                                        }

                                        // Center Progress Circle (Includes Percentage + Current Page + المتبقي للختم)
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(122.dp)
                                        ) {
                                            val strokeWidth = 9.5.dp
                                            val primaryColor = EmeraldGreen
                                            val backgroundColor = GoldBorder.copy(alpha = 0.35f)
                                            val progressSweep = (metrics.progressPercentage / 100.0 * 360.0).toFloat()

                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArc(
                                                    color = backgroundColor,
                                                    startAngle = 0f,
                                                    sweepAngle = 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                                                )
                                                drawArc(
                                                    color = primaryColor,
                                                    startAngle = -90f,
                                                    sweepAngle = progressSweep,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = "${metrics.progressPercentage}%",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = EmeraldGreen,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Text(
                                                    text = "${metrics.currentPage} / 604 ص",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = "متبقي ${metrics.remainingPages} ص",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldGreen,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Text(
                                                    text = "(${metrics.remainingRubCount} ربع حزب)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Side Capsule 2: الموعد المتوقع لختام المصحف
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(122.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFF3E9D2),
                                            border = BorderStroke(1.dp, GoldBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 5.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarToday,
                                                        contentDescription = null,
                                                        tint = GoldAccent,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    KhatmaCapsuleSingleLineText(
                                                        text = "ختام المصحف",
                                                        initialFontSizeSp = 11f,
                                                        minFontSizeSp = 8.5f,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldGreen
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFDF8EC),
                                                    border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.65f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 4.dp, vertical = 5.dp)
                                                    ) {
                                                        // Wrap date in LTR directional isolate (\u2066..\u2069) and force single line
                                                        KhatmaCapsuleSingleLineText(
                                                            text = "\u2066${metrics.projectedCompletionDate}\u2069",
                                                            initialFontSizeSp = 12.2f,
                                                            minFontSizeSp = 8.5f,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF1A110B)
                                                        )
                                                    }
                                                }

                                                KhatmaCapsuleSingleLineText(
                                                    text = "الموعد المتوقع",
                                                    initialFontSizeSp = 10.5f,
                                                    minFontSizeSp = 8.5f,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1A110B).copy(alpha = 0.75f)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Concise, Unclipped Action Buttons for Active Khatma Track
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Primary Button 1: متابعة القراءة (Includes Page + Surah + Ayah without separate card)
                                        Button(
                                            onClick = {
                                                val target = activeTrack.currentPage.coerceIn(1, 604)
                                                quranTargetPage = target
                                                lastReadPage = target
                                                lastReadSurah = activeTrack.currentSurah
                                                lastReadAyah = activeTrack.currentAyah
                                                localStorage.syncTrackToQuranReaderPrefs(
                                                    trackId = activeTrack.trackId,
                                                    surah = activeTrack.currentSurah,
                                                    ayah = activeTrack.currentAyah,
                                                    page = target
                                                )
                                                selectedBottomTab = 1
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 46.dp)
                                                .testTag("resume_track_primary_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldGreen,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "متابعة القراءة (ص ${activeTrack.currentPage} • ${activeTrack.currentSurah} • آية ${activeTrack.currentAyah})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                softWrap = true
                                            )
                                        }

                                        // Secondary Row: تسجيل الوقف | تصوير الصفحة
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    autoTriggerCameraInDialog = false
                                                    showInputDialog = true
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .defaultMinSize(minHeight = 44.dp)
                                                    .testTag("log_progress_button"),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                border = BorderStroke(1.2.dp, EmeraldGreen),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color(0xFFFDF8EC),
                                                    contentColor = EmeraldGreen
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bookmark,
                                                    contentDescription = null,
                                                    tint = EmeraldGreen,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "تسجيل الوقف",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = EmeraldGreen,
                                                    maxLines = 1
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    autoTriggerCameraInDialog = true
                                                    showInputDialog = true
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .defaultMinSize(minHeight = 44.dp)
                                                    .testTag("camera_stop_point_button"),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = RoyalNavyDark,
                                                    contentColor = EmeraldGreen
                                                ),
                                                border = BorderStroke(1.dp, GoldAccent)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = "تصوير الصفحة",
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "تصوير الصفحة",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = EmeraldGreen,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // Dedicated Alarm Reminder Button for the Active Werd Track
                                        AlarmTriggerChip(
                                            targetId = "werd_${activeTrack.trackId}",
                                            defaultTitle = "منبه الورد (${activeTrack.trackName})",
                                            defaultSubtitle = "تذكير بقراءة وردك اليومي (${activeTrack.trackName} - صفحة ${activeTrack.currentPage})",
                                            defaultHour = 20,
                                            defaultMinute = 0,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Morning & Evening Adhkar Box (2 Buttons + Complete Offline Hisn Al-Muslim)
                        MorningEveningAdhkarBox()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Tasbeeh Counter Section
                        TasbeehSection()
                    }
                }

                1 -> {
                    // Embedded Offline Mus-haf Reader Tab (with Full-Screen Toggle & Auto-Save on Exit)
                    val effectiveInitialPage = if (activeTrack.isFixedSurahWerd) {
                        activeTrack.fixedSurahStartPage.coerceIn(1, 604)
                    } else {
                        activeTrack.currentPage.coerceIn(1, 604)
                    }
                    QuranReaderScreen(
                        activeTrackId = activeTrack.trackId,
                        activeTrackName = activeTrack.trackName,
                        initialPage = effectiveInitialPage,
                        isFixedSurahWerd = activeTrack.isFixedSurahWerd,
                        isFullScreen = isMushafFullScreen,
                        onToggleFullScreen = { full -> isMushafFullScreen = full },
                        onAutoSaveStopBookmark = { surah, ayah, page ->
                            if (!activeTrack.isFixedSurahWerd) {
                                quranTargetPage = page
                                lastReadPage = page
                                lastReadSurah = surah
                                lastReadAyah = ayah
                                onAutoSaveStopPoint(surah, ayah, page)
                            }
                        },
                        onSaveStopBookmark = { surah, ayah, page ->
                            onLogProgress(surah, ayah, page, (page - activeTrack.currentPage).coerceAtLeast(1))
                            if (!activeTrack.isFixedSurahWerd) {
                                quranTargetPage = page
                                lastReadPage = page
                                lastReadSurah = surah
                                lastReadAyah = ayah
                            }
                        }
                    )
                }

                2 -> {
                    // Dedicated Prayer Times, Adhan Settings & Reverent Mihrab Qibla Compass Tab
                    PrayerAndQiblaScreen()
                }

                3 -> {
                    // Comprehensive Settings, User Guide, Backup, Adhan, and Logs Tab ("⚙️ إعدادات")
                    ComprehensiveSettingsAndGuideTab(
                        cumulativeAdhkarTasbeehTotal = cumulativeAdhkarTasbeehTotal,
                        lastReadSurah = lastReadSurah,
                        lastReadAyah = lastReadAyah,
                        lastReadPage = lastReadPage,
                        readingLogs = readingLogs,
                        spiritualMessages = spiritualMessages,
                        onResumeReadingAtLastStop = {
                            quranTargetPage = lastReadPage
                            selectedBottomTab = 1
                        },
                        onOpenBackupDialog = { showBackupDialog = true },
                        onOpenSpiritualMessage = { msg -> activeStartupMessage = msg },
                        onGenerateNewSpiritualMessage = {
                            val nextMsg = localStorage.recordNextStartupMessage()
                            activeStartupMessage = nextMsg
                        },
                        onOpenPrayerAndQiblaTab = { selectedBottomTab = 2 },
                        onOpenIntentionDuaDialog = { showIntentionRenewalDialog = true }
                    )
                }
            }
        }
    }

    // Ornate Historical Startup Spiritual Message Dialog (On App Open or Re-opened from "إعدادات")
    activeStartupMessage?.let { currentMsg ->
        SpiritualStartupMessageDialog(
            message = currentMsg,
            onMarkReadAndClose = {
                localStorage.markStartupMessageAsRead(currentMsg.id)
                activeStartupMessage = null
            },
            onShare = {
                localStorage.markStartupMessageAsRead(currentMsg.id)
                shareSpiritualMessage(context, currentMsg)
            },
            onDismissClose = {
                activeStartupMessage = null
            }
        )
    }

    if (showIntentionRenewalDialog) {
        QuranIntentionRenewalDialog(
            onDismiss = { showIntentionRenewalDialog = false }
        )
    }

    if (showWaqfDedicationDialog) {
        WaqfDedicationDialog(
            onDismiss = { showWaqfDedicationDialog = false }
        )
    }

    // Full Local Backup & Restore Dialog
    if (showBackupDialog) {
        BackupRestoreDialog(
            tracksCount = tracks.size,
            logsCount = readingLogs.size,
            currentSurah = activeTrack.currentSurah,
            currentPage = activeTrack.currentPage,
            onDismiss = { showBackupDialog = false },
            onRestoredSuccess = {
                val refreshedTrack = localStorage.tracksFlow.value.firstOrNull() ?: activeTrack
                lastReadPage = refreshedTrack.currentPage
                lastReadSurah = refreshedTrack.currentSurah
                lastReadAyah = refreshedTrack.currentAyah
                quranTargetPage = refreshedTrack.currentPage
            }
        )
    }

    if (showCompletionDialog) {
        KhatmaCompletionDialog(
            trackName = activeTrack.trackName,
            khatmaNumber = activeTrack.khatmaNumber,
            onDismiss = onDismissCompletionDialog,
            onStartNextKhatma = onStartNextKhatma
        )
    }

    postLogReflectionData?.let { data ->
        val sanitizedText = remember(data) {
            com.example.data.IslamicDuaHelper.sanitizeAndRandomizeDua(data.reflectionText)
        }
        val safeData = remember(data, sanitizedText) {
            data.copy(reflectionText = sanitizedText)
        }
        PostLogReflectionDialog(
            data = safeData,
            onDismiss = onDismissPostLogReflectionDialog,
            onShare = {
                ReflectionShareUtil.shareReflectionCard(
                    context = context,
                    surahName = data.surah,
                    pageNumber = data.page,
                    reflectionText = sanitizedText
                )
            }
        )
    }

    if (showInputDialog) {
        StopPointInputDialog(
            currentSurah = activeTrack.currentSurah,
            currentAyah = activeTrack.currentAyah,
            currentPage = activeTrack.currentPage,
            autoLaunchCamera = autoTriggerCameraInDialog,
            onDismiss = {
                showInputDialog = false
                autoTriggerCameraInDialog = false
            },
            onSubmit = { surah, ayah, page, pagesRead ->
                val clampedPage = page.coerceIn(1, 604)
                val safeAyah = ayah.coerceAtLeast(1)
                quranTargetPage = clampedPage
                lastReadPage = clampedPage
                lastReadSurah = surah
                lastReadAyah = safeAyah
                localStorage.syncTrackToQuranReaderPrefs(
                    trackId = activeTrack.trackId,
                    surah = surah,
                    ayah = safeAyah,
                    page = clampedPage
                )
                onLogProgress(surah, safeAyah, clampedPage, pagesRead)
                showInputDialog = false
                autoTriggerCameraInDialog = false
            }
        )
    }

    if (showCreateTrackDialog) {
        var isFixedSurahMode by remember { mutableStateOf(true) } // Default to Quick Fixed Surah Werd like سورة الكهف
        var selectedFixedSurah by remember {
            mutableStateOf(QuranDataHelper.surahs.find { it.number == 18 } ?: QuranDataHelper.surahs.first())
        }
        var newTrackName by remember { mutableStateOf("ورد سورة الكهف") }
        var isSurahMenuExpanded by remember { mutableStateOf(false) }

        val popularSurahNums = remember { listOf(18, 67, 36, 56, 32, 44, 2) } // الكهف، الملك، يس، الواقعة، السجدة، الدخان، البقرة

        AlertDialog(
            onDismissRequest = { showCreateTrackDialog = false },
            title = {
                Text(
                    text = "إضافة ورد جديد",
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mode Selector: [ورد سورة محددة] vs [ختمة كاملة]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoyalNavyDark)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFixedSurahMode) EmeraldGreen else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    isFixedSurahMode = true
                                    newTrackName = "ورد سورة ${selectedFixedSurah.nameArabic}"
                                }
                        ) {
                            Text(
                                text = "ورد سورة محددة",
                                color = if (isFixedSurahMode) Color.White else EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isFixedSurahMode) EmeraldGreen else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    isFixedSurahMode = false
                                    newTrackName = ""
                                }
                        ) {
                            Text(
                                text = "ختمة كاملة",
                                color = if (!isFixedSurahMode) Color.White else EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    if (isFixedSurahMode) {
                        Text(
                            text = "اختر السورة ليفتح عليها المصحف من بدايتها في كل مرة بدون احتسابها كتقدم ختمة (مثال: سورة الكهف كل جمعة):",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            lineHeight = 17.sp
                        )

                        // Quick Popular Surah Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(popularSurahNums) { sNum ->
                                val sObj = QuranDataHelper.surahs.find { it.number == sNum }
                                if (sObj != null) {
                                    val isChosen = selectedFixedSurah.number == sObj.number
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isChosen) EmeraldGreen else Color(0xFFFDF8EC),
                                        border = BorderStroke(1.dp, if (isChosen) EmeraldGreen else GoldBorder),
                                        modifier = Modifier.clickable {
                                            selectedFixedSurah = sObj
                                            newTrackName = "ورد سورة ${sObj.nameArabic}"
                                        }
                                    ) {
                                        Text(
                                            text = "سورة ${sObj.nameArabic}",
                                            color = if (isChosen) Color.White else EmeraldGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Full 114 Surah Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isSurahMenuExpanded,
                            onExpandedChange = { isSurahMenuExpanded = !isSurahMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = "سورة ${selectedFixedSurah.nameArabic} (تبدأ من صفحة ${selectedFixedSurah.startPage})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("السورة المختارة") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSurahMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isSurahMenuExpanded,
                                onDismissRequest = { isSurahMenuExpanded = false }
                            ) {
                                QuranDataHelper.surahs.forEach { surah ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${surah.number}. سورة ${surah.nameArabic} (ص ${surah.startPage})")
                                        },
                                        onClick = {
                                            selectedFixedSurah = surah
                                            newTrackName = "ورد سورة ${surah.nameArabic}"
                                            isSurahMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newTrackName,
                        onValueChange = { newTrackName = it },
                        label = {
                            Text(if (isFixedSurahMode) "اسم الورد (مثال: ورد الكهف)" else "اسم الختمة (مثال: ختمة التدبر)")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = newTrackName.trim().ifBlank {
                            if (isFixedSurahMode) "ورد سورة ${selectedFixedSurah.nameArabic}" else "ختمة مخصصة"
                        }
                        if (isFixedSurahMode) {
                            onCreateFixedSurahTrack(
                                cleanName,
                                selectedFixedSurah.nameArabic,
                                selectedFixedSurah.startPage
                            )
                        } else {
                            onCreateTrack(cleanName)
                        }
                        showCreateTrackDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White)
                ) {
                    Text("إضافة الورد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTrackDialog = false }) { Text("إلغاء", color = GoldAccent) }
            }
        )
    }

    if (showRenameTrackDialog) {
        var renameInput by remember { mutableStateOf(activeTrack.trackName) }
        AlertDialog(
            onDismissRequest = { showRenameTrackDialog = false },
            title = { Text("تعديل أو حذف الورد", color = EmeraldGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("اسم الورد") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (activeTrack.trackId != "main") {
                        OutlinedButton(
                            onClick = {
                                onDeleteTrack(activeTrack.trackId)
                                showRenameTrackDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حذف هذا الورد المخصص", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameTrack(activeTrack.trackId, renameInput.trim())
                            showRenameTrackDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White)
                ) {
                    Text("حفظ الاسم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameTrackDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier.border(0.6.dp, GoldBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopPointInputDialog(
    currentSurah: String,
    currentAyah: Int,
    currentPage: Int,
    autoLaunchCamera: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (surah: String, ayah: Int, page: Int, pagesReadToday: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pastedAyahText by remember { mutableStateOf("") }
    var detectedNotice by remember { mutableStateOf<String?>(null) }
    var cameraErrorNotice by remember { mutableStateOf<String?>(null) }
    var isRecognizingCamera by remember { mutableStateOf(false) }
    var selectedSurahName by remember { mutableStateOf(currentSurah) }
    var ayahInput by remember { mutableStateOf(currentAyah.toString()) }
    var pageInput by remember { mutableStateOf(currentPage.toString()) }
    var isExpanded by remember { mutableStateOf(false) }

    fun processBitmapForStopPoint(bitmap: Bitmap) {
        isRecognizingCamera = true
        cameraErrorNotice = null
        detectedNotice = null
        coroutineScope.launch {
            val result = MushafCameraRecognizer.recognizeMushafPage(context, bitmap)
            isRecognizingCamera = false
            result.onSuccess { rec ->
                selectedSurahName = rec.surahName
                ayahInput = rec.ayahNumber.toString()
                pageInput = rec.pageNumber.toString()
                if (rec.recognizedVerseSnippet.isNotBlank()) {
                    pastedAyahText = rec.recognizedVerseSnippet
                }
                detectedNotice = "${rec.confidenceNote}: سورة ${rec.surahName} • آية ${rec.ayahNumber} • صفحة ${rec.pageNumber}"
                Toast.makeText(
                    context,
                    "تم التعرف على صفحة ${rec.pageNumber} (سورة ${rec.surahName} آية ${rec.ayahNumber})",
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { err ->
                cameraErrorNotice = err.localizedMessage ?: "تعذر تحليل صورة المصحف."
            }
        }
    }

    var pendingCameraPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun decodeUriToBitmap(uri: android.net.Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processBitmapForStopPoint(bitmap)
        }
    }

    val takeHighResPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingCameraPhotoUri
        if (success && uri != null) {
            try {
                val bitmap = decodeUriToBitmap(uri)
                if (bitmap != null) {
                    processBitmapForStopPoint(bitmap)
                }
            } catch (e: Exception) {
                cameraErrorNotice = "تعذر قراءة الصورة الملتقطة: ${e.localizedMessage}"
            }
        }
    }

    fun startCameraCapture() {
        try {
            val tempFile = java.io.File(context.cacheDir, "mushaf_capture_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            pendingCameraPhotoUri = uri
            takeHighResPictureLauncher.launch(uri)
        } catch (_: Exception) {
            takePicturePreviewLauncher.launch(null)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraCapture()
        } else {
            cameraErrorNotice = "يرجى السماح بصلاحية الكاميرا لتصوير صفحة المصحف، أو اختر صورة من المعرض."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = decodeUriToBitmap(uri)
                if (bitmap != null) {
                    processBitmapForStopPoint(bitmap)
                }
            } catch (e: Exception) {
                cameraErrorNotice = "تعذر قراءة الصورة المختارة: ${e.localizedMessage}"
            }
        }
    }

    fun launchCameraWithPermissionCheck() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            startCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(autoLaunchCamera) {
        if (autoLaunchCamera) {
            launchCameraWithPermissionCheck()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل نقطة التوقف والورد اليومي",
                style = MaterialTheme.typography.titleLarge,
                color = GoldAccent,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Camera & Photo Picker Smart Recognition Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldAccent, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = RoyalNavyDark.copy(alpha = 0.85f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "التعرف الذكي على صفحة المصحف بالكاميرا",
                                style = MaterialTheme.typography.titleSmall,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "صوّر صفحة المصحف التي وقفت عندها وسيتم التعرف تلقائياً على رقم الصفحة والسورة والآية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1A110B).copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (isRecognizingCamera) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = GoldAccent,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "جاري قراءة صفحة المصحف وتحديد موضع الوقف...",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { launchCameraWithPermissionCheck() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dialog_capture_camera_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = androidx.compose.ui.graphics.Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تصوير بالكاميرا",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dialog_pick_gallery_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldAccent),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "صورة من الهاتف",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (!cameraErrorNotice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = cameraErrorNotice!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Copy-paste Ayah field with automatic 6,236-verse text detection
                OutlinedTextField(
                    value = pastedAyahText,
                    onValueChange = { newText ->
                        pastedAyahText = newText
                        val match = com.example.data.QuranTextMatcher.findAyahMatch(context, newText)
                        if (match != null) {
                            selectedSurahName = match.surahName
                            ayahInput = match.ayahNumber.toString()
                            pageInput = match.pageNumber.toString()
                            detectedNotice = match.description
                        } else {
                            detectedNotice = null
                        }
                    },
                    label = { Text("أو ألصق جزءاً من الآية الكريمة للتعرف التلقائي") },
                    placeholder = { Text("مثال: ذلك الكتاب لا ريب فيه... أو اسم السورة") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (!detectedNotice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = detectedNotice!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSurahName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اختر السورة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        QuranDataHelper.surahs.forEach { surah ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${surah.number}. سورة ${surah.nameArabic}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "صفحة ${surah.startPage}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmeraldGreen
                                        )
                                    }
                                },
                                onClick = {
                                    selectedSurahName = surah.nameArabic
                                    pageInput = surah.startPage.toString()
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ayahInput,
                        onValueChange = { newAyahStr ->
                            val clean = newAyahStr.filter { char -> char.isDigit() }
                            ayahInput = clean
                            val aNum = clean.toIntOrNull()
                            if (aNum != null && aNum >= 1) {
                                val exactVerse = com.example.data.OfflineQuranDatabase.getVerseBySurahName(context, selectedSurahName, aNum)
                                if (exactVerse != null) {
                                    pageInput = exactVerse.page.toString()
                                }
                            }
                        },
                        label = { Text("رقم الآية") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { newPageStr ->
                            val clean = newPageStr.filter { char -> char.isDigit() }
                            pageInput = clean
                            val pNum = clean.toIntOrNull()
                            if (pNum != null && pNum in 1..604) {
                                val pageVerses = com.example.data.OfflineQuranDatabase.getVersesForPage(context, pNum)
                                val firstV = pageVerses.firstOrNull()
                                if (firstV != null) {
                                    selectedSurahName = firstV.surahNameAr
                                    ayahInput = firstV.ayahNumber.toString()
                                }
                            }
                        },
                        label = { Text("رقم الصفحة (1-604)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                val currentPageInt = pageInput.toIntOrNull()?.coerceIn(1, 604) ?: currentPage
                val versesOnSelectedPage = remember(currentPageInt) {
                    com.example.data.OfflineQuranDatabase.getVersesForPage(context, currentPageInt)
                }
                if (versesOnSelectedPage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "آيات صفحة $currentPageInt (اضغط لاختيار آية التوقف بدقة):",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(versesOnSelectedPage) { v ->
                            val isSelectedVerse = v.surahNameAr == selectedSurahName && v.ayahNumber.toString() == ayahInput
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelectedVerse) EmeraldGreen.copy(alpha = 0.22f) else RoyalNavyDark.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = if (isSelectedVerse) 1.5.dp else 0.8.dp,
                                    color = if (isSelectedVerse) EmeraldGreen else GoldBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedSurahName = v.surahNameAr
                                        ayahInput = v.ayahNumber.toString()
                                        pageInput = v.page.toString()
                                        pastedAyahText = v.textAr
                                        detectedNotice = "تم اختيار سورة ${v.surahNameAr} • الآية ${v.ayahNumber} (صفحة ${v.page})"
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 170.dp)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${v.surahNameAr} - آية ${v.ayahNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelectedVerse) EmeraldGreen else GoldAccent
                                    )
                                    Text(
                                        text = v.textAr,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val newPage = ((pageInput.toIntOrNull() ?: currentPage) + 2).coerceIn(1, 604)
                        pageInput = newPage.toString()
                        val pageVerses = com.example.data.OfflineQuranDatabase.getVersesForPage(context, newPage)
                        val firstV = pageVerses.firstOrNull()
                        if (firstV != null) {
                            selectedSurahName = firstV.surahNameAr
                            ayahInput = firstV.ayahNumber.toString()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("تم إنجاز ربع حزب اليوم (+2.5 صفحة)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val page = pageInput.toIntOrNull()?.coerceIn(1, 604) ?: currentPage
                    val ayah = ayahInput.toIntOrNull()?.coerceAtLeast(1) ?: currentAyah
                    val pagesRead = (page - currentPage).coerceAtLeast(1)
                    onSubmit(selectedSurahName, ayah, page, pagesRead)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalNavyDark,
                    contentColor = GoldAccent
                )
            ) {
                Text("حفظ والتدبر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun PostLogReflectionDialog(
    data: com.example.ui.viewmodel.ReflectionDialogData,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "تدبر الورد وقصة الآيات المباركة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Text(
                        text = "سورة ${data.surah} - الآية ${data.ayah} (صفحة ${data.page})",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldGreen
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, GoldBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = data.reflectionText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalNavyDark, contentColor = GoldAccent)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة التدبر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("تم بحمد الله")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasbeehSection() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("tasbeeh_prefs", Context.MODE_PRIVATE) }

    data class DhikrData(
        val key: String,
        val text: String,
        val target: Int = 33,
        val isCustom: Boolean = false
    )

    val baseAdhkarList = remember {
        listOf(
            DhikrData("subhan_allah", "سُبْحَانَ اللَّهِ", 33),
            DhikrData("alhamdulillah", "الْحَمْدُ لِلَّهِ", 33),
            DhikrData("la_ilaha_illallah", "لَا إِلَهَ إِلَّا اللَّهُ", 33),
            DhikrData("allahu_akbar", "اللَّهُ أَكْبَرُ", 33),
            DhikrData("salawat", "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى سَيِّدِنَا مُحَمَّدٍ", 100)
        )
    }

    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }
    val backupVersion by localStorage.backupRestoreVersionFlow.collectAsState()
    val customizationVersion by AdhkarCustomizationManager.versionFlow.collectAsState()

    val adhkarList = remember(backupVersion, customizationVersion) {
        val overrides = AdhkarCustomizationManager.getOverridesMap(context)
        val deletedKeys = AdhkarCustomizationManager.getDeletedKeys(context)
        val customAdded = AdhkarCustomizationManager.getCustomAddedAdhkar(context, scopeFilter = "tasbeeh")

        val customMapped = customAdded
            .filter { !deletedKeys.contains(it.uniqueKey) }
            .map { c ->
                val ov = overrides[c.uniqueKey]
                DhikrData(
                    key = c.uniqueKey,
                    text = ov?.arabicText?.ifBlank { c.arabicText } ?: c.arabicText,
                    target = (ov?.targetCount ?: c.targetCount).coerceAtLeast(1),
                    isCustom = true
                )
            }

        val builtInMapped = baseAdhkarList
            .filter { !deletedKeys.contains(it.key) }
            .map { d ->
                val ov = overrides[d.key]
                if (ov != null) {
                    d.copy(
                        text = ov.arabicText.ifBlank { d.text },
                        target = ov.targetCount.coerceAtLeast(1)
                    )
                } else {
                    d
                }
            }

        val rawCombined = builtInMapped + customMapped
        AdhkarCustomizationManager.applyScopeOrder(
            context = context,
            scopeKey = "tasbeeh",
            items = rawCombined,
            keySelector = { it.key }
        )
    }

    val favoriteItemKeys = remember(customizationVersion) {
        AdhkarCustomizationManager.getFavoriteItems(context).map { it.uniqueKey }.toSet()
    }

    var showAddTasbeehDialog by remember { mutableStateOf(false) }
    var editingTasbeeh by remember { mutableStateOf<DhikrData?>(null) }
    var dailyResetTrigger by remember { mutableStateOf(0) }
    val adhkarPrefs = remember { context.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        if (com.example.ui.components.ensureDailyAdhkarResetIfNewDay(adhkarPrefs, context)) {
            dailyResetTrigger++
        }
        while (true) {
            kotlinx.coroutines.delay(20_000L)
            if (com.example.ui.components.ensureDailyAdhkarResetIfNewDay(adhkarPrefs, context)) {
                dailyResetTrigger++
            }
        }
    }

    val counts = remember(backupVersion, adhkarList, dailyResetTrigger) {
        com.example.ui.components.ensureDailyAdhkarResetIfNewDay(adhkarPrefs, context)
        mutableStateMapOf<String, Int>().apply {
            adhkarList.forEach { dhikr ->
                this[dhikr.key] = prefs.getInt(dhikr.key, 0)
            }
        }
    }

    val totalCount = counts.values.sum()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Section — Clean & Unclipped
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RoyalNavyDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "عداد التسبيح والأذكار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "إجمالي التسبيحات الحالية: $totalCount • اضغط مطولاً لتعديل أي ذكر",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Quick Add Custom Tasbeeh Button
                OutlinedButton(
                    onClick = { showAddTasbeehDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة ذكر للتسبيح",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة ذكر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Full-Width / Clean Reset & Alarm Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlarmTriggerChip(
                    targetId = "tasbeeh_main",
                    defaultTitle = "منبه عداد التسبيح والأذكار",
                    defaultSubtitle = "حان موعد ورد التسبيح والاستغفار والصلاة على النبي ﷺ",
                    defaultHour = 21,
                    defaultMinute = 0
                )

                OutlinedButton(
                    onClick = {
                        adhkarList.forEach { dhikr ->
                            counts[dhikr.key] = 0
                            prefs.edit().putInt(dhikr.key, 0).apply()
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تصفير العداد",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تصفير العدادات",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Adhkar Clickable Buttons (Tap = +1, Long-Press = Edit text / repeat count / delete, Heart = Favorite, Up/Down = Reorder)
            val allTasbeehKeys = adhkarList.map { it.key }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                adhkarList.forEachIndexed { index, dhikr ->
                    val count = counts[dhikr.key] ?: 0
                    val isTargetReached = count >= dhikr.target
                    val isFav = favoriteItemKeys.contains(dhikr.key)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = {
                                    val newCount = count + 1
                                    counts[dhikr.key] = newCount
                                    prefs.edit().putInt(dhikr.key, newCount).apply()
                                    localStorage.incrementCumulativeAdhkarTasbeeh(1)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    editingTasbeeh = dhikr
                                }
                            )
                            .border(
                                width = if (isTargetReached) 1.4.dp else 0.8.dp,
                                color = if (isTargetReached) EmeraldGreen else GoldBorder,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTargetReached)
                                EmeraldGreen.copy(alpha = 0.08f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // Top Row: Heart Favorite Icon + Full-Width Dhikr Text + Up/Down Reorder Pill + Quick Edit Icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val unified = UnifiedDhikrItem(
                                            uniqueKey = dhikr.key,
                                            scopeKey = "tasbeeh",
                                            title = dhikr.text,
                                            categoryTitle = "عداد التسبيح والأذكار",
                                            arabicText = dhikr.text,
                                            benefit = "",
                                            targetCount = dhikr.target,
                                            isCustomAdded = dhikr.isCustom
                                        )
                                        val added = AdhkarCustomizationManager.toggleDhikrFavorite(context, unified)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(
                                            context,
                                            if (added) "تمت إضافة الذكر إلى المفضلة ❤️" else "تمت إزالة الذكر من المفضلة",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "إضافة للمفضلة",
                                        tint = if (isFav) FavoriteHeartCrimson else GoldAccent,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = dhikr.text,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 16.sp,
                                    lineHeight = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTargetReached) EmeraldGreen else MaterialTheme.colorScheme.onSurface
                                )

                                if (adhkarList.size > 1) {
                                    DhikrOrderArrowsPill(
                                        canMoveUp = index > 0,
                                        canMoveDown = index < adhkarList.lastIndex,
                                        onMoveUp = {
                                            AdhkarCustomizationManager.moveItemInScope(
                                                context = context,
                                                scopeKey = "tasbeeh",
                                                currentOrderedKeys = allTasbeehKeys,
                                                targetKey = dhikr.key,
                                                direction = -1
                                            )
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onMoveDown = {
                                            AdhkarCustomizationManager.moveItemInScope(
                                                context = context,
                                                scopeKey = "tasbeeh",
                                                currentOrderedKeys = allTasbeehKeys,
                                                targetKey = dhikr.key,
                                                direction = 1
                                            )
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                IconButton(
                                    onClick = { editingTasbeeh = dhikr },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل الذكر",
                                        tint = GoldAccent.copy(alpha = 0.75f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Row: Non-wrapping Counter Badge on Right + Action Buttons (Alarm, +1, Reset) on Left
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isTargetReached)
                                        EmeraldGreen.copy(alpha = 0.12f)
                                    else
                                        GoldAccent.copy(alpha = 0.10f),
                                    border = BorderStroke(
                                        0.6.dp,
                                        if (isTargetReached) EmeraldGreen.copy(alpha = 0.45f) else GoldBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "التسبيح: ",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "$count / ${dhikr.target}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isTargetReached) EmeraldGreen else GoldAccent,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (isTargetReached) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "تم إكمال الورد",
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AlarmTriggerChip(
                                        targetId = "tasbeeh_${dhikr.key}",
                                        defaultTitle = "منبه ذكر: ${dhikr.text}",
                                        defaultSubtitle = "تذكير بتسبيح: ${dhikr.text}",
                                        defaultHour = 10,
                                        defaultMinute = 0,
                                        compact = true
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = EmeraldGreen,
                                        border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+1",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFFDF8EC),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            counts[dhikr.key] = 0
                                            prefs.edit().putInt(dhikr.key, 0).apply()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "تصفير الذكر",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTasbeehDialog) {
        AddOrEditDhikrDialog(
            isEditMode = false,
            scopeKey = "tasbeeh",
            categoryTitle = "عداد التسبيح والأذكار",
            initialTargetCount = 33,
            currentScopeKeys = adhkarList.map { it.key },
            onDismiss = { showAddTasbeehDialog = false }
        )
    }

    editingTasbeeh?.let { target ->
        AddOrEditDhikrDialog(
            isEditMode = true,
            scopeKey = "tasbeeh",
            categoryTitle = "عداد التسبيح والأذكار",
            initialUniqueKey = target.key,
            initialTitle = target.text,
            initialArabicText = target.text,
            initialBenefit = "",
            initialTargetCount = target.target,
            currentScopeKeys = adhkarList.map { it.key },
            onDismiss = { editingTasbeeh = null }
        )
    }
}

@Composable
private fun KhatmaCapsuleSingleLineText(
    text: String,
    initialFontSizeSp: Float,
    minFontSizeSp: Float = 8.5f,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color
) {
    var currentFontSizeSp by remember(text, initialFontSizeSp) {
        mutableStateOf(initialFontSizeSp)
    }

    Text(
        text = text,
        fontSize = currentFontSizeSp.sp,
        fontWeight = fontWeight,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { layoutResult ->
            if (layoutResult.didOverflowWidth && currentFontSizeSp > minFontSizeSp) {
                currentFontSizeSp = (currentFontSizeSp - 0.5f).coerceAtLeast(minFontSizeSp)
            }
        }
    )
}

