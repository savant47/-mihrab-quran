package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.QuranAudioPlayerManager
import com.example.audio.QuranReciter
import com.example.data.OfflineQuranDatabase
import com.example.data.OfflineVerse
import com.example.data.QuranDataHelper
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import kotlinx.coroutines.launch

// Keep helper for URL compatibility if needed by tests
fun parsePageFromUrl(url: String?): Int? {
    if (url.isNullOrBlank()) return null
    val pageRegex = Regex("""/page/(\d+)""", RegexOption.IGNORE_CASE)
    val pageMatch = pageRegex.find(url)
    if (pageMatch != null) {
        val p = pageMatch.groupValues[1].toIntOrNull()
        if (p != null && p in 1..604) return p
    }
    return null
}

private fun toArabicIndicDigits(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { ch ->
        if (ch in '0'..'9') arabicDigits[ch - '0'] else ch
    }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    activeTrackId: String = "main",
    activeTrackName: String,
    initialPage: Int,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (Boolean) -> Unit = {},
    onAutoSaveStopBookmark: (surahName: String, ayahNumber: Int, pageNumber: Int) -> Unit = { _, _, _ -> },
    onSaveStopBookmark: (surahName: String, ayahNumber: Int, pageNumber: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val quranPrefs = remember { context.getSharedPreferences("quran_reader_prefs", Context.MODE_PRIVATE) }
    val backupVersion by remember {
        com.example.data.local.LocalKhatmaStorage.getInstance(context).backupRestoreVersionFlow
    }.collectAsState()

    val audioPlaybackState by QuranAudioPlayerManager.playbackState.collectAsState()

    LaunchedEffect(backupVersion) {
        QuranAudioPlayerManager.initPreferences(context)
    }

    val savedLastPage = remember(activeTrackId, backupVersion) {
        quranPrefs.getInt("last_viewed_page_$activeTrackId", initialPage.coerceIn(1, 604)).coerceIn(1, 604)
    }

    var isDatabaseReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        OfflineQuranDatabase.ensureLoaded(context)
        isDatabaseReady = true
    }

    val startPage = remember(activeTrackId, initialPage, backupVersion) {
        if (initialPage in 1..604) initialPage else savedLastPage
    }

    val pagerState = rememberPagerState(
        initialPage = (startPage - 1).coerceIn(0, 603),
        pageCount = { 604 }
    )

    val currentPage = pagerState.currentPage + 1

    // Reader customization states
    var isNightMushafMode by remember(backupVersion) {
        mutableStateOf(quranPrefs.getBoolean("night_mushaf_mode", false))
    }
    var isAyahByAyahMode by remember(backupVersion) {
        mutableStateOf(quranPrefs.getBoolean("ayah_by_ayah_mode", false))
    }
    var fontScaleSp by remember(backupVersion) {
        mutableFloatStateOf(quranPrefs.getFloat("mushaf_font_size_sp_v2", 28f).coerceIn(20f, 38f))
    }

    // Exit Full-Screen mode when user presses Back button
    BackHandler(enabled = isFullScreen) {
        onToggleFullScreen(false)
    }

    // Track current stop bookmark (Surah & Ayah)
    var bookmarkedSurah by remember(activeTrackId, backupVersion) {
        mutableStateOf(quranPrefs.getString("last_read_surah_$activeTrackId", "الفاتحة") ?: "الفاتحة")
    }
    var bookmarkedAyah by remember(activeTrackId, backupVersion) {
        mutableIntStateOf(quranPrefs.getInt("last_read_ayah_$activeTrackId", 1))
    }
    var bookmarkedPage by remember(activeTrackId, backupVersion) {
        mutableIntStateOf(quranPrefs.getInt("last_read_page_$activeTrackId", startPage))
    }

    // Hifz (Memorized / Saved Ayahs) set stored in SharedPreferences
    var memorizedAyahKeys by remember(backupVersion) {
        mutableStateOf(
            quranPrefs.getStringSet("memorized_ayahs_set", emptySet())?.toSet() ?: emptySet()
        )
    }

    fun toggleMemorizedAyah(surahNum: Int, ayahNum: Int): Boolean {
        val key = "$surahNum:$ayahNum"
        val updated = memorizedAyahKeys.toMutableSet()
        val added = if (updated.contains(key)) {
            updated.remove(key)
            false
        } else {
            updated.add(key)
            true
        }
        memorizedAyahKeys = updated
        quranPrefs.edit().putStringSet("memorized_ayahs_set", updated).apply()
        return added
    }

    // Floating Mini Popover above tapped Ayah (Play ▶️, Tafsir 🔎, Reciter/Repeat ⚙️, Stop Bookmark 🔖)
    var floatingPopoverVerse by remember { mutableStateOf<OfflineVerse?>(null) }
    // Full Tafsir Al-Muyassar Bottom Sheet (opened when user clicks 🔎 in the floating popover)
    var selectedVerseForSheet by remember { mutableStateOf<OfflineVerse?>(null) }
    var showAudioSettingsDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSurahIndexDialog by remember { mutableStateOf(false) }

    // Synchronize if parent requests jumping to a specific page
    LaunchedEffect(initialPage) {
        if (initialPage in 1..604 && initialPage != currentPage) {
            pagerState.scrollToPage(initialPage - 1)
        }
    }

    // When audio player advances to an Ayah on a different page, scroll pager automatically
    LaunchedEffect(audioPlaybackState.currentPage, audioPlaybackState.isPlaying) {
        if (audioPlaybackState.isPlaying && !audioPlaybackState.isFullSurahMode) {
            val targetP = audioPlaybackState.currentPage.coerceIn(1, 604)
            if (targetP != currentPage) {
                pagerState.animateScrollToPage(targetP - 1)
            }
        }
    }

    val currentPageVerses = remember(currentPage, isDatabaseReady) {
        if (isDatabaseReady) OfflineQuranDatabase.getVersesForPage(context, currentPage) else emptyList()
    }

    val currentSurahInfo = remember(currentPage, currentPageVerses) {
        val firstVerse = currentPageVerses.firstOrNull()
        if (firstVerse != null) {
            QuranDataHelper.surahs.find { it.number == firstVerse.surahNumber }
                ?: QuranDataHelper.pageToSurahAndAyahEstimate(currentPage).first
        } else {
            QuranDataHelper.pageToSurahAndAyahEstimate(currentPage).first
        }
    }

    val currentJuz = remember(currentPage, currentPageVerses) {
        currentPageVerses.firstOrNull()?.juz ?: (((currentPage - 1) / 20) + 1).coerceIn(1, 30)
    }

    // Helper that silently saves the current reading stop-point automatically
    fun persistAutoStopPoint(surahName: String, ayahNumber: Int, pageNumber: Int) {
        bookmarkedSurah = surahName
        bookmarkedAyah = ayahNumber
        bookmarkedPage = pageNumber
        quranPrefs.edit()
            .putInt("last_viewed_page_$activeTrackId", pageNumber)
            .putInt("last_viewed_page", pageNumber)
            .putInt("last_read_page_$activeTrackId", pageNumber)
            .putString("last_read_surah_$activeTrackId", surahName)
            .putInt("last_read_ayah_$activeTrackId", ayahNumber)
            .putInt("last_read_page", pageNumber)
            .putString("last_read_surah", surahName)
            .putInt("last_read_ayah", ayahNumber)
            .apply()
        onAutoSaveStopBookmark(surahName, ayahNumber, pageNumber)
    }

    // Automatically save stop-point whenever user flips pages or taps a verse
    LaunchedEffect(currentPage, currentSurahInfo, floatingPopoverVerse, activeTrackId) {
        if (isDatabaseReady) {
            val targetVerse = floatingPopoverVerse?.takeIf { it.page == currentPage }
                ?: currentPageVerses.firstOrNull()
            val sName = targetVerse?.surahNameAr ?: currentSurahInfo.nameArabic
            val aNum = targetVerse?.ayahNumber ?: 1
            persistAutoStopPoint(sName, aNum, currentPage)
        }
    }

    // Also guarantee auto-save when the user exits the Mushaf screen without pressing Save
    val latestPageRef by rememberUpdatedState(currentPage)
    val latestSurahRef by rememberUpdatedState(
        floatingPopoverVerse?.takeIf { it.page == currentPage }?.surahNameAr ?: currentSurahInfo.nameArabic
    )
    val latestAyahRef by rememberUpdatedState(
        floatingPopoverVerse?.takeIf { it.page == currentPage }?.ayahNumber
            ?: (currentPageVerses.firstOrNull()?.ayahNumber ?: 1)
    )

    DisposableEffect(activeTrackId) {
        onDispose {
            persistAutoStopPoint(latestSurahRef, latestAyahRef, latestPageRef)
        }
    }

    val pageBgColor = if (isNightMushafMode) Color(0xFF0B1520) else Color(0xFFFDF8EE)
    val pageCardColor = if (isNightMushafMode) Color(0xFF101F2E) else Color(0xFFFFFBF2)
    val textColor = if (isNightMushafMode) Color(0xFFF5EFE0) else Color(0xFF1B1612)
    val secondaryTextColor = if (isNightMushafMode) Color(0xFFB8C7D9) else Color(0xFF5C4E3C)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBgColor)
            .then(
                if (isFullScreen) Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                else Modifier
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Control Bar (Collapsible in Full Screen Mode)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .border(1.dp, GoldBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Row 1: Page Navigation, Track Badge & Save Stop Point Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentPage > 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(currentPage - 2)
                                            }
                                        }
                                    },
                                    enabled = currentPage > 1,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NavigateNext,
                                        contentDescription = "الصفحة السابقة",
                                        tint = if (currentPage > 1) GoldAccent else GoldAccent.copy(alpha = 0.3f)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showSurahIndexDialog = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "سورة ${currentSurahInfo.nameArabic}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "صفحة $currentPage من 604 • الجزء $currentJuz (حفظ تلقائي ✓)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldGreen,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (currentPage < 604) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(currentPage)
                                            }
                                        }
                                    },
                                    enabled = currentPage < 604,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NavigateBefore,
                                        contentDescription = "الصفحة التالية",
                                        tint = if (currentPage < 604) GoldAccent else GoldAccent.copy(alpha = 0.3f)
                                    )
                                }
                            }

                            // Save Stop Bookmark Button (also logs reflection)
                            Button(
                                onClick = {
                                    val targetVerse = floatingPopoverVerse
                                        ?: selectedVerseForSheet
                                        ?: currentPageVerses.lastOrNull()
                                    val sName = targetVerse?.surahNameAr ?: currentSurahInfo.nameArabic
                                    val aNum = targetVerse?.ayahNumber ?: 1
                                    persistAutoStopPoint(sName, aNum, currentPage)
                                    onSaveStopBookmark(sName, aNum, currentPage)
                                    Toast.makeText(
                                        context,
                                        "تم حفظ علامة الوقف: سورة $sName آية $aNum (صفحة $currentPage)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 38.dp)
                                    .testTag("save_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "حفظ الوقف",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 2: Quick Action Toolbar (Index, Search, Audio Reciter Settings, Display Mode, Font, Theme)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Surah Index Button
                                OutlinedButton(
                                    onClick = { showSurahIndexDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "فهرس السور",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "الفهرس",
                                        fontSize = 11.5.sp,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Search Ayahs Offline Button
                                OutlinedButton(
                                    onClick = { showSearchDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "بحث في المصحف",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "بحث",
                                        fontSize = 11.5.sp,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Audio & Reciter Settings Button (mp3quran.net)
                                OutlinedButton(
                                    onClick = { showAudioSettingsDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, EmeraldGreen),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = EmeraldGreen.copy(alpha = 0.12f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("open_quran_audio_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = "إعدادات القارئ والتكرار",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "القارئ والتكرار",
                                        fontSize = 11.5.sp,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Toggle Page Mode vs Ayah-by-Ayah Mode
                                IconButton(
                                    onClick = {
                                        isAyahByAyahMode = !isAyahByAyahMode
                                        quranPrefs.edit().putBoolean("ayah_by_ayah_mode", isAyahByAyahMode).apply()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAyahByAyahMode) Icons.Default.MenuBook else Icons.Default.ViewAgenda,
                                        contentDescription = "تبديل طريقة العرض",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Font Size Cycle Button
                                IconButton(
                                    onClick = {
                                        fontScaleSp = when {
                                            fontScaleSp < 26f -> 28f
                                            fontScaleSp < 30f -> 31f
                                            fontScaleSp < 33f -> 34f
                                            else -> 24f
                                        }
                                        quranPrefs.edit().putFloat("mushaf_font_size_sp_v2", fontScaleSp).apply()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "حجم الخط",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Night / Parchment Mode Toggle
                                IconButton(
                                    onClick = {
                                        isNightMushafMode = !isNightMushafMode
                                        quranPrefs.edit().putBoolean("night_mushaf_mode", isNightMushafMode).apply()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isNightMushafMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "الوضع الليلي للمصحف",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 3: Prominent Dedicated Full-Screen Mushaf Button
                        Button(
                            onClick = { onToggleFullScreen(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalNavyDark,
                                contentColor = GoldAccent
                            ),
                            border = BorderStroke(1.dp, GoldAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 36.dp)
                                .testTag("toggle_fullscreen_mushaf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "ملء الشاشة",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فتح المصحف في وضع ملء الشاشة (Full Screen)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                    }
                }
            }

            // Active Online Audio Player Control Bar (shown only when streaming or loading or error)
            AnimatedVisibility(
                visible = audioPlaybackState.isPlaying || audioPlaybackState.isLoading || audioPlaybackState.errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = RoyalNavyDark,
                    border = BorderStroke(1.dp, EmeraldGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (audioPlaybackState.isFullSurahMode)
                                        "بث مباشر للسورة كاملة: سورة ${audioPlaybackState.currentSurahNameAr}"
                                    else
                                        "سورة ${audioPlaybackState.currentSurahNameAr} • الآية ${toArabicIndicDigits(audioPlaybackState.currentAyahNumber)}",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val repeatInfo = if (audioPlaybackState.targetRepeatCount == -1) {
                                    "تكرار مستمر (${audioPlaybackState.currentRepeatIteration})"
                                } else {
                                    "تكرار ${audioPlaybackState.currentRepeatIteration}/${audioPlaybackState.targetRepeatCount}"
                                }
                                Text(
                                    text = "القارئ: ${audioPlaybackState.selectedReciter.nameAr} • $repeatInfo",
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { QuranAudioPlayerManager.playPreviousAyah(context) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "الآية السابقة", tint = GoldAccent)
                                }

                                IconButton(
                                    onClick = { QuranAudioPlayerManager.togglePlayPause() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    if (audioPlaybackState.isLoading) {
                                        CircularProgressIndicator(
                                            color = GoldAccent,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (audioPlaybackState.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                            contentDescription = "تشغيل / إيقاف مؤقت",
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { QuranAudioPlayerManager.playNextAyah(context) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "الآية التالية", tint = GoldAccent)
                                }

                                IconButton(
                                    onClick = { showAudioSettingsDialog = true },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "إعدادات الصوت", tint = GoldAccent, modifier = Modifier.size(17.dp))
                                }

                                IconButton(
                                    onClick = { QuranAudioPlayerManager.stopPlayback() },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.StopCircle, contentDescription = "إيقاف المشغل", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
                                }
                            }
                        }

                        if (!audioPlaybackState.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = audioPlaybackState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Main Offline Mushaf Viewport (604 Pages Horizontal Pager)
            if (!isDatabaseReady) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "جاري تجهيز المصحف الشريف والتفسير الميسر (أوفلاين بالكامل)...",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("offline_mushaf_pager")
                ) { pageIndex ->
                    val pageNum = pageIndex + 1
                    val versesOnThisPage = remember(pageNum, isDatabaseReady) {
                        OfflineQuranDatabase.getVersesForPage(context, pageNum)
                    }

                    val activePlayingVerse = if (audioPlaybackState.isPlaying && !audioPlaybackState.isFullSurahMode) {
                        versesOnThisPage.find {
                            it.surahNumber == audioPlaybackState.currentSurahNumber &&
                                    it.ayahNumber == audioPlaybackState.currentAyahNumber
                        }
                    } else null

                    MushafPageContent(
                        pageNumber = pageNum,
                        verses = versesOnThisPage,
                        isAyahByAyahMode = isAyahByAyahMode,
                        fontScaleSp = fontScaleSp,
                        pageCardColor = pageCardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        isNightMode = isNightMushafMode,
                        isFullScreen = isFullScreen,
                        onExitFullScreen = { onToggleFullScreen(false) },
                        bookmarkedSurah = bookmarkedSurah,
                        bookmarkedAyah = bookmarkedAyah,
                        memorizedAyahKeys = memorizedAyahKeys,
                        selectedVerse = floatingPopoverVerse ?: activePlayingVerse ?: selectedVerseForSheet,
                        onVerseClicked = { clickedVerse ->
                            floatingPopoverVerse = clickedVerse
                            persistAutoStopPoint(clickedVerse.surahNameAr, clickedVerse.ayahNumber, clickedVerse.page)
                        }
                    )
                }
            }
        }
    }

    // TRUE FLOATING POPUP CARD WHEN USER TAPS ANY AYAH (Floats over center of screen, NOT at top of page)
    floatingPopoverVerse?.let { tappedVerse ->
        val isThisVersePlaying = audioPlaybackState.isPlaying &&
                audioPlaybackState.currentSurahNumber == tappedVerse.surahNumber &&
                audioPlaybackState.currentAyahNumber == tappedVerse.ayahNumber &&
                !audioPlaybackState.isFullSurahMode
        val isThisVerseLoading = audioPlaybackState.isLoading &&
                audioPlaybackState.currentSurahNumber == tappedVerse.surahNumber &&
                audioPlaybackState.currentAyahNumber == tappedVerse.ayahNumber
        val isMemorized = memorizedAyahKeys.contains("${tappedVerse.surahNumber}:${tappedVerse.ayahNumber}")
        val isStopBookmark = (bookmarkedSurah == tappedVerse.surahNameAr && bookmarkedAyah == tappedVerse.ayahNumber)

        Dialog(
            onDismissRequest = { floatingPopoverVerse = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(2.dp, GoldAccent, RoundedCornerShape(22.dp))
                    .testTag("floating_ayah_popover_box"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalNavyDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Header Row: Surah + Ayah Number + Page Number + Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldAccent
                            ) {
                                Text(
                                    text = "سورة ${tappedVerse.surahNameAr} • الآية ${toArabicIndicDigits(tappedVerse.ayahNumber)}",
                                    color = RoyalNavyDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldGreen.copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, EmeraldGreen)
                            ) {
                                Text(
                                    text = "صفحة ${toArabicIndicDigits(tappedVerse.page)}",
                                    color = Color(0xFFFDF8EE),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { floatingPopoverVerse = null },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المربع العائم",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 2. Vocalized Ayah Text Preview Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF15263F),
                        border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "﴿ ${tappedVerse.textAr} ﴾",
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            lineHeight = 31.sp,
                            color = Color(0xFFFDF8EE),
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }

                    // 3. Row 1: Audio Playback (تشغيل صوتي) + Reciter/Repeat Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (isThisVersePlaying) {
                                    QuranAudioPlayerManager.togglePlayPause()
                                } else {
                                    QuranAudioPlayerManager.playAyahOnline(
                                        context = context,
                                        surahNumber = tappedVerse.surahNumber,
                                        surahNameAr = tappedVerse.surahNameAr,
                                        ayahNumber = tappedVerse.ayahNumber,
                                        page = tappedVerse.page
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("popover_play_ayah_button")
                        ) {
                            if (isThisVerseLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isThisVersePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل الآية صوتياً",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isThisVersePlaying) "إيقاف مؤقت" else "تشغيل صوتي للآية",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { showAudioSettingsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("popover_audio_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "القارئ والتكرار",
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "القارئ / تكرار",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 4. Row 2: Bookmarks & Memorization Marks (علامات الحفظ والوقف)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                persistAutoStopPoint(tappedVerse.surahNameAr, tappedVerse.ayahNumber, tappedVerse.page)
                                onSaveStopBookmark(tappedVerse.surahNameAr, tappedVerse.ayahNumber, tappedVerse.page)
                                Toast.makeText(
                                    context,
                                    "تم حفظ علامة الوقف عند الآية ${tappedVerse.ayahNumber} من سورة ${tappedVerse.surahNameAr}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                floatingPopoverVerse = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStopBookmark) GoldAccent else Color(0xFF1F3A5F),
                                contentColor = if (isStopBookmark) RoyalNavyDark else GoldAccent
                            ),
                            border = BorderStroke(1.dp, GoldAccent),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isStopBookmark) Icons.Default.BookmarkAdded else Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isStopBookmark) "موضع الوقف ✓" else "علامة الوقف",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                val added = toggleMemorizedAyah(tappedVerse.surahNumber, tappedVerse.ayahNumber)
                                Toast.makeText(
                                    context,
                                    if (added) "تم وضع علامة الحفظ ✓" else "تم إزالة علامة الحفظ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMemorized) EmeraldGreen else Color(0xFF1F3A5F),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, if (isMemorized) EmeraldGreen else GoldBorder),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isMemorized) Icons.Default.CheckCircle else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isMemorized) "تم الحفظ ✓" else "علامة الحفظ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 5. Row 3: Copy Ayah & Share Ayah as Full Text (نسخ الآية + مشاركة الآية مع اسم السورة ورقم الآية)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val formattedAyah = "﴿ ${tappedVerse.textAr} ﴾\n[سورة ${tappedVerse.surahNameAr} - الآية ${toArabicIndicDigits(tappedVerse.ayahNumber)}]"
                                clipboard.setPrimaryClip(ClipData.newPlainText("آية كريمة", formattedAyah))
                                Toast.makeText(
                                    context,
                                    "تم نسخ الآية الكريمة (سورة ${tappedVerse.surahNameAr} - آية ${tappedVerse.ayahNumber})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, GoldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("popover_copy_ayah_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ الآية",
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "نسخ الآية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val shareBody = buildString {
                                    append("﴿ ${tappedVerse.textAr} ﴾\n\n")
                                    append("📖 سورة ${tappedVerse.surahNameAr} - الآية ${toArabicIndicDigits(tappedVerse.ayahNumber)} (صفحة ${toArabicIndicDigits(tappedVerse.page)})\n")
                                    append("— تطبيق محرابي")
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareBody)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة الآية الكريمة"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, EmeraldGreen),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = EmeraldGreen.copy(alpha = 0.16f),
                                contentColor = Color(0xFFFDF8EE)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("popover_share_ayah_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة الآية",
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "مشاركة الآية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 6. Row 4: Offline Tafsir Al-Muyassar Button (+ Exit Fullscreen if in Fullscreen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val v = tappedVerse
                                floatingPopoverVerse = null
                                selectedVerseForSheet = v
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = RoyalNavyDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("popover_tafsir_muyassar_button")
                        ) {
                            Text(
                                text = "🔎 عرض التفسير الميسر للآية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalNavyDark,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (isFullScreen) {
                            OutlinedButton(
                                onClick = {
                                    floatingPopoverVerse = null
                                    onToggleFullScreen(false)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GoldAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("exit_fullscreen_mushaf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "تصغير الشاشة",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصغير", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Audio & Reciter Settings Dialog (mp3quran.net Reciter Selection & Ayah Repeat Settings)
    if (showAudioSettingsDialog) {
        QuranAudioSettingsDialog(
            currentSurahNumber = floatingPopoverVerse?.surahNumber ?: currentSurahInfo.number,
            currentSurahNameAr = floatingPopoverVerse?.surahNameAr ?: currentSurahInfo.nameArabic,
            currentPage = currentPage,
            onDismiss = { showAudioSettingsDialog = false }
        )
    }

    // Interactive Offline Tafsir Al-Muyassar Bottom Sheet (when user clicks 🔎 التفسير الميسر)
    selectedVerseForSheet?.let { verse ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isStopBookmark = (bookmarkedSurah == verse.surahNameAr && bookmarkedAyah == verse.ayahNumber)
        val isMemorized = memorizedAyahKeys.contains("${verse.surahNumber}:${verse.ayahNumber}")
        val asbabNuzul = remember(verse.surahNameAr) {
            OfflineQuranDatabase.getSurahAsbabAlNuzul(context, verse.surahNameAr)
        }

        ModalBottomSheet(
            onDismissRequest = { selectedVerseForSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                // Header Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = RoyalNavyDark,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Text(
                                text = "سورة ${verse.surahNameAr} • آية ${toArabicIndicDigits(verse.ayahNumber)}",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "صفحة ${verse.page} • الجزء ${verse.juz}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = { selectedVerseForSheet = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = GoldAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selected Ayah Uthmani Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = RoyalNavyDark.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "﴿ ${verse.textAr} ﴾",
                        fontFamily = AmiriFontFamily,
                        fontSize = 23.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDF8EE),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Play Audio Button inside Tafsir Sheet too
                Button(
                    onClick = {
                        QuranAudioPlayerManager.playAyahOnline(
                            context = context,
                            surahNumber = verse.surahNumber,
                            surahNameAr = verse.surahNameAr,
                            ayahNumber = verse.ayahNumber,
                            page = verse.page
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalNavyDark,
                        contentColor = GoldAccent
                    ),
                    border = BorderStroke(1.dp, GoldAccent)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "▶️ استماع لتلاوة الآية بصوت ${audioPlaybackState.selectedReciter.nameAr}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Action Buttons for this Ayah:
                // 1) Set as Stop Bookmark (علامة قراءة / وقف الورد)
                // 2) Mark as Memorized/Saved (علامة حفظ ومراجعة)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            persistAutoStopPoint(verse.surahNameAr, verse.ayahNumber, verse.page)
                            onSaveStopBookmark(verse.surahNameAr, verse.ayahNumber, verse.page)
                            Toast.makeText(
                                context,
                                "تم تسجيل علامة الوقف عند سورة ${verse.surahNameAr} آية ${verse.ayahNumber}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStopBookmark) GoldAccent else EmeraldGreen,
                            contentColor = if (isStopBookmark) RoyalNavyDark else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isStopBookmark) Icons.Default.BookmarkAdded else Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStopBookmark) "موضع الوقف الحالي" else "وضع علامة قراءة ووقف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val nowMemorized = toggleMemorizedAyah(verse.surahNumber, verse.ayahNumber)
                            val msg = if (nowMemorized) {
                                "تمت إضافة الآية ${verse.ayahNumber} من سورة ${verse.surahNameAr} إلى محفوظاتك"
                            } else {
                                "تم إزالة علامة الحفظ عن الآية"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, if (isMemorized) EmeraldGreen else GoldAccent),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isMemorized) EmeraldGreen.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (isMemorized) EmeraldGreen else GoldAccent
                        )
                    ) {
                        Icon(
                            imageVector = if (isMemorized) Icons.Default.CheckCircle else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMemorized) "تم الحفظ بعون الله" else "علامة حفظ ومراجعة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Authentic Offline Tafsir Al-Muyassar Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔎 التفسير الميسر للآية الكريمة (أوفلاين):",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verse.tafsirMuyassar.ifBlank {
                                OfflineQuranDatabase.getSurahTafsirOverview(context, verse.surahNameAr)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (verse.textEn.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = GoldBorder.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "الترجمة الإنجليزية للمعاني:",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = verse.textEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = GoldBorder.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "مقاصد السورة وأسباب النزول:",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = asbabNuzul,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Copy & Share Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val copyString = "﴿ ${verse.textAr} ﴾ [سورة ${verse.surahNameAr}: ${verse.ayahNumber}]\n\nالتفسير الميسر:\n${verse.tafsirMuyassar}"
                            clipboard.setPrimaryClip(ClipData.newPlainText("آية وتفسير", copyString))
                            Toast.makeText(context, "تم نسخ الآية الكريمة مع التفسير", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldBorder)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ الآية والتفسير", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = "﴿ ${verse.textAr} ﴾\n[سورة ${verse.surahNameAr} - الآية ${verse.ayahNumber} - صفحة ${verse.page}]\n\nالتفسير الميسر:\n${verse.tafsirMuyassar}\n\n— تطبيق محرابي"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة الآية والتفسير"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة التدبر", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Surah Index & Direct Page Jump Dialog
    if (showSurahIndexDialog) {
        var pageJumpInput by remember { mutableStateOf(currentPage.toString()) }
        var surahFilter by remember { mutableStateOf("") }

        val filteredSurahs = remember(surahFilter) {
            if (surahFilter.isBlank()) QuranDataHelper.surahs
            else QuranDataHelper.surahs.filter {
                it.nameArabic.contains(surahFilter.trim()) || it.number.toString() == surahFilter.trim()
            }
        }

        AlertDialog(
            onDismissRequest = { showSurahIndexDialog = false },
            title = {
                Text(
                    text = "فهرس السور والانتقال المباشر",
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick Page Jump Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pageJumpInput,
                            onValueChange = { pageJumpInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("رقم الصفحة (1-604)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val target = pageJumpInput.toIntOrNull()?.coerceIn(1, 604) ?: currentPage
                                coroutineScope.launch {
                                    pagerState.scrollToPage(target - 1)
                                }
                                showSurahIndexDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("انتقال", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = surahFilter,
                        onValueChange = { surahFilter = it },
                        label = { Text("ابحث باسم السورة أو رقمها...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage((surah.startPage - 1).coerceIn(0, 603))
                                        }
                                        showSurahIndexDialog = false
                                    }
                                    .border(0.5.dp, GoldBorder, RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = RoyalNavyDark,
                                            border = BorderStroke(0.5.dp, GoldAccent)
                                        ) {
                                            Text(
                                                text = "${surah.number}",
                                                color = GoldAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "سورة ${surah.nameArabic}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "عدد آياتها: ${surah.totalAyahs}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "صفحة ${surah.startPage}",
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSurahIndexDialog = false }) {
                    Text("إغلاق", color = GoldAccent)
                }
            }
        )
    }

    // Offline Full-Quran Verse Search Dialog
    if (showSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val searchResults = remember(searchQuery) {
            if (searchQuery.trim().length >= 2) {
                OfflineQuranDatabase.searchVerses(context, searchQuery, limit = 40)
            } else {
                emptyList()
            }
        }

        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = {
                Text(
                    text = "البحث الفوري في آيات المصحف الشريف",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("اكتب كلمة أو جزءاً من آية...") },
                        placeholder = { Text("مثال: الصراط المستقيم، الله نور، شهر رمضان") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (searchQuery.trim().length >= 2 && searchResults.isEmpty()) {
                        Text(
                            text = "لم يتم العثور على آيات مطابقة.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { v ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            showSearchDialog = false
                                            coroutineScope.launch {
                                                pagerState.scrollToPage((v.page - 1).coerceIn(0, 603))
                                                floatingPopoverVerse = v
                                            }
                                        }
                                        .border(0.5.dp, GoldBorder, RoundedCornerShape(10.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "سورة ${v.surahNameAr} • آية ${v.ayahNumber}",
                                                color = GoldAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "صفحة ${v.page}",
                                                color = EmeraldGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = v.textAr,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("إغلاق", color = GoldAccent)
                }
            }
        )
    }
}

@Composable
private fun QuranAudioSettingsDialog(
    currentSurahNumber: Int,
    currentSurahNameAr: String,
    currentPage: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playbackState by QuranAudioPlayerManager.playbackState.collectAsState()

    val repeatOptions = listOf(
        1 to "مرة واحدة (1x)",
        2 to "مرتان (2x)",
        3 to "3 مرات (3x)",
        5 to "5 مرات (5x)",
        7 to "7 مرات (7x)",
        -1 to "تكرار مستمر للحفظ (∞)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "إعدادات مشغل التلاوة الصوتية (mp3quran.net)",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "بث صوتي مباشر أونلاين بدون تحميل على ذاكرة الهاتف",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldGreen,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Full Surah Stream from mp3quran.net Button
                Button(
                    onClick = {
                        QuranAudioPlayerManager.playFullSurahFromMp3Quran(
                            context = context,
                            surahNumber = currentSurahNumber,
                            surahNameAr = currentSurahNameAr,
                            page = currentPage
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تشغيل سورة $currentSurahNameAr كاملة (mp3quran.net)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Repeat Count Section
                Text(
                    text = "عدد مرات تكرار الآية (للتحفيظ والتدبر):",
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    fontSize = 13.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeatOptions.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (countVal, label) ->
                                val selected = playbackState.targetRepeatCount == countVal
                                FilterChip(
                                    selected = selected,
                                    onClick = { QuranAudioPlayerManager.setRepeatCount(context, countVal) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Auto-Advance Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الانتقال التلقائي للآية التالية بعد التكرار",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = playbackState.autoAdvanceNextAyah,
                        onCheckedChange = { QuranAudioPlayerManager.setAutoAdvanceNextAyah(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen
                        )
                    )
                }

                HorizontalDivider(color = GoldBorder.copy(alpha = 0.4f))

                // Reciter Selection List
                Text(
                    text = "اختر القارئ المفضل (مصدر mp3quran.net):",
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    fontSize = 13.sp
                )

                QuranAudioPlayerManager.reciters.forEach { reciter ->
                    val isSelected = playbackState.selectedReciter.id == reciter.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.7.dp,
                            color = if (isSelected) EmeraldGreen else GoldBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                QuranAudioPlayerManager.selectReciter(context, reciter)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { QuranAudioPlayerManager.selectReciter(context, reciter) },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "الشيخ ${reciter.nameAr}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = reciter.styleAr,
                                    fontSize = 10.5.sp,
                                    color = GoldAccent
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("تم الحفظ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MushafPageContent(
    pageNumber: Int,
    verses: List<OfflineVerse>,
    isAyahByAyahMode: Boolean,
    fontScaleSp: Float,
    pageCardColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    isNightMode: Boolean,
    isFullScreen: Boolean = false,
    onExitFullScreen: () -> Unit = {},
    bookmarkedSurah: String,
    bookmarkedAyah: Int,
    memorizedAyahKeys: Set<String>,
    selectedVerse: OfflineVerse?,
    onVerseClicked: (OfflineVerse) -> Unit
) {
    val firstVerse = verses.firstOrNull()
    val surahName = firstVerse?.surahNameAr ?: ""
    val juzNumber = firstVerse?.juz ?: 1
    val arabicPageNumber = remember(pageNumber) { toArabicIndicDigits(pageNumber) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isFullScreen) 4.dp else 6.dp, vertical = 2.dp)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(GoldAccent, GoldBorder, GoldAccent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(3.dp)
            .border(0.8.dp, GoldBorder, RoundedCornerShape(13.dp))
            .clip(RoundedCornerShape(13.dp))
            .background(pageCardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Ornamental Mushaf Page Top Header Bar (Juz + Page Number + Surah Name + Optional FullScreen Exit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.7.dp, GoldBorder, RoundedCornerShape(8.dp))
                    .background(
                        if (isNightMode) Color(0xFF162A3F).copy(alpha = 0.72f)
                        else Color(0xFFF3E9D2).copy(alpha = 0.78f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الجزء ${toArabicIndicDigits(juzNumber)}",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (isNightMode) GoldAccent else EmeraldGreen
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isNightMode) RoyalNavyDark else Color(0xFFE7D6B5),
                        border = BorderStroke(0.8.dp, GoldAccent)
                    ) {
                        Text(
                            text = "صفحة $arabicPageNumber",
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (isNightMode) GoldAccent else Color(0xFF1B1612),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
                        )
                    }

                    if (isFullScreen) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = RoyalNavyDark,
                            border = BorderStroke(0.8.dp, GoldAccent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onExitFullScreen() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "تصغير الشاشة",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "تصغير",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "سُورَةُ $surahName",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isNightMode) GoldAccent else EmeraldGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Main Verses Single-Page Auto-Fit Viewport (Fits entire page on ONE screen with bold, heavy font)
            if (!isAyahByAyahMode) {
                val surahGroups = remember(verses) {
                    val groups = mutableListOf<Pair<OfflineVerse, List<OfflineVerse>>>()
                    var currentList = mutableListOf<OfflineVerse>()
                    var currentSurahNum = -1
                    for (v in verses) {
                        if (v.surahNumber != currentSurahNum) {
                            if (currentList.isNotEmpty()) {
                                groups.add(Pair(currentList.first(), currentList))
                            }
                            currentList = mutableListOf(v)
                            currentSurahNum = v.surahNumber
                        } else {
                            currentList.add(v)
                        }
                    }
                    if (currentList.isNotEmpty()) {
                        groups.add(Pair(currentList.first(), currentList))
                    }
                    groups
                }

                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val availWidthPx = constraints.maxWidth.coerceAtLeast(100)
                    val availHeightPx = constraints.maxHeight.coerceAtLeast(100)

                    // Automatically compute the largest BOLD font size that fits 100% of this page's verses
                    // on a single screen without requiring vertical scrolling!
                    val autoFittedFontSp = remember(
                        pageNumber,
                        verses,
                        availWidthPx,
                        availHeightPx,
                        bookmarkedSurah,
                        bookmarkedAyah,
                        memorizedAyahKeys
                    ) {
                        var bannerOverheadPx = 0f
                        val groupStrings = surahGroups.map { (firstV, groupVerses) ->
                            if (firstV.ayahNumber == 1) {
                                bannerOverheadPx += with(density) { 40.dp.toPx() }
                                if (firstV.surahNumber != 1 && firstV.surahNumber != 9) {
                                    bannerOverheadPx += with(density) { 26.dp.toPx() }
                                }
                            }
                            buildString {
                                groupVerses.forEach { v ->
                                    append(v.textAr)
                                    val isBookmarked = (bookmarkedSurah == v.surahNameAr && bookmarkedAyah == v.ayahNumber)
                                    val isMemorized = memorizedAyahKeys.contains("${v.surahNumber}:${v.ayahNumber}")
                                    val badgeSymbol = when {
                                        isBookmarked -> " ﴿${toArabicIndicDigits(v.ayahNumber)}•وقف﴾ "
                                        isMemorized -> " ﴿${toArabicIndicDigits(v.ayahNumber)}✓﴾ "
                                        else -> " ﴿${toArabicIndicDigits(v.ayahNumber)}﴾ "
                                    }
                                    append(badgeSymbol)
                                }
                            }
                        }

                        val targetMaxHeightPx = (availHeightPx - bannerOverheadPx - with(density) { 8.dp.toPx() })
                            .coerceAtLeast(80f)

                        var chosenSp = 20f
                        var candidate = 31f
                        while (candidate >= 14.5f) {
                            var measuredTotalHeight = 0
                            for (gStr in groupStrings) {
                                val res = textMeasurer.measure(
                                    text = gStr,
                                    style = TextStyle(
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = candidate.sp,
                                        lineHeight = (candidate * 1.34f).sp,
                                        textAlign = TextAlign.Justify,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    constraints = Constraints(maxWidth = availWidthPx)
                                )
                                measuredTotalHeight += res.size.height + with(density) { 4.dp.roundToPx() }
                            }
                            if (measuredTotalHeight <= targetMaxHeightPx) {
                                chosenSp = candidate
                                break
                            }
                            candidate -= 0.5f
                        }
                        chosenSp
                    }

                    val effectiveLineHeightSp = autoFittedFontSp * 1.34f

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        surahGroups.forEach { (firstV, groupVerses) ->
                            // Show Ornamental Surah Header if Ayah 1 is on this page
                            if (firstV.ayahNumber == 1) {
                                SurahOrnamentalBanner(
                                    surahNameAr = firstV.surahNameAr,
                                    revelationPlaceAr = firstV.revelationPlaceAr,
                                    isNightMode = isNightMode
                                )
                                if (firstV.surahNumber != 1 && firstV.surahNumber != 9) {
                                    Text(
                                        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (autoFittedFontSp - 0.5f).coerceAtLeast(15f).sp,
                                        color = if (isNightMode) GoldAccent else EmeraldGreen,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }

                            val annotatedPageText = buildAnnotatedString {
                                groupVerses.forEach { v ->
                                    val tagKey = "${v.surahNumber}:${v.ayahNumber}"
                                    val isBookmarked = (bookmarkedSurah == v.surahNameAr && bookmarkedAyah == v.ayahNumber)
                                    val isMemorized = memorizedAyahKeys.contains(tagKey)
                                    val isSelected = (selectedVerse?.surahNumber == v.surahNumber && selectedVerse.ayahNumber == v.ayahNumber)

                                    val bgHighlight = when {
                                        isSelected -> GoldAccent.copy(alpha = 0.35f)
                                        isBookmarked -> EmeraldGreen.copy(alpha = 0.25f)
                                        isMemorized -> GoldAccent.copy(alpha = 0.16f)
                                        else -> Color.Transparent
                                    }

                                    pushStringAnnotation(tag = "AYAH_TAP", annotation = tagKey)
                                    withStyle(
                                        style = SpanStyle(
                                            color = textColor,
                                            background = bgHighlight,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = autoFittedFontSp.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(v.textAr)
                                    }
                                    withStyle(
                                        style = SpanStyle(
                                            color = when {
                                                isBookmarked -> EmeraldGreen
                                                isMemorized -> GoldAccent
                                                isNightMode -> GoldAccent
                                                else -> Color(0xFFA67B14)
                                            },
                                            background = bgHighlight,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = (autoFittedFontSp - 1.5f).coerceAtLeast(13f).sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    ) {
                                        val badgeSymbol = when {
                                            isBookmarked -> " ﴿${toArabicIndicDigits(v.ayahNumber)}•وقف﴾ "
                                            isMemorized -> " ﴿${toArabicIndicDigits(v.ayahNumber)}✓﴾ "
                                            else -> " ﴿${toArabicIndicDigits(v.ayahNumber)}﴾ "
                                        }
                                        append(badgeSymbol)
                                    }
                                    pop()
                                }
                            }

                            ClickableText(
                                text = annotatedPageText,
                                style = TextStyle(
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = autoFittedFontSp.sp,
                                    lineHeight = effectiveLineHeightSp.sp,
                                    textAlign = TextAlign.Justify,
                                    textDirection = TextDirection.Rtl
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                onClick = { offset ->
                                    annotatedPageText.getStringAnnotations(
                                        tag = "AYAH_TAP",
                                        start = offset,
                                        end = offset
                                    ).firstOrNull()?.let { annotation ->
                                        val parts = annotation.item.split(":")
                                        val sNum = parts.getOrNull(0)?.toIntOrNull()
                                        val aNum = parts.getOrNull(1)?.toIntOrNull()
                                        val matched = groupVerses.find { it.surahNumber == sNum && it.ayahNumber == aNum }
                                        if (matched != null) {
                                            onVerseClicked(matched)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Ayah-by-Ayah Interactive Cards Mode on current page
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(verses) { v ->
                        val tagKey = "${v.surahNumber}:${v.ayahNumber}"
                        val isBookmarked = (bookmarkedSurah == v.surahNameAr && bookmarkedAyah == v.ayahNumber)
                        val isMemorized = memorizedAyahKeys.contains(tagKey)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onVerseClicked(v) }
                                .border(
                                    width = if (isBookmarked || isMemorized) 1.5.dp else 0.6.dp,
                                    color = when {
                                        isBookmarked -> EmeraldGreen
                                        isMemorized -> GoldAccent
                                        else -> GoldBorder
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNightMode) Color(0xFF162A3F).copy(alpha = 0.5f)
                                else Color(0xFFF6EFE0).copy(alpha = 0.6f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سورة ${v.surahNameAr} • الآية ${toArabicIndicDigits(v.ayahNumber)}",
                                        color = if (isNightMode) GoldAccent else EmeraldGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isBookmarked) {
                                            Surface(
                                                color = EmeraldGreen,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "علامة وقف",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (isMemorized) {
                                            Surface(
                                                color = GoldAccent,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "محفوظة ✓",
                                                    color = RoyalNavyDark,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${v.textAr} ﴿${toArabicIndicDigits(v.ayahNumber)}﴾",
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontScaleSp.sp,
                                    lineHeight = (fontScaleSp * 1.55f).sp,
                                    color = textColor,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (v.tafsirMuyassar.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isNightMode) Color(0xFF0E1D2F).copy(alpha = 0.85f)
                                        else Color(0xFFEDE2CC).copy(alpha = 0.72f),
                                        border = BorderStroke(0.8.dp, GoldBorder.copy(alpha = 0.55f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "📖 التفسير الميسر:",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isNightMode) GoldAccent else EmeraldGreen
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = v.tafsirMuyassar,
                                                fontSize = 16.5.sp,
                                                lineHeight = 26.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isNightMode) Color(0xFFFDF8EE) else Color(0xFF1F160A),
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 3. Pinned Ornamental Mushaf Bottom Page Number Medallion (Always visible on every page)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isNightMode) Color(0xFF162A3F) else Color(0xFFF3E9D2),
                    border = BorderStroke(1.dp, GoldAccent)
                ) {
                    Text(
                        text = "━━ ۞ صفحة $arabicPageNumber من ٦٠٤ ۞ ━━",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isNightMode) GoldAccent else Color(0xFF1B1612),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahOrnamentalBanner(
    surahNameAr: String,
    revelationPlaceAr: String,
    isNightMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isNightMode) RoyalNavyDark else Color(0xFFEDE0C4),
        border = BorderStroke(1.2.dp, GoldAccent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "۞",
                fontSize = 15.sp,
                color = GoldAccent
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "سُورَةُ $surahNameAr",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isNightMode) GoldAccent else EmeraldGreen
                )
                Text(
                    text = "(${revelationPlaceAr})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNightMode) Color.White.copy(alpha = 0.78f) else Color(0xFF5C4E3C)
                )
            }
            Text(
                text = "۞",
                fontSize = 15.sp,
                color = GoldAccent
            )
        }
    }
}
