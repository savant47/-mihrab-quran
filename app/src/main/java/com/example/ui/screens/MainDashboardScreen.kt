package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import com.example.data.KhatmaMetrics
import com.example.ui.components.AlarmTriggerChip
import com.example.ui.components.MihrabiLogo
import com.example.ui.components.MorningEveningAdhkarBox
import com.example.data.QuranDataHelper
import com.example.data.model.KhatmaTrack
import com.example.data.model.ReadingLog
import com.example.data.model.User
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
    onRenameTrack: (String, String) -> Unit,
    onAutoSaveStopPoint: (surah: String, ayah: Int, page: Int) -> Unit = { _, _, _ -> },
    onLogProgress: (surah: String, ayah: Int, page: Int, pagesReadToday: Int) -> Unit,
    onStartNextKhatma: () -> Unit,
    onDismissCompletionDialog: () -> Unit
) {
    val context = LocalContext.current
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: الورد, 1: المصحف, 2: مواقيت الصلاة والقبلة, 3: السجل
    val quranPrefs = remember { context.getSharedPreferences("quran_reader_prefs", Context.MODE_PRIVATE) }
    var quranTargetPage by remember { mutableIntStateOf(activeTrack.currentPage) }

    var lastReadPage by remember(activeTrack) { mutableIntStateOf(activeTrack.currentPage) }
    var lastReadSurah by remember(activeTrack) { mutableStateOf(activeTrack.currentSurah) }
    var lastReadAyah by remember(activeTrack) { mutableIntStateOf(activeTrack.currentAyah) }

    LaunchedEffect(Unit) {
        com.example.alarm.PrayerTimesAndAdhanManager.scheduleAllFivePrayers(context)
    }

    LaunchedEffect(selectedBottomTab, activeTrack) {
        val tId = activeTrack.trackId
        val savedPage = quranPrefs.getInt("last_read_page_$tId", activeTrack.currentPage)
        lastReadPage = if (savedPage in 1..604) savedPage else activeTrack.currentPage
        lastReadSurah = quranPrefs.getString("last_read_surah_$tId", activeTrack.currentSurah) ?: activeTrack.currentSurah
        lastReadAyah = quranPrefs.getInt("last_read_ayah_$tId", activeTrack.currentAyah)
        quranTargetPage = lastReadPage
    }

    var showInputDialog by remember { mutableStateOf(false) }
    var autoTriggerCameraInDialog by remember { mutableStateOf(false) }
    var isMushafFullScreen by remember { mutableStateOf(false) }
    var showCreateTrackDialog by remember { mutableStateOf(false) }
    var showRenameTrackDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedBottomTab == 1 && isMushafFullScreen) {
        isMushafFullScreen = false
    }

    Scaffold(
        topBar = {
            if (selectedBottomTab != 1) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MihrabiLogo(
                                size = 38.dp,
                                showIntegratedText = false,
                                animatedGlow = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "محرابي",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "إعداد آسر الشربيني | وقف لله تعالى - لا تنسونا من خالص الدعاء لأبي وأمي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldAccent,
                                    fontSize = 9.5.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
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
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = null) },
                        label = { Text("سجل القراءات", fontSize = 11.sp, maxLines = 1) }
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
                        // Multi-Track Selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "متابعة الأوراد المخصصة:",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showCreateTrackDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("add_track_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إضافة ورد جديد",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
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
                                Card(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { onSelectTrack(track.trackId) }
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) GoldAccent else GoldBorder,
                                            shape = RoundedCornerShape(20.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) RoyalNavyDark else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = track.trackName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "صفحة ${track.currentPage} • ${track.currentSurah}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.sp,
                                                color = if (isSelected) GoldAccent.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
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
                                                    val target = track.currentPage.coerceIn(1, 604)
                                                    quranTargetPage = target
                                                    quranPrefs.edit()
                                                        .putInt("last_read_page", target)
                                                        .putString("last_read_surah", track.currentSurah)
                                                        .putInt("last_read_ayah", track.currentAyah)
                                                        .apply()
                                                    selectedBottomTab = 1
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.MenuBook,
                                                    contentDescription = "استكمال القراءة في المصحف",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل اسم الورد",
                                                tint = GoldAccent,
                                                modifier = Modifier
                                                    .size(14.dp)
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Circular Progress Card for Active Track
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
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${activeTrack.trackName} - (الختمة رقم ${activeTrack.khatmaNumber})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GoldAccent
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(160.dp)
                                ) {
                                    val strokeWidth = 14.dp
                                    val primaryColor = GoldAccent
                                    val backgroundColor = RoyalNavyDark.copy(alpha = 0.2f)
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

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${metrics.progressPercentage}%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${metrics.currentPage} / 604 صفحة",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Dual Action Buttons for Active Track: Resume Reading & Log Progress
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Primary Button 1: Resume Reading this specific track directly in the Mus-haf
                                    Button(
                                        onClick = {
                                            val target = activeTrack.currentPage.coerceIn(1, 604)
                                            quranTargetPage = target
                                            quranPrefs.edit()
                                                .putInt("last_read_page", target)
                                                .putString("last_read_surah", activeTrack.currentSurah)
                                                .putInt("last_read_ayah", activeTrack.currentAyah)
                                                .apply()
                                            selectedBottomTab = 1
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("resume_track_primary_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldGreen,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "متابعة قراءة ${activeTrack.trackName} (صفحة ${activeTrack.currentPage})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Secondary Button 2: Log Progress / Stop Point
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
                                                .defaultMinSize(minHeight = 46.dp)
                                                .testTag("log_progress_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            border = BorderStroke(1.dp, GoldBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = GoldAccent
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = GoldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "تسجيل نقطة التوقف",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                autoTriggerCameraInDialog = true
                                                showInputDialog = true
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 46.dp)
                                                .testTag("camera_stop_point_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = RoyalNavyDark,
                                                contentColor = GoldAccent
                                            ),
                                            border = BorderStroke(1.dp, GoldAccent)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "تصوير صفحة المصحف بالكاميرا",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "تصوير الوقف بالكاميرا",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Dedicated Alarm Reminder Button for the Active Werd Track
                                    AlarmTriggerChip(
                                        targetId = "werd_${activeTrack.trackId}",
                                        defaultTitle = "منبه الورد القرآني (${activeTrack.trackName})",
                                        defaultSubtitle = "تذكير بقراءة وردك اليومي (${activeTrack.trackName} - صفحة ${activeTrack.currentPage})",
                                        defaultHour = 20,
                                        defaultMinute = 0,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Telemetry Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "الصفحات والأرباع",
                                value = "متبقي ${metrics.remainingPages} صفحة",
                                subtitle = "(${metrics.remainingRubCount} ربع حزب)",
                                icon = Icons.Default.MenuBook
                            )
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "المعدل اليومي",
                                value = "${metrics.actualDailyPacePages} صفحة/يوم",
                                subtitle = "~1 ربع/يوم",
                                icon = Icons.Default.Speed
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        quranTargetPage = activeTrack.currentPage
                                        selectedBottomTab = 1
                                    },
                                title = "موقع التوقف",
                                value = "${activeTrack.currentSurah} - آية ${activeTrack.currentAyah}",
                                subtitle = "صفحة ${activeTrack.currentPage} (اضغط للقراءة)",
                                icon = Icons.Default.Book
                            )
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "التاريخ المتوقع",
                                value = metrics.projectedCompletionDate,
                                subtitle = "لختام المصحف",
                                icon = Icons.Default.CalendarToday
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Morning & Evening Adhkar Box (2 Buttons)
                        MorningEveningAdhkarBox()

                        Spacer(modifier = Modifier.height(18.dp))

                        // Interactive Tasbeeh Counter Section
                        TasbeehSection()
                    }
                }

                1 -> {
                    // Embedded Offline Mus-haf Reader Tab (with Full-Screen Toggle & Auto-Save on Exit)
                    QuranReaderScreen(
                        activeTrackId = activeTrack.trackId,
                        activeTrackName = activeTrack.trackName,
                        initialPage = quranTargetPage,
                        isFullScreen = isMushafFullScreen,
                        onToggleFullScreen = { full -> isMushafFullScreen = full },
                        onAutoSaveStopBookmark = { surah, ayah, page ->
                            quranTargetPage = page
                            lastReadPage = page
                            lastReadSurah = surah
                            lastReadAyah = ayah
                            onAutoSaveStopPoint(surah, ayah, page)
                        },
                        onSaveStopBookmark = { surah, ayah, page ->
                            onLogProgress(surah, ayah, page, (page - activeTrack.currentPage).coerceAtLeast(1))
                            quranTargetPage = page
                            lastReadPage = page
                            lastReadSurah = surah
                            lastReadAyah = ayah
                        }
                    )
                }

                2 -> {
                    // Dedicated Prayer Times, Adhan Settings & Reverent Mihrab Qibla Compass Tab
                    PrayerAndQiblaScreen()
                }

                3 -> {
                    // History Log View
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
                            Text(
                                text = "سجل الأوراد والتدبر السابقة:",
                                style = MaterialTheme.typography.titleLarge,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RoyalNavyDark,
                                border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${readingLogs.size} تسجيل",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = GoldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Bookmark Quick-Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "نقطة التوقف الحالية في المصحف",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "سورة $lastReadSurah • آية $lastReadAyah • صفحة $lastReadPage",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        quranTargetPage = lastReadPage
                                        selectedBottomTab = 1
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("متابعة القراءة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (readingLogs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد سجلات قراءة سابقة بعد.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(readingLogs) { log ->
                                    val dateFormatted = remember(log.timestampMillis) {
                                        try {
                                            SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(log.timestampMillis))
                                        } catch (_: Exception) {
                                            ""
                                        }
                                    }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(0.5.dp, GoldBorder, RoundedCornerShape(14.dp)),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "سورة ${log.surah} - آية ${log.ayah}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldGreen
                                                )
                                                Text(
                                                    text = "+${log.pagesReadToday} صفحة",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = GoldAccent
                                                )
                                            }
                                            if (dateFormatted.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = dateFormatted,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = log.reflectionSummary,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis
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
                onLogProgress(surah, ayah, page, pagesRead)
                showInputDialog = false
                autoTriggerCameraInDialog = false
            }
        )
    }

    if (showCreateTrackDialog) {
        var newTrackName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateTrackDialog = false },
            title = { Text("إضافة ورد / جروب متابعة جديد", color = GoldAccent) },
            text = {
                OutlinedTextField(
                    value = newTrackName,
                    onValueChange = { newTrackName = it },
                    label = { Text("اسم الورد (مثال: جروب الفجر / ورد المساء)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTrackName.isNotBlank()) {
                            onCreateTrack(newTrackName.trim())
                            showCreateTrackDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalNavyDark, contentColor = GoldAccent)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTrackDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showRenameTrackDialog) {
        var renameInput by remember { mutableStateOf(activeTrack.trackName) }
        AlertDialog(
            onDismissRequest = { showRenameTrackDialog = false },
            title = { Text("تعديل اسم الورد", color = GoldAccent) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("الاسم الجديد") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameTrack(activeTrack.trackId, renameInput.trim())
                            showRenameTrackDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalNavyDark, contentColor = GoldAccent)
                ) {
                    Text("حفظ")
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
        modifier = modifier.border(0.5.dp, GoldBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp
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

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processBitmapForStopPoint(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePicturePreviewLauncher.launch(null)
        } else {
            cameraErrorNotice = "يرجى السماح بصلاحية الكاميرا لتصوير صفحة المصحف، أو اختر صورة من المعرض."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
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
            takePicturePreviewLauncher.launch(null)
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
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

@Composable
fun TasbeehSection() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("tasbeeh_prefs", Context.MODE_PRIVATE) }

    data class DhikrData(
        val key: String,
        val text: String,
        val target: Int = 33
    )

    val adhkarList = remember {
        listOf(
            DhikrData("subhan_allah", "سُبْحَانَ اللَّهِ", 33),
            DhikrData("alhamdulillah", "الْحَمْدُ لِلَّهِ", 33),
            DhikrData("la_ilaha_illallah", "لَا إِلَهَ إِلَّا اللَّهُ", 33),
            DhikrData("allahu_akbar", "اللَّهُ أَكْبَرُ", 33),
            DhikrData("salawat", "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى سَيِّدِنَا مُحَمَّدٍ", 100)
        )
    }

    val counts = remember {
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
                modifier = Modifier.fillMaxWidth()
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
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "إجمالي التسبيحات: $totalCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
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

            // Adhkar Clickable Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                adhkarList.forEach { dhikr ->
                    val count = counts[dhikr.key] ?: 0
                    val isTargetReached = count >= dhikr.target

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val newCount = count + 1
                                counts[dhikr.key] = newCount
                                prefs.edit().putInt(dhikr.key, newCount).apply()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            .border(
                                width = if (isTargetReached) 1.5.dp else 0.8.dp,
                                color = if (isTargetReached) EmeraldGreen else GoldBorder,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTargetReached)
                                EmeraldGreen.copy(alpha = 0.08f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dhikr.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTargetReached) EmeraldGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "مرات التسبيح: ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldAccent
                                    )
                                    if (isTargetReached) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "تم إكمال الورد",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AlarmTriggerChip(
                                    targetId = "tasbeeh_${dhikr.key}",
                                    defaultTitle = "منبه ذكر: ${dhikr.text}",
                                    defaultSubtitle = "تذكير بتسبيح: ${dhikr.text}",
                                    defaultHour = 10,
                                    defaultMinute = 0,
                                    compact = true
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(RoyalNavyDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+1",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = {
                                        counts[dhikr.key] = 0
                                        prefs.edit().putInt(dhikr.key, 0).apply()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تصفير الذكر",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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
}
