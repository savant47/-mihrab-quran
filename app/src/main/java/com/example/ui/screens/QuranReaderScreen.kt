package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.audio.QuranAudioPlayerManager
import com.example.audio.QuranReciter
import com.example.data.OfflineQuranDatabase
import com.example.data.OfflineVerse
import com.example.data.QuranDataHelper
import com.example.data.QuranSearchResult
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
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
    isFixedSurahWerd: Boolean = false,
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

    val haptic = LocalHapticFeedback.current
    val audioPlaybackState by QuranAudioPlayerManager.playbackState.collectAsState()
    val activeWordPronunciation by QuranAudioPlayerManager.activeWordState.collectAsState()

    LaunchedEffect(backupVersion) {
        QuranAudioPlayerManager.initPreferences(context)
    }

    val savedLastPage = remember(activeTrackId, backupVersion, isFixedSurahWerd, initialPage) {
        if (isFixedSurahWerd) {
            initialPage.coerceIn(1, 604)
        } else if (initialPage in 1..604) {
            initialPage
        } else {
            quranPrefs.getInt("last_viewed_page_$activeTrackId", 1).coerceIn(1, 604)
        }
    }

    var isDatabaseReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        OfflineQuranDatabase.ensureLoaded(context)
        isDatabaseReady = true
    }

    val startPage = remember(activeTrackId, initialPage, backupVersion, isFixedSurahWerd) {
        if (isFixedSurahWerd) {
            initialPage.coerceIn(1, 604)
        } else if (initialPage in 1..604) {
            initialPage
        } else {
            savedLastPage
        }
    }

    val pagerState = key(activeTrackId) {
        rememberPagerState(
            initialPage = (startPage - 1).coerceIn(0, 603),
            pageCount = { 604 }
        )
    }

    val currentPage = pagerState.currentPage + 1
    var hasSettledInitialPage by remember(activeTrackId) { mutableStateOf(false) }

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
    var fontZoomStep by remember(backupVersion) {
        mutableIntStateOf(quranPrefs.getInt("mushaf_font_zoom_step", 0).coerceIn(0, 4))
    }

    // Single-tap reading focus state (strong light-gray highlight) + double-tap detector
    var focusedReadingVerseKey by remember(activeTrackId, backupVersion) {
        mutableStateOf(quranPrefs.getString("focused_verse_key_$activeTrackId", null))
    }
    var lastTappedVerseKey by remember { mutableStateOf<String?>(null) }
    var lastTapTimestampMs by remember { mutableLongStateOf(0L) }

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
    LaunchedEffect(initialPage, activeTrackId) {
        if (initialPage in 1..604 && initialPage != pagerState.currentPage + 1) {
            pagerState.scrollToPage(initialPage - 1)
        }
        hasSettledInitialPage = true
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
        if (isFixedSurahWerd) {
            val lockedStartPage = initialPage.coerceIn(1, 604)
            bookmarkedPage = lockedStartPage
            quranPrefs.edit()
                .putInt("last_viewed_page_$activeTrackId", lockedStartPage)
                .putInt("last_read_page_$activeTrackId", lockedStartPage)
                .apply()
            return
        }
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

    // Automatically save stop-point whenever user flips pages or taps a verse (after initial page has settled)
    LaunchedEffect(currentPage, currentSurahInfo, floatingPopoverVerse, activeTrackId, hasSettledInitialPage, isDatabaseReady) {
        if (isDatabaseReady && hasSettledInitialPage) {
            val targetVerse = floatingPopoverVerse?.takeIf { it.page == currentPage }
                ?: currentPageVerses.firstOrNull { it.surahNameAr == bookmarkedSurah && it.ayahNumber == bookmarkedAyah }
                ?: currentPageVerses.firstOrNull()
            val sName = targetVerse?.surahNameAr ?: currentSurahInfo.nameArabic
            val aNum = targetVerse?.ayahNumber ?: 1
            persistAutoStopPoint(sName, aNum, currentPage)
        }
    }

    // Also guarantee auto-save when the user exits the Mushaf screen without pressing Save
    val latestPageRef by rememberUpdatedState(currentPage)
    val latestSurahRef by rememberUpdatedState(
        floatingPopoverVerse?.takeIf { it.page == currentPage }?.surahNameAr
            ?: currentPageVerses.firstOrNull { it.surahNameAr == bookmarkedSurah && it.ayahNumber == bookmarkedAyah }?.surahNameAr
            ?: currentSurahInfo.nameArabic
    )
    val latestAyahRef by rememberUpdatedState(
        floatingPopoverVerse?.takeIf { it.page == currentPage }?.ayahNumber
            ?: currentPageVerses.firstOrNull { it.surahNameAr == bookmarkedSurah && it.ayahNumber == bookmarkedAyah }?.ayahNumber
            ?: (currentPageVerses.firstOrNull()?.ayahNumber ?: 1)
    )
    val settledRef by rememberUpdatedState(hasSettledInitialPage)

    DisposableEffect(activeTrackId) {
        onDispose {
            if (settledRef) {
                persistAutoStopPoint(latestSurahRef, latestAyahRef, latestPageRef)
            }
        }
    }

    val pageBgColor = if (isNightMushafMode) Color(0xFF0B1520) else Color(0xFFF6F7F9)
    val pageCardColor = if (isNightMushafMode) Color(0xFF101F2E) else Color(0xFFFFFFFF)
    val textColor = if (isNightMushafMode) Color(0xFFF5EFE0) else Color(0xFF111111)
    val secondaryTextColor = if (isNightMushafMode) Color(0xFFB8C7D9) else Color(0xFF4B5563)

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
                                    border = BorderStroke(1.dp, GoldAccent),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = GoldAccent.copy(alpha = 0.12f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("open_quran_search_button")
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

                                // Font Size Cycle Button (works in both Single-Page Mushaf Mode & Ayah-by-Ayah Mode)
                                IconButton(
                                    onClick = {
                                        fontZoomStep = (fontZoomStep + 1) % 5
                                        fontScaleSp = when (fontZoomStep) {
                                            0 -> 28f
                                            1 -> 30f
                                            2 -> 33f
                                            3 -> 36f
                                            else -> 24f
                                        }
                                        quranPrefs.edit()
                                            .putInt("mushaf_font_zoom_step", fontZoomStep)
                                            .putFloat("mushaf_font_size_sp_v2", fontScaleSp)
                                            .apply()
                                        val zoomLabel = when (fontZoomStep) {
                                            0 -> "حجم الخط: تلقائي (صفحة كاملة)"
                                            1 -> "حجم الخط: تكبير متوسط (+1)"
                                            2 -> "حجم الخط: تكبير كبير (+2)"
                                            3 -> "حجم الخط: تكبير كبير جداً (+3)"
                                            else -> "حجم الخط: تصغير مريح"
                                        }
                                        Toast.makeText(context, zoomLabel, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("mushaf_font_size_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "حجم الخط",
                                        tint = if (fontZoomStep == 0) GoldAccent else EmeraldGreen,
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

                        // Row 3: Concise Dedicated Full-Screen Mushaf Button
                        Button(
                            onClick = { onToggleFullScreen(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalNavyDark,
                                contentColor = EmeraldGreen
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
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وضع ملء الشاشة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
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

            // Subtle Non-Intrusive Word Pronunciation Status Pill (when long-pressing a single word)
            AnimatedVisibility(
                visible = activeWordPronunciation != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                activeWordPronunciation?.let { wordState ->
                    Surface(
                        color = Color(0xFFD1FAE5),
                        border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .testTag("active_word_pronunciation_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "نطق الكلمة: «${wordState.wordText}» (${toArabicIndicDigits(wordState.currentIteration)}/${toArabicIndicDigits(wordState.totalIterations)})",
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFF064E3B),
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إيقاف نطق الكلمة",
                                tint = EmeraldGreen,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { QuranAudioPlayerManager.stopWordPronunciation() }
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

                    val activeWordHighlightColor = if (isNightMushafMode) {
                        Color(0xFF10B981).copy(alpha = 0.42f)
                    } else {
                        Color(0xFF34D399).copy(alpha = 0.45f)
                    }

                    MushafPageContent(
                        pageNumber = pageNum,
                        verses = versesOnThisPage,
                        isAyahByAyahMode = isAyahByAyahMode,
                        fontScaleSp = fontScaleSp,
                        fontZoomStep = fontZoomStep,
                        pageCardColor = pageCardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        isNightMode = isNightMushafMode,
                        isFullScreen = isFullScreen,
                        onExitFullScreen = { onToggleFullScreen(false) },
                        onOpenSearch = { showSearchDialog = true },
                        onPreviousPage = {
                            if (pageNum > 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pageNum - 2)
                                }
                            }
                        },
                        onNextPage = {
                            if (pageNum < 604) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pageNum)
                                }
                            }
                        },
                        bookmarkedSurah = bookmarkedSurah,
                        bookmarkedAyah = bookmarkedAyah,
                        memorizedAyahKeys = memorizedAyahKeys,
                        focusedReadingVerseKey = focusedReadingVerseKey,
                        activeWordKey = activeWordPronunciation?.wordKey,
                        activeWordHighlightColor = activeWordHighlightColor,
                        selectedVerse = floatingPopoverVerse ?: activePlayingVerse ?: selectedVerseForSheet,
                        onVerseClicked = { clickedVerse ->
                            QuranAudioPlayerManager.stopWordPronunciation()
                            val clickedKey = "${clickedVerse.surahNumber}:${clickedVerse.ayahNumber}"
                            val nowMs = System.currentTimeMillis()
                            focusedReadingVerseKey = clickedKey
                            quranPrefs.edit()
                                .putString("focused_verse_key_$activeTrackId", clickedKey)
                                .apply()
                            persistAutoStopPoint(clickedVerse.surahNameAr, clickedVerse.ayahNumber, clickedVerse.page)

                            if (lastTappedVerseKey == clickedKey && (nowMs - lastTapTimestampMs) <= 550L) {
                                // Double tap (ضغطتان متتاليتان): Open the floating Ayah action menu
                                floatingPopoverVerse = clickedVerse
                                lastTapTimestampMs = 0L
                            } else {
                                // Single tap (ضغطة واحدة): Highlight Ayah with strong light-gray reading focus only
                                lastTappedVerseKey = clickedKey
                                lastTapTimestampMs = nowMs
                            }
                        },
                        onVerseDoubleTapped = { doubleTappedVerse ->
                            QuranAudioPlayerManager.stopWordPronunciation()
                            val clickedKey = "${doubleTappedVerse.surahNumber}:${doubleTappedVerse.ayahNumber}"
                            focusedReadingVerseKey = clickedKey
                            quranPrefs.edit()
                                .putString("focused_verse_key_$activeTrackId", clickedKey)
                                .apply()
                            persistAutoStopPoint(doubleTappedVerse.surahNameAr, doubleTappedVerse.ayahNumber, doubleTappedVerse.page)
                            floatingPopoverVerse = doubleTappedVerse
                        },
                        onWordLongPressed = { surahNum, ayahNum, wordIdx1Based, totalWordsInAyah, wordText ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            QuranAudioPlayerManager.playSingleWordTwice(
                                context = context,
                                surahNumber = surahNum,
                                ayahNumber = ayahNum,
                                wordIndex1Based = wordIdx1Based,
                                wordText = wordText,
                                totalWordsInAyah = totalWordsInAyah
                            )
                        }
                    )
                }
            }
        }
    }

    // TRUE FLOATING POPUP CARD WHEN USER DOUBLE-TAPS ANY AYAH
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)),
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
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldGreen.copy(alpha = 0.14f),
                                border = BorderStroke(0.8.dp, EmeraldGreen)
                            ) {
                                Text(
                                    text = "صفحة ${toArabicIndicDigits(tappedVerse.page)}",
                                    color = EmeraldGreen,
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
                                .background(RoyalNavyDark)
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
                        color = Color(0xFFF3E9D2),
                        border = BorderStroke(1.dp, GoldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "﴿ ${tappedVerse.textAr} ﴾",
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            lineHeight = 31.sp,
                            color = Color(0xFF1A110B),
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
                                containerColor = if (isStopBookmark) GoldAccent else RoyalNavyDark,
                                contentColor = if (isStopBookmark) Color.White else GoldAccent
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
                                containerColor = if (isMemorized) EmeraldGreen else RoyalNavyDark,
                                contentColor = if (isMemorized) Color.White else EmeraldGreen
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
                                containerColor = EmeraldGreen.copy(alpha = 0.12f),
                                contentColor = EmeraldGreen
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
                                tint = EmeraldGreen,
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
                                contentColor = Color.White
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
                                color = Color.White,
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
                        containerColor = Color(0xFFF3E9D2)
                    )
                ) {
                    Text(
                        text = "﴿ ${verse.textAr} ﴾",
                        fontFamily = AmiriFontFamily,
                        fontSize = 23.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A110B),
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

    // Offline Full-Quran Instant Word & Ayah Search Dialog
    if (showSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var exactWholeWordOnly by remember { mutableStateOf(false) }

        val searchResult: QuranSearchResult = remember(searchQuery, exactWholeWordOnly, isDatabaseReady) {
            if (isDatabaseReady && searchQuery.trim().length >= 2) {
                OfflineQuranDatabase.searchVersesInstant(
                    context = context,
                    query = searchQuery,
                    exactWholeWordOnly = exactWholeWordOnly,
                    limit = 800
                )
            } else {
                QuranSearchResult(
                    query = searchQuery.trim(),
                    totalMatchingVerses = 0,
                    totalWordOccurrences = 0,
                    matches = emptyList()
                )
            }
        }

        val quickExamples = remember {
            listOf("الصلاة", "الرحمن", "الجنة", "يمشي", "الصراط المستقيم", "الحمد لله", "شهر رمضان")
        }

        Dialog(
            onDismissRequest = { showSearchDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.88f)
                    .border(1.4.dp, GoldAccent, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "البحث الفوري في المصحف الشريف",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "ابحث عن أي كلمة أو آية ليظهر كل موضع لها فوراً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSearchDialog = false },
                            modifier = Modifier.testTag("close_quran_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق البحث",
                                tint = GoldAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Instant Search Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("اكتب كلمة أو جزءاً من آية...") },
                        placeholder = { Text("مثال: الصلاة، يمشي، الرحمن، الصراط المستقيم") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = EmeraldGreen
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح البحث",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quran_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Match Mode Toggle Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !exactWholeWordOnly,
                            onClick = { exactWholeWordOnly = false },
                            label = {
                                Text(
                                    text = "جميع المطابقات",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("search_mode_all_chip")
                        )
                        FilterChip(
                            selected = exactWholeWordOnly,
                            onClick = { exactWholeWordOnly = true },
                            label = {
                                Text(
                                    text = "كلمة مطابقة تماماً",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("search_mode_exact_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchQuery.trim().length < 2) {
                        // Quick Suggestions when empty
                        Text(
                            text = "أمثلة سريعة للبحث الفوري:",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickExamples) { sample ->
                                AssistChip(
                                    onClick = { searchQuery = sample },
                                    label = {
                                        Text(
                                            text = sample,
                                            fontFamily = AmiriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldGreen.copy(alpha = 0.08f),
                            border = BorderStroke(0.8.dp, EmeraldGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "بحث ذكي يدعم الرسم العثماني والإملائي",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "• اكتب الكلمة بإملاء لوحة المفاتيح العادي (مثل: الصلاة، الكتاب، السماوات، يمشي، في) وستظهر لك فوراً كل الآيات التي وردت فيها مع تظليل الكلمة داخل كل آية.\n• اضغط على أي نتيجة للانتقال المباشر إلى صفحتها وتحديد الآية في المصحف.",
                                    fontSize = 12.sp,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else if (searchResult.totalMatchingVerses == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = GoldAccent.copy(alpha = 0.55f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "لم يتم العثور على آيات مطابقة لعبارة «${searchQuery.trim()}»",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                if (exactWholeWordOnly) {
                                    TextButton(onClick = { exactWholeWordOnly = false }) {
                                        Text(
                                            text = "جرب البحث في «جميع المطابقات»",
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Live Match Statistics Summary Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الآيات المطابقة: ${toArabicIndicDigits(searchResult.totalMatchingVerses)} آية",
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "إجمالي التكرار: ${toArabicIndicDigits(searchResult.totalWordOccurrences)} مرة",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (searchResult.totalMatchingVerses > searchResult.matches.size) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يتم عرض أول ${toArabicIndicDigits(searchResult.matches.size)} آية مطابقة (يمكنك كتابة كلمة إضافية لتضييق النتائج)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("quran_search_results_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = searchResult.matches,
                                key = { "${it.verse.surahNumber}:${it.verse.ayahNumber}" }
                            ) { matchItem ->
                                val v = matchItem.verse
                                val highlightedAyahText = remember(v.textAr, searchQuery, exactWholeWordOnly) {
                                    val matchedIndices = OfflineQuranDatabase.findMatchingWordIndicesInVerse(
                                        rawVerseText = v.textAr,
                                        query = searchQuery,
                                        exactWholeWordOnly = exactWholeWordOnly
                                    )
                                    val tokens = v.textAr.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                                    buildAnnotatedString {
                                        tokens.forEachIndexed { idx, token ->
                                            if (idx > 0) append(" ")
                                            if (matchedIndices.contains(idx)) {
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color(0xFF065F46),
                                                        background = Color(0xFFFDE68A)
                                                    )
                                                ) {
                                                    append(token)
                                                }
                                            } else {
                                                append(token)
                                            }
                                        }
                                        withStyle(
                                            SpanStyle(
                                                color = Color(0xFF9A6F0B)
                                            )
                                        ) {
                                            append("\u202F﴿${toArabicIndicDigits(v.ayahNumber)}﴾")
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val targetKey = "${v.surahNumber}:${v.ayahNumber}"
                                            focusedReadingVerseKey = targetKey
                                            quranPrefs.edit()
                                                .putString("focused_verse_key_$activeTrackId", targetKey)
                                                .apply()
                                            showSearchDialog = false
                                            coroutineScope.launch {
                                                pagerState.scrollToPage((v.page - 1).coerceIn(0, 603))
                                                floatingPopoverVerse = v
                                            }
                                        }
                                        .border(0.9.dp, GoldBorder, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isNightMushafMode) Color(0xFF132436) else Color(0xFFFFFFFF)
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
                                                color = if (isNightMushafMode) GoldAccent else Color(0xFF0F5257),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (matchItem.occurrenceCount > 1) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = GoldAccent.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "${toArabicIndicDigits(matchItem.occurrenceCount)}×",
                                                            color = GoldAccent,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = EmeraldGreen.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        text = "صفحة ${toArabicIndicDigits(v.page)}",
                                                        color = EmeraldGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "الجزء ${toArabicIndicDigits(v.juz)} • الحزب ${toArabicIndicDigits(v.hizb)} (${rubFractionShortLabel(v.rubInHizb)})",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = highlightedAyahText,
                                            fontFamily = AmiriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.5.sp,
                                            lineHeight = 31.sp,
                                            color = if (isNightMushafMode) Color(0xFFF5EFE0) else Color(0xFF111111),
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

// Exact 15 Sajdah Tilawah (سجدة تلاوة) positions in the Holy Quran
private val SAJDAH_VERSE_KEYS = setOf(
    "7:206",   // الأعراف ٢٠٦
    "13:15",   // الرعد ١٥
    "16:50",   // النحل ٥٠
    "17:109",  // الإسراء ١٠٩
    "19:58",   // مريم ٥٨
    "22:18",   // الحج ١٨
    "22:77",   // الحج ٧٧
    "25:60",   // الفرقان ٦٠
    "27:26",   // النمل ٢٦
    "32:15",   // السجدة ١٥
    "38:24",   // ص ٢٤
    "41:38",   // فصلت ٣٨
    "53:62",   // النجم ٦٢
    "84:21",   // الانشقاق ٢١
    "96:19"    // العلق ١٩
)

private val DIVINE_NAME_NORMALIZED_FORMS = setOf(
    "الله", "اللهم", "لله", "بالله", "تالله", "وتالله", "والله", "فالله",
    "واللهم", "ولله", "فلله", "ابالله", "افالله", "ءالله", "االله"
)

// True standalone Uthmani Waqf, Saktah, Rub' Al-Hizb (۞), and Sajdah (۩) symbols.
// Excludes word-internal Uthmani orthography/diacritics such as U+06E1 (ۡ Sukun),
// U+06E2 (ۢ Iqlab Meem), U+06E4 (ۤ Maddah), U+06E5 (ۥ Small Waw), U+06E6 (ۦ Small Yeh), U+06E0 (۠ Rectangular Zero).
private fun isUthmaniWaqfChar(ch: Char): Boolean {
    return ch in '\u06D6'..'\u06DC' || ch == '\u06DE' || ch == '\u06E9'
}

private fun normalizeWordForDivineNameCheck(word: String): String {
    val sb = StringBuilder(word.length)
    for (ch in word) {
        when {
            ch == '\u0671' || ch == 'أ' || ch == 'إ' || ch == 'آ' -> sb.append('ا')
            ch == '\u0640' -> {} // Tatweel
            ch in '\u064B'..'\u065F' || ch == '\u0670' || ch in '\u06D6'..'\u06ED' -> {} // Harakat & Uthmani marks
            ch in '؀'..'ۿ' -> sb.append(ch)
        }
    }
    return sb.toString()
}

private fun countSpokenWordsInAyah(rawText: String): Int {
    var count = 0
    var index = 0
    val len = rawText.length
    while (index < len) {
        val start = index
        val isSpace = rawText[index].isWhitespace()
        while (index < len && rawText[index].isWhitespace() == isSpace) {
            index++
        }
        if (!isSpace) {
            val token = rawText.substring(start, index)
            var leadIdx = 0
            while (leadIdx < token.length && token[leadIdx] == '\u06DE') {
                leadIdx++
            }
            val remainingToken = token.substring(leadIdx)
            if (remainingToken.isNotEmpty()) {
                var splitIdx = remainingToken.length
                while (splitIdx > 0 && isUthmaniWaqfChar(remainingToken[splitIdx - 1])) {
                    splitIdx--
                }
                val coreWord = remainingToken.substring(0, splitIdx)
                val isPureWaqfToken = coreWord.isEmpty() || coreWord.all { isUthmaniWaqfChar(it) }
                if (coreWord.isNotEmpty() && !isPureWaqfToken) {
                    count++
                }
            }
        }
    }
    return count.coerceAtLeast(1)
}

private const val QURAN_OPENTYPE_FONT_FEATURES = "\"liga\" 1, \"calt\" 1, \"mark\" 1, \"mkmk\" 1, \"kern\" 1"
private val REPEATED_WHITESPACE_REGEX = Regex("\\s+")

/**
 * Collapses any repeated whitespace in the source ayah string to a single standard space (' ').
 */
private fun collapseRepeatedSpaces(rawText: String): String {
    return rawText.trim().replace(REPEATED_WHITESPACE_REGEX, " ")
}

/**
 * Builds the exact invariant plain-text character sequence for a group of verses:
 * collapsed single spaces, continuous inline Arabic text, and inline ayah number rosette ` ﴿...﴾ `.
 */
private fun buildCanonicalGroupPlainText(groupVerses: List<OfflineVerse>): String {
    return buildString {
        groupVerses.forEachIndexed { idx, v ->
            if (idx > 0) append(" ")
            val normalizedAyah = collapseRepeatedSpaces(v.textAr)
            append(normalizedAyah)
            append(" ﴿${toArabicIndicDigits(v.ayahNumber)}﴾")
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendShamarlyColoredQuranText(
    rawText: String,
    baseTextColor: Color,
    divineNameColor: Color,
    waqfColor: Color,
    bgHighlight: Color,
    fontSizeSp: Float,
    verseTagKey: String = "",
    activeWordKey: String? = null,
    activeWordHighlightColor: Color = Color.Transparent
) {
    // 1. Before rendering, collapse any repeated spaces in the source string to a single space.
    val normalizedText = collapseRepeatedSpaces(rawText)
    val totalWordsInAyah = if (verseTagKey.isNotEmpty()) countSpokenWordsInAyah(normalizedText) else 1
    var spokenWordCounter = 0

    // 2. Keep each ayah as one continuous inline Arabic text stream with standard single spaces (' ').
    // Never insert non-breaking spaces (\u00A0, \u202F), never wrap words in separate layout elements,
    // and never split a word from its trailing Uthmani marks.
    val tokens = normalizedText.split(' ')
    tokens.forEachIndexed { tokenIdx, token ->
        if (tokenIdx > 0) {
            withStyle(
                SpanStyle(
                    color = baseTextColor,
                    background = bgHighlight
                )
            ) {
                append(" ")
            }
        }
        if (token.isNotEmpty()) {
            var leadIdx = 0
            while (leadIdx < token.length && token[leadIdx] == '\u06DE') {
                leadIdx++
            }
            val remainingToken = token.substring(leadIdx)
            var splitIdx = remainingToken.length
            while (splitIdx > 0 && isUthmaniWaqfChar(remainingToken[splitIdx - 1])) {
                splitIdx--
            }
            val coreWord = remainingToken.substring(0, splitIdx)
            val normalizedCore = normalizeWordForDivineNameCheck(coreWord)
            val isDivineName = normalizedCore in DIVINE_NAME_NORMALIZED_FORMS
            val isPureWaqfToken = coreWord.isEmpty() || coreWord.all { isUthmaniWaqfChar(it) }

            val isSpokenWord = coreWord.isNotEmpty() && !isPureWaqfToken && verseTagKey.isNotEmpty()
            if (isSpokenWord) {
                spokenWordCounter++
            }
            val currentWordKey = if (isSpokenWord) "$verseTagKey:$spokenWordCounter" else ""
            val isWordPronouncing = currentWordKey.isNotEmpty() && currentWordKey == activeWordKey
            val effectiveWordBg = if (isWordPronouncing) activeWordHighlightColor else bgHighlight

            val tokenColor = when {
                isWordPronouncing -> Color(0xFF064E3B)
                isDivineName -> divineNameColor
                isPureWaqfToken -> waqfColor
                else -> baseTextColor
            }

            if (isSpokenWord) {
                pushStringAnnotation(
                    tag = "WORD_TAP",
                    annotation = "$currentWordKey|$totalWordsInAyah|$coreWord"
                )
            }
            // Append the entire token (including any leading/trailing Quranic marks) as a single
            // continuous inline Arabic sequence so HarfBuzz shapes the word and its marks naturally.
            withStyle(
                SpanStyle(
                    color = tokenColor,
                    background = effectiveWordBg
                )
            ) {
                append(token)
            }
            if (isSpokenWord) {
                pop()
            }
        }
    }
}

@Composable
private fun MushafPageContent(
    pageNumber: Int,
    verses: List<OfflineVerse>,
    isAyahByAyahMode: Boolean,
    fontScaleSp: Float,
    fontZoomStep: Int = 0,
    pageCardColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    isNightMode: Boolean,
    isFullScreen: Boolean = false,
    onExitFullScreen: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    bookmarkedSurah: String,
    bookmarkedAyah: Int,
    memorizedAyahKeys: Set<String>,
    focusedReadingVerseKey: String? = null,
    activeWordKey: String? = null,
    activeWordHighlightColor: Color = Color.Transparent,
    selectedVerse: OfflineVerse?,
    onVerseClicked: (OfflineVerse) -> Unit,
    onVerseDoubleTapped: (OfflineVerse) -> Unit = onVerseClicked,
    onWordLongPressed: (surahNumber: Int, ayahNumber: Int, wordIndex1Based: Int, totalWordsInAyah: Int, wordText: String) -> Unit = { _, _, _, _, _ -> }
) {
    val firstVerse = verses.firstOrNull()
    val lastVerse = verses.lastOrNull()
    val surahName = firstVerse?.surahNameAr ?: ""
    val juzNumber = lastVerse?.juz ?: firstVerse?.juz ?: (((pageNumber - 1) / 20) + 1).coerceIn(1, 30)
    val hizbNumber = lastVerse?.hizb ?: firstVerse?.hizb ?: 1
    val currentRubInHizb = lastVerse?.rubInHizb ?: firstVerse?.rubInHizb ?: 1
    val rubStartVersesOnPage = remember(verses) {
        verses.filter { it.startsNewRub && !(it.surahNumber == 1 && it.ayahNumber == 1) }
    }
    val hasSajdahOnPage = remember(verses) {
        verses.any { it.isSajda || SAJDAH_VERSE_KEYS.contains("${it.surahNumber}:${it.ayahNumber}") }
    }
    val arabicPageNumber = remember(pageNumber) { toArabicIndicDigits(pageNumber) }

    val divineNameColor = if (isNightMode) Color(0xFFFF5A6E) else Color(0xFFC41E3A)
    val waqfMarkColor = if (isNightMode) Color(0xFF4FD1C5) else Color(0xFF0F6B58)
    val currentRubAccentColor = rubFractionAccentColor(currentRubInHizb, isNightMode)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isFullScreen) 4.dp else 6.dp, vertical = 2.dp)
            .border(
                width = 2.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(GoldAccent, GoldBorder, GoldAccent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(3.dp)
            .border(1.dp, GoldBorder, RoundedCornerShape(13.dp))
            .clip(RoundedCornerShape(13.dp))
            .background(pageCardColor),
        contentAlignment = Alignment.TopCenter
    ) {
        // Fixed reading column: max-width 46rem (736.dp), centered with margin-inline auto and 16px (16.dp) side padding
        Column(
            modifier = Modifier
                .widthIn(max = 736.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Ornamental Bright Mushaf Top Header Bar
            // Right: Surah Name | Center: Juz, Hizb & Current Rub' Badge | Left: Sajdah + Search (Fullscreen) + Page/Exit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.9.dp, GoldBorder, RoundedCornerShape(9.dp))
                    .background(
                        if (isNightMode) Color(0xFF162A3F).copy(alpha = 0.82f)
                        else Color(0xFFFAFBFD),
                        RoundedCornerShape(9.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right in RTL: Surah Name
                Text(
                    text = "سُورَةُ $surahName",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (isNightMode) GoldAccent else Color(0xFF0F5257),
                    maxLines = 1
                )

                // Center: Canonical Juz, Hizb & Clear Rub' Indicator Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "الجزء ${toArabicIndicDigits(juzNumber)} • الحزب ${toArabicIndicDigits(hizbNumber)}",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = if (isNightMode) Color(0xFFE6F1FF) else Color(0xFF1F2937),
                        maxLines = 1
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = currentRubAccentColor.copy(alpha = if (isNightMode) 0.22f else 0.13f),
                        border = BorderStroke(0.8.dp, currentRubAccentColor.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = rubFractionShortLabel(currentRubInHizb),
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp,
                            color = currentRubAccentColor,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                // Left in RTL: Sajdah Badge (if on page) + Quick Search (FullScreen) + FullScreen Exit / Page Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasSajdahOnPage) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = divineNameColor.copy(alpha = 0.14f),
                            border = BorderStroke(0.9.dp, divineNameColor)
                        ) {
                            Text(
                                text = "۩ سجدة",
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = divineNameColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (isFullScreen) {
                        Surface(
                            shape = CircleShape,
                            color = if (isNightMode) RoyalNavyDark else Color(0xFFFFFFFF),
                            border = BorderStroke(0.8.dp, GoldAccent),
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable { onOpenSearch() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "بحث في المصحف",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = RoyalNavyDark,
                            border = BorderStroke(0.8.dp, GoldAccent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onExitFullScreen() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isNightMode) RoyalNavyDark else Color(0xFFFFFFFF),
                            border = BorderStroke(0.9.dp, GoldAccent)
                        ) {
                            Text(
                                text = "ص $arabicPageNumber",
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                color = if (isNightMode) GoldAccent else Color(0xFF111111),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // 1B. Enlarged Ornamental Rub' Al-Hizb / Half-Hizb Milestone Banner (whenever a Hizb / Rub' / Half-Hizb begins on this page)
            if (rubStartVersesOnPage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                RubAlHizbMilestoneBanner(
                    rubStartVerses = rubStartVersesOnPage,
                    isNightMode = isNightMode
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 2. Main Verses Viewport:
            // - Height equal to visible screen minus header and bottom bar (weight(1f))
            // - overflow: hidden (clipToBounds()), never scrolls and has no scrollbar
            // - ResizeObserver equivalent via onSizeChanged so it re-fits when toolbars show/hide or window resizes
            // - Dynamic 0.5-step font-size fitting in [12, 34] with line-height = 2.05 * font-size and 2.dp vertical padding
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

                val groupPlainTexts = remember(surahGroups) {
                    surahGroups.map { (firstV, groupVerses) ->
                        Triple(firstV, buildCanonicalGroupPlainText(groupVerses), groupVerses)
                    }
                }

                val density = LocalDensity.current
                val textMeasurer = rememberTextMeasurer()
                var wrapperWidthPx by remember { mutableIntStateOf(0) }
                var wrapperHeightPx by remember { mutableIntStateOf(0) }
                var measuredSurahBannerHeightPx by remember { mutableIntStateOf(0) }
                var postLayoutOverflowStepDownSp by remember(pageNumber, verses, wrapperWidthPx, wrapperHeightPx) {
                    mutableFloatStateOf(0f)
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds() // overflow: hidden — the page itself never scrolls
                        .onSizeChanged { size ->
                            if (size.width != wrapperWidthPx || size.height != wrapperHeightPx) {
                                wrapperWidthPx = size.width
                                wrapperHeightPx = size.height
                            }
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    val availableWidthPx = if (wrapperWidthPx > 0) wrapperWidthPx else constraints.maxWidth
                    val availableHeightPx = if (wrapperHeightPx > 0) wrapperHeightPx else constraints.maxHeight
                    val blockVerticalPadPx = with(density) { 4.dp.toPx().toInt() } // 2.dp top + 2.dp bottom
                    val defaultSurahBannerPx = with(density) { 46.dp.toPx().toInt() }
                    val effectiveSurahBannerPx = if (measuredSurahBannerHeightPx > 0) {
                        measuredSurahBannerHeightPx + blockVerticalPadPx
                    } else {
                        defaultSurahBannerPx
                    }

                    // Measure total page content height at a candidate font size (with line-height = 2.05)
                    fun measureTotalPageHeightPx(candidateFontSp: Float): Int {
                        if (availableWidthPx <= 0) return 0
                        val candidateStyle = TextStyle(
                            fontFamily = AmiriQuranFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = candidateFontSp.sp,
                            lineHeight = (candidateFontSp * 2.05f).sp,
                            fontFeatureSettings = QURAN_OPENTYPE_FONT_FEATURES,
                            platformStyle = PlatformTextStyle(includeFontPadding = true),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None
                            ),
                            textAlign = TextAlign.Center,
                            textDirection = TextDirection.Rtl
                        )
                        var totalHeight = blockVerticalPadPx
                        for ((firstV, plainText, _) in groupPlainTexts) {
                            if (firstV.ayahNumber == 1) {
                                totalHeight += effectiveSurahBannerPx
                                if (firstV.surahNumber != 1 && firstV.surahNumber != 9) {
                                    val basmalahLayout = textMeasurer.measure(
                                        text = AnnotatedString("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"),
                                        style = candidateStyle,
                                        constraints = Constraints(maxWidth = availableWidthPx)
                                    )
                                    totalHeight += basmalahLayout.size.height + blockVerticalPadPx
                                }
                            }
                            val groupLayout = textMeasurer.measure(
                                text = AnnotatedString(plainText),
                                style = candidateStyle,
                                constraints = Constraints(maxWidth = availableWidthPx)
                            )
                            totalHeight += groupLayout.size.height + blockVerticalPadPx
                        }
                        return totalHeight
                    }

                    // Auto-fit font-size in 0.5 steps between 12 and 34 whenever pageNumber, verses, or wrapper dimensions change
                    val fittedFontSp = remember(
                        pageNumber,
                        groupPlainTexts,
                        availableWidthPx,
                        availableHeightPx,
                        effectiveSurahBannerPx
                    ) {
                        if (availableWidthPx <= 0 || availableHeightPx <= 0) {
                            (maxWidth.value * 0.052f).coerceIn(22f, 34f)
                        } else {
                            val minFontSp = 12.0f
                            val maxFontSp = 34.0f
                            val stepSp = 0.5f
                            // Start from clamp(22px, 5.2vw, 34px) quantized to 0.5 steps
                            var candidateSp = (((maxWidth.value * 0.052f).coerceIn(22f, 34f) * 2f).toInt() / 2f)
                                .coerceIn(minFontSp, maxFontSp)
                            var measuredHeight = measureTotalPageHeightPx(candidateSp)

                            if (measuredHeight > availableHeightPx) {
                                // Taller than wrapper: decrease font-size in 0.5 steps until it fits (min 12)
                                while (measuredHeight > availableHeightPx && candidateSp > minFontSp) {
                                    candidateSp = (candidateSp - stepSp).coerceAtLeast(minFontSp)
                                    measuredHeight = measureTotalPageHeightPx(candidateSp)
                                }
                            } else if (measuredHeight < availableHeightPx) {
                                // Shorter than wrapper: increase font-size in 0.5 steps up to 34 while it still fits
                                while (candidateSp < maxFontSp) {
                                    val nextSp = (candidateSp + stepSp).coerceAtMost(maxFontSp)
                                    val nextHeight = measureTotalPageHeightPx(nextSp)
                                    if (nextHeight <= availableHeightPx) {
                                        candidateSp = nextSp
                                        measuredHeight = nextHeight
                                    } else {
                                        break
                                    }
                                }
                            }
                            candidateSp
                        }
                    }

                    val displayFontSp = (fittedFontSp - postLayoutOverflowStepDownSp).coerceIn(12.0f, 34.0f)
                    // Keep line-height at 2.05 of the font-size
                    val effectiveLineHeightSp = displayFontSp * 2.05f

                    val renderedGroupHeights = remember(pageNumber, groupPlainTexts.size) {
                        mutableStateMapOf<Int, Int>()
                    }
                    val renderedBasmalahHeights = remember(pageNumber, groupPlainTexts.size) {
                        mutableStateMapOf<Int, Int>()
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 736.dp)
                            .fillMaxSize()
                            .clipToBounds() // Never allow vertical scroll or scrollbar
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        surahGroups.forEachIndexed { groupIdx, (firstV, groupVerses) ->
                            // Show Ornamental Surah Header if Ayah 1 is on this page
                            if (firstV.ayahNumber == 1) {
                                SurahOrnamentalBanner(
                                    surahNameAr = firstV.surahNameAr,
                                    revelationPlaceAr = firstV.revelationPlaceAr,
                                    isNightMode = isNightMode,
                                    onHeightMeasured = { h ->
                                        if (h > 0 && h != measuredSurahBannerHeightPx) {
                                            measuredSurahBannerHeightPx = h
                                        }
                                    }
                                )
                                if (firstV.surahNumber != 1 && firstV.surahNumber != 9) {
                                    val basmalahAnnotated = remember(isNightMode, displayFontSp) {
                                        buildAnnotatedString {
                                            appendShamarlyColoredQuranText(
                                                rawText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                                baseTextColor = if (isNightMode) GoldAccent else Color(0xFF0F5257),
                                                divineNameColor = divineNameColor,
                                                waqfColor = waqfMarkColor,
                                                bgHighlight = Color.Transparent,
                                                fontSizeSp = displayFontSp
                                            )
                                        }
                                    }
                                    Text(
                                        text = basmalahAnnotated,
                                        onTextLayout = { result ->
                                            renderedBasmalahHeights[groupIdx] = result.size.height + blockVerticalPadPx
                                        },
                                        style = TextStyle(
                                            fontFamily = AmiriQuranFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = displayFontSp.sp,
                                            lineHeight = effectiveLineHeightSp.sp,
                                            fontFeatureSettings = QURAN_OPENTYPE_FONT_FEATURES,
                                            platformStyle = PlatformTextStyle(includeFontPadding = true),
                                            lineHeightStyle = LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            ),
                                            textAlign = TextAlign.Center,
                                            textDirection = TextDirection.Rtl
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }

                            val annotatedPageText = remember(
                                groupVerses,
                                textColor,
                                divineNameColor,
                                waqfMarkColor,
                                isNightMode,
                                displayFontSp,
                                bookmarkedSurah,
                                bookmarkedAyah,
                                memorizedAyahKeys,
                                focusedReadingVerseKey,
                                selectedVerse,
                                activeWordKey,
                                activeWordHighlightColor
                            ) {
                                buildAnnotatedString {
                                    groupVerses.forEachIndexed { verseIdx, v ->
                                        if (verseIdx > 0) {
                                            withStyle(
                                                style = SpanStyle(
                                                    color = textColor,
                                                    background = Color.Transparent
                                                )
                                            ) {
                                                append(" ")
                                            }
                                        }

                                        val tagKey = "${v.surahNumber}:${v.ayahNumber}"
                                        val isBookmarked = (bookmarkedSurah == v.surahNameAr && bookmarkedAyah == v.ayahNumber)
                                        val isMemorized = memorizedAyahKeys.contains(tagKey)
                                        val isFocusedReading = (focusedReadingVerseKey == tagKey)
                                        val isSelected = (selectedVerse?.surahNumber == v.surahNumber && selectedVerse.ayahNumber == v.ayahNumber)

                                        val bgHighlight = when {
                                            isFocusedReading -> if (isNightMode) Color(0xFF334155) else Color(0xFFCFD6DF)
                                            isSelected -> GoldAccent.copy(alpha = 0.35f)
                                            isBookmarked -> EmeraldGreen.copy(alpha = 0.22f)
                                            isMemorized -> GoldAccent.copy(alpha = 0.16f)
                                            else -> Color.Transparent
                                        }

                                        pushStringAnnotation(tag = "AYAH_TAP", annotation = tagKey)

                                        appendShamarlyColoredQuranText(
                                            rawText = v.textAr,
                                            baseTextColor = textColor,
                                            divineNameColor = divineNameColor,
                                            waqfColor = waqfMarkColor,
                                            bgHighlight = bgHighlight,
                                            fontSizeSp = displayFontSp,
                                            verseTagKey = tagKey,
                                            activeWordKey = activeWordKey,
                                            activeWordHighlightColor = activeWordHighlightColor
                                        )

                                        val ayahMarkerColor = when {
                                            isBookmarked -> EmeraldGreen
                                            isMemorized -> GoldAccent
                                            isNightMode -> GoldAccent
                                            else -> Color(0xFF9A6F0B)
                                        }

                                        // Keep the ayah number rosette inline at the end of each ayah
                                        // with a standard single space (~0.15em inline margin) on each side.
                                        withStyle(
                                            style = SpanStyle(
                                                color = ayahMarkerColor,
                                                background = bgHighlight
                                            )
                                        ) {
                                            append(" ")
                                        }

                                        if (isBookmarked) {
                                            pushStringAnnotation(tag = "BOOKMARKED_AYAH_MARKER", annotation = tagKey)
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                color = ayahMarkerColor,
                                                background = bgHighlight
                                            )
                                        ) {
                                            append("﴿${toArabicIndicDigits(v.ayahNumber)}﴾")
                                        }
                                        if (isBookmarked) {
                                            pop()
                                        }
                                        pop()
                                    }
                                }
                            }

                            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                            val currentAnnotatedText by rememberUpdatedState(annotatedPageText)
                            val currentOnVerseClicked by rememberUpdatedState(onVerseClicked)
                            val currentOnVerseDoubleTapped by rememberUpdatedState(onVerseDoubleTapped)
                            val currentOnWordLongPressed by rememberUpdatedState(onWordLongPressed)

                            Text(
                                text = annotatedPageText,
                                onTextLayout = { result ->
                                    textLayoutResult = result
                                    renderedGroupHeights[groupIdx] = result.size.height + blockVerticalPadPx
                                    // Post-render verification: if actual rendered content exceeds wrapper height,
                                    // step down by 0.5px/sp (down to 12) so it never clips or overflows.
                                    var actualTotal = blockVerticalPadPx
                                    for (i in surahGroups.indices) {
                                        val gFirst = surahGroups[i].first
                                        if (gFirst.ayahNumber == 1) {
                                            actualTotal += effectiveSurahBannerPx
                                            if (gFirst.surahNumber != 1 && gFirst.surahNumber != 9) {
                                                actualTotal += (renderedBasmalahHeights[i] ?: 0)
                                            }
                                        }
                                        actualTotal += (renderedGroupHeights[i] ?: 0)
                                    }
                                    if ((result.didOverflowHeight || actualTotal > availableHeightPx) &&
                                        availableHeightPx > 0 &&
                                        displayFontSp > 12.0f
                                    ) {
                                        postLayoutOverflowStepDownSp = (postLayoutOverflowStepDownSp + 0.5f)
                                            .coerceAtMost(fittedFontSp - 12.0f)
                                    }
                                },
                                style = TextStyle(
                                    fontFamily = AmiriQuranFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = displayFontSp.sp,
                                    lineHeight = effectiveLineHeightSp.sp,
                                    fontFeatureSettings = QURAN_OPENTYPE_FONT_FEATURES,
                                    platformStyle = PlatformTextStyle(includeFontPadding = true),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.None
                                    ),
                                    textAlign = TextAlign.Center,
                                    textDirection = TextDirection.Rtl
                                ),
                                modifier = Modifier
                                    .widthIn(max = 736.dp)
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .drawWithContent {
                                        drawContent()
                                        val layout = textLayoutResult ?: return@drawWithContent
                                        val ribbonAnns = annotatedPageText.getStringAnnotations(
                                            tag = "BOOKMARKED_AYAH_MARKER",
                                            start = 0,
                                            end = annotatedPageText.length
                                        )
                                        for (ann in ribbonAnns) {
                                            if (ann.start < ann.end && ann.end <= annotatedPageText.length) {
                                                val boxOpen = layout.getBoundingBox(ann.start)
                                                val boxClose = layout.getBoundingBox((ann.end - 1).coerceAtLeast(ann.start))
                                                val markerLeft = minOf(boxOpen.left, boxClose.left)
                                                val closeW = (boxClose.right - boxClose.left).coerceAtLeast(6f)
                                                val lineIdx = layout.getLineForOffset(ann.start)
                                                val lineTop = layout.getLineTop(lineIdx)
                                                val lineBottom = layout.getLineBottom(lineIdx)
                                                val lineH = (lineBottom - lineTop).coerceAtLeast(16f)
                                                val ribbonW = (lineH * 0.24f).coerceAtLeast(8f)
                                                val ribbonH = (lineH * 0.48f).coerceAtLeast(14f)
                                                val cx = (markerLeft + closeW * 0.14f).coerceIn(
                                                    ribbonW * 0.5f,
                                                    (size.width - ribbonW * 0.5f).coerceAtLeast(ribbonW * 0.5f)
                                                )
                                                val topY = lineTop + lineH * 0.18f
                                                drawWaqfGreenRibbonOnCanvas(
                                                    left = cx - ribbonW * 0.5f,
                                                    top = topY,
                                                    width = ribbonW,
                                                    height = ribbonH
                                                )
                                            }
                                        }
                                    }
                                    .pointerInput(groupVerses) {
                                        detectTapGestures(
                                            onTap = { tapOffset ->
                                                val layout = textLayoutResult ?: return@detectTapGestures
                                                val activeText = currentAnnotatedText
                                                val charOffset = layout.getOffsetForPosition(tapOffset)
                                                activeText.getStringAnnotations(
                                                    tag = "AYAH_TAP",
                                                    start = charOffset,
                                                    end = charOffset
                                                ).firstOrNull()?.let { annotation ->
                                                    val parts = annotation.item.split(":")
                                                    val sNum = parts.getOrNull(0)?.toIntOrNull()
                                                    val aNum = parts.getOrNull(1)?.toIntOrNull()
                                                    val matched = groupVerses.find { it.surahNumber == sNum && it.ayahNumber == aNum }
                                                    if (matched != null) {
                                                        currentOnVerseClicked(matched)
                                                    }
                                                }
                                            },
                                            onDoubleTap = { tapOffset ->
                                                val layout = textLayoutResult ?: return@detectTapGestures
                                                val activeText = currentAnnotatedText
                                                val charOffset = layout.getOffsetForPosition(tapOffset)
                                                activeText.getStringAnnotations(
                                                    tag = "AYAH_TAP",
                                                    start = charOffset,
                                                    end = charOffset
                                                ).firstOrNull()?.let { annotation ->
                                                    val parts = annotation.item.split(":")
                                                    val sNum = parts.getOrNull(0)?.toIntOrNull()
                                                    val aNum = parts.getOrNull(1)?.toIntOrNull()
                                                    val matched = groupVerses.find { it.surahNumber == sNum && it.ayahNumber == aNum }
                                                    if (matched != null) {
                                                        currentOnVerseDoubleTapped(matched)
                                                    }
                                                }
                                            },
                                            onLongPress = { longPressOffset ->
                                                val layout = textLayoutResult ?: return@detectTapGestures
                                                val activeText = currentAnnotatedText
                                                val charOffset = layout.getOffsetForPosition(longPressOffset)
                                                val minSearch = (charOffset - 2).coerceAtLeast(0)
                                                val maxSearch = (charOffset + 2).coerceAtMost(activeText.length)
                                                val wordAnnotation = activeText.getStringAnnotations(
                                                    tag = "WORD_TAP",
                                                    start = charOffset,
                                                    end = charOffset
                                                ).firstOrNull() ?: activeText.getStringAnnotations(
                                                    tag = "WORD_TAP",
                                                    start = minSearch,
                                                    end = maxSearch
                                                ).firstOrNull()

                                                if (wordAnnotation != null) {
                                                    val segments = wordAnnotation.item.split("|", limit = 3)
                                                    val keyParts = segments.getOrNull(0)?.split(":") ?: emptyList()
                                                    val sNum = keyParts.getOrNull(0)?.toIntOrNull()
                                                    val aNum = keyParts.getOrNull(1)?.toIntOrNull()
                                                    val wIdx = keyParts.getOrNull(2)?.toIntOrNull()
                                                    val totalW = segments.getOrNull(1)?.toIntOrNull() ?: 1
                                                    val wText = segments.getOrNull(2).orEmpty()
                                                    if (sNum != null && aNum != null && wIdx != null && wText.isNotBlank()) {
                                                        currentOnWordLongPressed(sNum, aNum, wIdx, totalW, wText)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            } else {
                // Ayah-by-Ayah Interactive Cards Mode on current page
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 736.dp)
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = verses,
                        key = { "${it.surahNumber}:${it.ayahNumber}" }
                    ) { v ->
                        val tagKey = "${v.surahNumber}:${v.ayahNumber}"
                        val isBookmarked = (bookmarkedSurah == v.surahNameAr && bookmarkedAyah == v.ayahNumber)
                        val isMemorized = memorizedAyahKeys.contains(tagKey)
                        val isFocusedReading = (focusedReadingVerseKey == tagKey)
                        val isSajdahVerse = v.isSajda || SAJDAH_VERSE_KEYS.contains(tagKey)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onVerseClicked(v) }
                                .border(
                                    width = 1.2.dp,
                                    color = when {
                                        isFocusedReading -> if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        isBookmarked -> EmeraldGreen
                                        isMemorized -> GoldAccent
                                        else -> GoldBorder
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isFocusedReading -> if (isNightMode) Color(0xFF334155) else Color(0xFFE5EAF0)
                                    isNightMode -> Color(0xFF162A3F).copy(alpha = 0.55f)
                                    else -> Color(0xFFFFFFFF)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سورة ${v.surahNameAr} • الآية ${toArabicIndicDigits(v.ayahNumber)}",
                                        color = if (isNightMode) GoldAccent else Color(0xFF0F5257),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (v.startsNewRub && !(v.surahNumber == 1 && v.ayahNumber == 1)) {
                                            val rubAccent = rubFractionAccentColor(v.rubInHizb, isNightMode)
                                            Surface(
                                                color = rubAccent.copy(alpha = 0.14f),
                                                border = BorderStroke(0.9.dp, rubAccent),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    RubAlHizbStarIcon(
                                                        isNightMode = isNightMode,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = rubFractionLabel(v.rubInHizb, v.hizb),
                                                        color = rubAccent,
                                                        fontFamily = AmiriFontFamily,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                        if (isSajdahVerse) {
                                            Surface(
                                                color = divineNameColor.copy(alpha = 0.15f),
                                                border = BorderStroke(0.8.dp, divineNameColor),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "۩ سجدة تلاوة",
                                                    color = divineNameColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (isBookmarked) {
                                            WaqfGreenRibbonBadge(
                                                modifier = Modifier
                                                    .width(14.dp)
                                                    .height(28.dp)
                                            )
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

                                val clampedCardFontSp = fontScaleSp.coerceIn(22f, 34f)
                                val coloredAyahText = remember(
                                    v.textAr,
                                    isNightMode,
                                    clampedCardFontSp,
                                    isBookmarked,
                                    activeWordKey,
                                    activeWordHighlightColor
                                ) {
                                    buildAnnotatedString {
                                        appendShamarlyColoredQuranText(
                                            rawText = v.textAr,
                                            baseTextColor = textColor,
                                            divineNameColor = divineNameColor,
                                            waqfColor = waqfMarkColor,
                                            bgHighlight = Color.Transparent,
                                            fontSizeSp = clampedCardFontSp,
                                            verseTagKey = tagKey,
                                            activeWordKey = activeWordKey,
                                            activeWordHighlightColor = activeWordHighlightColor
                                        )
                                        val markerColor = if (isBookmarked) EmeraldGreen else if (isNightMode) GoldAccent else Color(0xFF9A6F0B)
                                        withStyle(SpanStyle(color = markerColor)) {
                                            append(" ")
                                        }
                                        if (isBookmarked) {
                                            pushStringAnnotation(tag = "BOOKMARKED_AYAH_MARKER", annotation = tagKey)
                                        }
                                        withStyle(SpanStyle(color = markerColor)) {
                                            append("﴿${toArabicIndicDigits(v.ayahNumber)}﴾")
                                        }
                                        if (isBookmarked) {
                                            pop()
                                        }
                                    }
                                }

                                var ayahTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                                val currentColoredAyahText by rememberUpdatedState(coloredAyahText)
                                val currentOnVerseClickedCard by rememberUpdatedState(onVerseClicked)
                                val currentOnVerseDoubleTappedCard by rememberUpdatedState(onVerseDoubleTapped)
                                val currentOnWordLongPressedCard by rememberUpdatedState(onWordLongPressed)

                                Text(
                                    text = coloredAyahText,
                                    onTextLayout = { ayahTextLayout = it },
                                    style = TextStyle(
                                        fontFamily = AmiriQuranFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = clampedCardFontSp.sp,
                                        lineHeight = (clampedCardFontSp * 2.05f).sp,
                                        fontFeatureSettings = QURAN_OPENTYPE_FONT_FEATURES,
                                        platformStyle = PlatformTextStyle(includeFontPadding = true),
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Center,
                                            trim = LineHeightStyle.Trim.None
                                        ),
                                        textAlign = TextAlign.Center,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .drawWithContent {
                                            drawContent()
                                            val layout = ayahTextLayout ?: return@drawWithContent
                                            val rubStarAnns = coloredAyahText.getStringAnnotations(
                                                tag = "RUB_STAR_ANCHOR",
                                                start = 0,
                                                end = coloredAyahText.length
                                            )
                                            for (ann in rubStarAnns) {
                                                if (ann.start < ann.end && ann.end <= coloredAyahText.length) {
                                                    val box0 = layout.getBoundingBox(ann.start)
                                                    val box1 = layout.getBoundingBox((ann.end - 1).coerceAtLeast(ann.start))
                                                    val minLeft = minOf(box0.left, box1.left)
                                                    val maxRight = maxOf(box0.right, box1.right)
                                                    val lineIdx = layout.getLineForOffset(ann.start)
                                                    val lineTop = layout.getLineTop(lineIdx)
                                                    val lineBottom = layout.getLineBottom(lineIdx)
                                                    val lineH = (lineBottom - lineTop).coerceAtLeast(18f)
                                                    val starSize = maxOf(maxRight - minLeft, lineH * 0.85f)
                                                    val cx = (minLeft + maxRight) * 0.5f
                                                    val cy = (lineTop + lineBottom) * 0.5f
                                                    drawRubAlHizbStarOnCanvas(
                                                        cx = cx,
                                                        cy = cy,
                                                        radius = starSize * 0.46f,
                                                        isNightMode = isNightMode
                                                    )
                                                }
                                            }
                                            val ribbonAnns = coloredAyahText.getStringAnnotations(
                                                tag = "BOOKMARKED_AYAH_MARKER",
                                                start = 0,
                                                end = coloredAyahText.length
                                            )
                                            for (ann in ribbonAnns) {
                                                if (ann.start < ann.end && ann.end <= coloredAyahText.length) {
                                                    val boxOpen = layout.getBoundingBox(ann.start)
                                                    val boxClose = layout.getBoundingBox((ann.end - 1).coerceAtLeast(ann.start))
                                                    val markerLeft = minOf(boxOpen.left, boxClose.left)
                                                    val closeW = (boxClose.right - boxClose.left).coerceAtLeast(6f)
                                                    val lineIdx = layout.getLineForOffset(ann.start)
                                                    val lineTop = layout.getLineTop(lineIdx)
                                                    val lineBottom = layout.getLineBottom(lineIdx)
                                                    val lineH = (lineBottom - lineTop).coerceAtLeast(16f)
                                                    val ribbonW = (lineH * 0.28f).coerceAtLeast(8f)
                                                    val ribbonH = (lineH * 0.58f).coerceAtLeast(14f)
                                                    val cx = (markerLeft + closeW * 0.14f).coerceIn(
                                                        ribbonW * 0.5f,
                                                        (size.width - ribbonW * 0.5f).coerceAtLeast(ribbonW * 0.5f)
                                                    )
                                                    val topY = lineTop + lineH * 0.04f
                                                    drawWaqfGreenRibbonOnCanvas(
                                                        left = cx - ribbonW * 0.5f,
                                                        top = topY,
                                                        width = ribbonW,
                                                        height = ribbonH
                                                    )
                                                }
                                            }
                                        }
                                        .pointerInput(v) {
                                            detectTapGestures(
                                                onTap = { currentOnVerseClickedCard(v) },
                                                onDoubleTap = { currentOnVerseDoubleTappedCard(v) },
                                                onLongPress = { longPressOffset ->
                                                    val layout = ayahTextLayout ?: return@detectTapGestures
                                                    val activeText = currentColoredAyahText
                                                    val charOffset = layout.getOffsetForPosition(longPressOffset)
                                                    val minSearch = (charOffset - 2).coerceAtLeast(0)
                                                    val maxSearch = (charOffset + 2).coerceAtMost(activeText.length)
                                                    val wordAnnotation = activeText.getStringAnnotations(
                                                        tag = "WORD_TAP",
                                                        start = charOffset,
                                                        end = charOffset
                                                    ).firstOrNull() ?: activeText.getStringAnnotations(
                                                        tag = "WORD_TAP",
                                                        start = minSearch,
                                                        end = maxSearch
                                                    ).firstOrNull()

                                                    if (wordAnnotation != null) {
                                                        val segments = wordAnnotation.item.split("|", limit = 3)
                                                        val keyParts = segments.getOrNull(0)?.split(":") ?: emptyList()
                                                        val sNum = keyParts.getOrNull(0)?.toIntOrNull()
                                                        val aNum = keyParts.getOrNull(1)?.toIntOrNull()
                                                        val wIdx = keyParts.getOrNull(2)?.toIntOrNull()
                                                        val totalW = segments.getOrNull(1)?.toIntOrNull() ?: 1
                                                        val wText = segments.getOrNull(2).orEmpty()
                                                        if (sNum != null && aNum != null && wIdx != null && wText.isNotBlank()) {
                                                            currentOnWordLongPressedCard(sNum, aNum, wIdx, totalW, wText)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                )
                                if (v.tafsirMuyassar.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isNightMode) Color(0xFF0E1D2F).copy(alpha = 0.85f)
                                        else Color(0xFFF6F8FA),
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
                                                color = if (isNightMode) GoldAccent else Color(0xFF0F5257)
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

            Spacer(modifier = Modifier.height(2.dp))

            // 3. Pinned Ornamental Mushaf Bottom Page Number Medallion (﴿ ٣ ﴾) + Quick Page-Flip Arrows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right in RTL: Previous Page Button
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isNightMode) Color(0xFF162A3F) else Color(0xFFFAFBFD),
                    border = BorderStroke(0.8.dp, GoldBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = pageNumber > 1) { onPreviousPage() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NavigateNext,
                            contentDescription = "الصفحة السابقة",
                            tint = if (pageNumber > 1) GoldAccent else GoldAccent.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "السابقة",
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isNightMode) GoldAccent else Color(0xFF111111)
                        )
                    }
                }

                // Center: Ornamental Page Number Medallion ﴿ ٣ ﴾
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isNightMode) Color(0xFF162A3F) else Color(0xFFFAFBFD),
                    border = BorderStroke(1.2.dp, GoldAccent)
                ) {
                    Text(
                        text = "━━ ۞ ﴿ $arabicPageNumber ﴾ ۞ ━━",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isNightMode) GoldAccent else Color(0xFF111111),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    )
                }

                // Left in RTL: Next Page Button
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isNightMode) Color(0xFF162A3F) else Color(0xFFFAFBFD),
                    border = BorderStroke(0.8.dp, GoldBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = pageNumber < 604) { onNextPage() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "التالية",
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isNightMode) GoldAccent else Color(0xFF111111)
                        )
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = "الصفحة التالية",
                            tint = if (pageNumber < 604) GoldAccent else GoldAccent.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun rubFractionShortLabel(rubInHizb: Int): String = when (rubInHizb) {
    1 -> "بداية الحزب"
    2 -> "ربع الحزب"
    3 -> "نصف الحزب"
    4 -> "ثلاثة أرباع الحزب"
    else -> "ربع الحزب"
}

private fun rubFractionLabel(rubInHizb: Int, hizbNumber: Int): String {
    val hizbAr = toArabicIndicDigits(hizbNumber)
    return when (rubInHizb) {
        1 -> "بداية الحزب $hizbAr"
        2 -> "ربع الحزب $hizbAr"
        3 -> "نصف الحزب $hizbAr"
        4 -> "ثلاثة أرباع الحزب $hizbAr"
        else -> "الحزب $hizbAr"
    }
}

private fun rubFractionAccentColor(rubInHizb: Int, isNightMode: Boolean): Color = when (rubInHizb) {
    1 -> if (isNightMode) Color(0xFF34D399) else Color(0xFF067A4F) // بداية الحزب
    2 -> if (isNightMode) Color(0xFF38BDF8) else Color(0xFF0E7490) // ربع الحزب
    3 -> if (isNightMode) Color(0xFFFBBF24) else Color(0xFFB45309) // نصف الحزب
    4 -> if (isNightMode) Color(0xFFC084FC) else Color(0xFF6D28D9) // ثلاثة أرباع الحزب
    else -> if (isNightMode) GoldAccent else Color(0xFF0F5257)
}

@Composable
private fun RubAlHizbMilestoneBanner(
    rubStartVerses: List<OfflineVerse>,
    isNightMode: Boolean
) {
    val primary = rubStartVerses.first()
    val accent = rubFractionAccentColor(primary.rubInHizb, isNightMode)
    val bannerText = remember(rubStartVerses) {
        rubStartVerses.joinToString("  |  ") { v ->
            "${rubFractionLabel(v.rubInHizb, v.hizb)} (آية ${toArabicIndicDigits(v.ayahNumber)})"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rub_al_hizb_banner"),
        shape = RoundedCornerShape(10.dp),
        color = if (isNightMode) accent.copy(alpha = 0.16f) else accent.copy(alpha = 0.09f),
        border = BorderStroke(1.3.dp, accent.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RubAlHizbStarIcon(
                isNightMode = isNightMode,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = bannerText,
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.5.sp,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            RubAlHizbStarIcon(
                isNightMode = isNightMode,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun DrawScope.drawRubAlHizbStarOnCanvas(
    cx: Float,
    cy: Float,
    radius: Float,
    isNightMode: Boolean
) {
    val rOuter = radius.coerceAtLeast(6f)
    val goldColor = if (isNightMode) Color(0xFFF5D06B) else Color(0xFFC8961E)
    val emeraldOuter = if (isNightMode) Color(0xFF10B981) else Color(0xFF066C46)
    val innerFill = if (isNightMode) Color(0xFF0D2137) else Color(0xFFFFFFFF)

    fun drawOctagram(r: Float, color: Color) {
        val halfSide = r * 0.7071f
        val sq1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - halfSide, cy - halfSide)
            lineTo(cx + halfSide, cy - halfSide)
            lineTo(cx + halfSide, cy + halfSide)
            lineTo(cx - halfSide, cy + halfSide)
            close()
        }
        drawPath(path = sq1, color = color)

        val sq2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy)
            close()
        }
        drawPath(path = sq2, color = color)
    }

    drawOctagram(rOuter, goldColor)
    drawOctagram(rOuter * 0.80f, emeraldOuter)
    drawOctagram(rOuter * 0.56f, innerFill)
    drawCircle(color = goldColor, radius = rOuter * 0.24f, center = androidx.compose.ui.geometry.Offset(cx, cy))
}

@Composable
private fun RubAlHizbStarIcon(
    isNightMode: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawRubAlHizbStarOnCanvas(
            cx = size.width * 0.5f,
            cy = size.height * 0.5f,
            radius = minOf(size.width, size.height) * 0.48f,
            isNightMode = isNightMode
        )
    }
}

@Composable
private fun SurahOrnamentalBanner(
    surahNameAr: String,
    revelationPlaceAr: String,
    isNightMode: Boolean,
    onHeightMeasured: (Int) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightMeasured(it.height) }
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isNightMode) RoyalNavyDark else Color(0xFFF8F9FA),
        border = BorderStroke(1.2.dp, GoldAccent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RubAlHizbStarIcon(
                isNightMode = isNightMode,
                modifier = Modifier.size(18.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "سُورَةُ $surahNameAr",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    color = if (isNightMode) GoldAccent else Color(0xFF0F5257)
                )
                Text(
                    text = "(${revelationPlaceAr})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNightMode) Color.White.copy(alpha = 0.78f) else Color(0xFF4B5563)
                )
            }
            RubAlHizbStarIcon(
                isNightMode = isNightMode,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun DrawScope.drawWaqfGreenRibbonOnCanvas(
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val w = width
    val h = height
    val cx = left + w * 0.5f

    val goldOuter = Color(0xFFD9B863)
    val goldBright = Color(0xFFE5C878)
    val emeraldDark = Color(0xFF065535)
    val emeraldMid = Color(0xFF0B6E45)
    val emeraldCore = Color(0xFF043D26)

    // 1. Vertical Green Ribbon Body with Swallowtail V-Notch at bottom
    val ribbonLeft = left + w * 0.24f
    val ribbonRight = left + w * 0.76f
    val ribbonTop = top + h * 0.24f
    val ribbonBottom = top + h * 0.96f
    val ribbonNotchY = top + h * 0.88f

    val outerRibbonPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(ribbonLeft, ribbonTop)
        lineTo(ribbonRight, ribbonTop)
        lineTo(ribbonRight, ribbonBottom)
        lineTo(cx, ribbonNotchY)
        lineTo(ribbonLeft, ribbonBottom)
        close()
    }
    drawPath(path = outerRibbonPath, color = emeraldDark)

    val innerMargin = w * 0.055f
    val innerRibbonPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(ribbonLeft + innerMargin, ribbonTop)
        lineTo(ribbonRight - innerMargin, ribbonTop)
        lineTo(ribbonRight - innerMargin, ribbonBottom - h * 0.02f)
        lineTo(cx, ribbonNotchY - h * 0.015f)
        lineTo(ribbonLeft + innerMargin, ribbonBottom - h * 0.02f)
        close()
    }
    drawPath(path = innerRibbonPath, color = emeraldMid)

    // 2. Top 8-Pointed Gold & Emerald Islamic Rosette Medallion
    val rosetteCenterY = top + h * 0.21f
    val rOuter = minOf(w * 0.46f, h * 0.20f)

    fun drawOctagramStar(radius: Float, color: Color) {
        val halfSide = radius * 0.7071f
        // Axis-aligned square
        val sq1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - halfSide, rosetteCenterY - halfSide)
            lineTo(cx + halfSide, rosetteCenterY - halfSide)
            lineTo(cx + halfSide, rosetteCenterY + halfSide)
            lineTo(cx - halfSide, rosetteCenterY + halfSide)
            close()
        }
        drawPath(path = sq1, color = color)

        // 45-degree rotated diamond square
        val sq2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, rosetteCenterY - radius)
            lineTo(cx + radius, rosetteCenterY)
            lineTo(cx, rosetteCenterY + radius)
            lineTo(cx - radius, rosetteCenterY)
            close()
        }
        drawPath(path = sq2, color = color)
    }

    drawOctagramStar(rOuter, goldOuter)
    drawOctagramStar(rOuter * 0.82f, emeraldDark)
    drawOctagramStar(rOuter * 0.64f, goldBright)
    drawOctagramStar(rOuter * 0.46f, emeraldCore)
    drawOctagramStar(rOuter * 0.28f, goldOuter)
    drawOctagramStar(rOuter * 0.16f, emeraldMid)
}

@Composable
private fun WaqfGreenRibbonBadge(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawWaqfGreenRibbonOnCanvas(
            left = 0f,
            top = 0f,
            width = size.width,
            height = size.height
        )
    }
}


