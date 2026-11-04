package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.QuranTextMatcher
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

data class HisnDuaEntry(
    val uniqueKey: String,
    val idInChapter: Int,
    val categoryId: Int,
    val categoryTitle: String,
    val arabicText: String,
    val normalizedText: String,
    val targetCount: Int
)

data class HisnCategoryChapter(
    val id: Int,
    val title: String,
    val normalizedTitle: String,
    val groupKey: String,
    val duas: List<HisnDuaEntry>
)

enum class HisnSectionGroup(val key: String, val labelAr: String) {
    ALL("all", "الفهرس الكامل (١٣٢ باباً)"),
    SLEEP_MORNING("sleep_morning", "الصباح والمساء والنوم"),
    HOME_DRESS_WUDU("home_dress", "اللباس والمنزل والوضوء"),
    PRAYER_MOSQUE("prayer", "الأذان والمسجد والصلاة"),
    DISTRESS_ILLNESS("distress", "الهم والكرب والمرض والجنائز"),
    NATURE_FOOD_FAMILY("food_family", "المطر والطعام والزواج والمجلس"),
    TRAVEL_HAJJ("travel_hajj", "السفر والحج والعمرة"),
    ISTIGHFAR_ADAB("istighfar", "الاستغفار والتسبيح والآداب")
}

object HisnAlMuslimRepository {
    private const val ASSET_FILE = "hisn_al_muslim_complete.json"

    @Volatile
    private var cachedChapters: List<HisnCategoryChapter>? = null

    fun getChapters(context: Context): List<HisnCategoryChapter> {
        cachedChapters?.let { return it }
        synchronized(this) {
            cachedChapters?.let { return it }
            val loaded = loadFromAssets(context.applicationContext)
            cachedChapters = loaded
            return loaded
        }
    }

    private fun loadFromAssets(context: Context): List<HisnCategoryChapter> {
        val result = ArrayList<HisnCategoryChapter>(135)
        try {
            context.assets.open(ASSET_FILE).use { stream ->
                val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val cObj = arr.getJSONObject(i)
                    val cId = cObj.optInt("id", i + 1)
                    val cTitle = cObj.optString("category", "").trim()
                    val dArr = cObj.optJSONArray("array") ?: JSONArray()
                    val duas = ArrayList<HisnDuaEntry>(dArr.length())

                    for (j in 0 until dArr.length()) {
                        val dObj = dArr.getJSONObject(j)
                        val dId = dObj.optInt("id", j + 1)
                        val dText = dObj.optString("text", "").trim()
                        val dCount = dObj.optInt("count", 1).coerceAtLeast(1)
                        if (dText.isNotBlank()) {
                            duas.add(
                                HisnDuaEntry(
                                    uniqueKey = "hisn_${cId}_${dId}",
                                    idInChapter = dId,
                                    categoryId = cId,
                                    categoryTitle = cTitle,
                                    arabicText = dText,
                                    normalizedText = QuranTextMatcher.normalizeArabic(dText),
                                    targetCount = dCount
                                )
                            )
                        }
                    }

                    if (cTitle.isNotBlank() && duas.isNotEmpty()) {
                        result.add(
                            HisnCategoryChapter(
                                id = cId,
                                title = cTitle,
                                normalizedTitle = QuranTextMatcher.normalizeArabic(cTitle),
                                groupKey = classifyChapterGroup(cTitle),
                                duas = duas
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun classifyChapterGroup(title: String): String {
        val t = QuranTextMatcher.normalizeArabic(title)
        return when {
            t.contains("نوم") || t.contains("صباح") || t.contains("مساء") || t.contains("استيقاظ") || t.contains("تقلب") || t.contains("رؤيا") || t.contains("حلم") ->
                HisnSectionGroup.SLEEP_MORNING.key
            t.contains("ثوب") || t.contains("لبس") || t.contains("خلاء") || t.contains("وضوء") || t.contains("منزل") ->
                HisnSectionGroup.HOME_DRESS_WUDU.key
            t.contains("مسجد") || t.contains("اذان") || t.contains("استفتاح") || t.contains("ركوع") || t.contains("سجود") ||
                t.contains("تشهد") || t.contains("صلاه") || t.contains("وتر") || t.contains("استخاره") || t.contains("سلام من الصلاه") ->
                HisnSectionGroup.PRAYER_MOSQUE.key
            t.contains("هم") || t.contains("كرب") || t.contains("حزن") || t.contains("عدو") || t.contains("سلطان") ||
                t.contains("دين") || t.contains("وسوسه") || t.contains("استصعب") || t.contains("ذنب") || t.contains("شيطان") ||
                t.contains("مريض") || t.contains("محتضر") || t.contains("مصيبه") || t.contains("ميت") || t.contains("تعزيه") ||
                t.contains("قبر") || t.contains("قبور") || t.contains("وجع") || t.contains("فزع") || t.contains("عينه") ->
                HisnSectionGroup.DISTRESS_ILLNESS.key
            t.contains("ريح") || t.contains("رعد") || t.contains("مطر") || t.contains("استسقاء") || t.contains("استصحاء") ||
                t.contains("هلال") || t.contains("صائم") || t.contains("افطار") || t.contains("طعام") || t.contains("افطر") ||
                t.contains("ثمر") || t.contains("عطاس") || t.contains("عطس") || t.contains("متزوج") || t.contains("زوجه") ||
                t.contains("مولود") || t.contains("اولاد") || t.contains("مجلس") || t.contains("غضب") || t.contains("مبتلي") ->
                HisnSectionGroup.NATURE_FOOD_FAMILY.key
            t.contains("سفر") || t.contains("ركوب") || t.contains("مركوب") || t.contains("مسافر") || t.contains("قريه") ||
                t.contains("سوق") || t.contains("حج") || t.contains("عمره") || t.contains("ركن") || t.contains("صفا") ||
                t.contains("عرفه") || t.contains("مشعر") || t.contains("جمار") || t.contains("ذبح") ->
                HisnSectionGroup.TRAVEL_HAJJ.key
            else -> HisnSectionGroup.ISTIGHFAR_ADAB.key
        }
    }
}

/**
 * Full-screen Offline Hisn Al-Muslim Encyclopedia Dialog (132 Chapters, 267 Authentic Duas)
 * styled after the classic parchment index (`الفهرس`) of hisnmuslim.com/i/ar/1 with instant search,
 * thematic section tabs, interactive counters, copy, and share.
 */
@Composable
fun HisnAlMuslimCompleteDialog(
    initialGroupKey: String = HisnSectionGroup.ALL.key,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE) }

    val allChapters = remember { HisnAlMuslimRepository.getChapters(context) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember {
        mutableStateOf(
            HisnSectionGroup.values().find { it.key == initialGroupKey } ?: HisnSectionGroup.ALL
        )
    }
    var activeChapter by remember { mutableStateOf<HisnCategoryChapter?>(null) }

    val normalizedQuery = remember(searchQuery) {
        QuranTextMatcher.normalizeArabic(searchQuery)
    }

    val filteredChapters = remember(allChapters, selectedGroup, normalizedQuery) {
        allChapters.filter { chapter ->
            val groupMatch = selectedGroup == HisnSectionGroup.ALL || chapter.groupKey == selectedGroup.key
            if (!groupMatch) return@filter false
            if (normalizedQuery.isBlank()) return@filter true

            chapter.normalizedTitle.contains(normalizedQuery) ||
                chapter.duas.any { it.normalizedText.contains(normalizedQuery) }
        }
    }

    // Direct Dua matches when user searches for a specific phrase inside any Dua
    val directSearchDuaMatches = remember(allChapters, normalizedQuery) {
        if (normalizedQuery.length < 2) {
            emptyList()
        } else {
            allChapters.flatMap { ch ->
                ch.duas.filter {
                    it.normalizedText.contains(normalizedQuery) || ch.normalizedTitle.contains(normalizedQuery)
                }
            }.take(40)
        }
    }

    val parchmentBg = Color(0xFFF6E7CB)
    val parchmentCard = Color(0xFFFDF6E7)
    val warmBrownTitle = Color(0xFF5D3A1A)
    val warmGoldDivider = Color(0xFFD6BA8A)

    Dialog(
        onDismissRequest = {
            if (activeChapter != null) {
                activeChapter = null
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("hisn_al_muslim_complete_dialog"),
            color = parchmentBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Ornate Header Bar
                Surface(
                    color = RoyalNavyDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (activeChapter != null) {
                                    IconButton(
                                        onClick = { activeChapter = null },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "العودة للفهرس",
                                            tint = GoldAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GoldAccent.copy(alpha = 0.18f))
                                            .border(1.dp, GoldAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeChapter?.title ?: "فهرس حصن المسلم الكامل (أوفلاين)",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = GoldAccent,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (activeChapter != null)
                                            "الباب رقم ${activeChapter!!.id} من ١٣٢ • ${activeChapter!!.duas.size} ذكر ودعاء صحيح"
                                        else
                                            "جميع أبواب وأدعية حصن المسلم (${allChapters.size} باباً) كاملة بدون إنترنت",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFDF8EE).copy(alpha = 0.85f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (activeChapter != null) {
                                        activeChapter = null
                                    } else {
                                        onDismiss()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = GoldAccent
                                )
                            }
                        }

                        // Search Bar (Visible in Index view)
                        if (activeChapter == null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "ابحث عن أي دعاء أو باب (مثال: الاستخارة، السفر، الكرب، النوم...)",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFDF8EE).copy(alpha = 0.65f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = GoldAccent
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "مسح البحث",
                                                tint = GoldAccent
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFFFDF8EE),
                                    unfocusedTextColor = Color(0xFFFDF8EE),
                                    focusedContainerColor = Color(0xFF162844),
                                    unfocusedContainerColor = Color(0xFF162844),
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = GoldBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hisn_search_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Section Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HisnSectionGroup.values().forEach { group ->
                                    val isSelected = selectedGroup == group
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) GoldAccent else Color(0xFF162844),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) GoldAccent else GoldBorder.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.clickable {
                                            selectedGroup = group
                                        }
                                    ) {
                                        Text(
                                            text = group.labelAr,
                                            color = if (isSelected) RoyalNavyDark else Color(0xFFFDF8EE),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Main Content: Either Chapter Details OR Classic Index ("الفهرس") + Search Results
                val selectedChapter = activeChapter
                if (selectedChapter != null) {
                    HisnChapterDetailView(
                        chapter = selectedChapter,
                        prefs = prefs,
                        parchmentCard = parchmentCard,
                        warmBrownTitle = warmBrownTitle,
                        onBackToIndex = { activeChapter = null },
                        onNextChapter = {
                            val idx = allChapters.indexOfFirst { it.id == selectedChapter.id }
                            if (idx in 0 until allChapters.size - 1) {
                                activeChapter = allChapters[idx + 1]
                            }
                        },
                        onPrevChapter = {
                            val idx = allChapters.indexOfFirst { it.id == selectedChapter.id }
                            if (idx > 0) {
                                activeChapter = allChapters[idx - 1]
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Classic "الفهرس" Header matching hisnmuslim.com/i/ar/1
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        tint = warmBrownTitle,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الفهرس (${filteredChapters.size} باباً)",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = warmBrownTitle
                                    )
                                }
                                if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = "نتائج الأدعية المطابقة: ${directSearchDuaMatches.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 1.2.dp, color = warmGoldDivider)
                        }

                        // Classic Index List of Chapters (matching hisnmuslim.com screenshots)
                        items(filteredChapters, key = { "ch_${it.id}" }) { chapter ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = parchmentCard,
                                border = BorderStroke(1.dp, warmGoldDivider),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeChapter = chapter
                                    }
                                    .testTag("hisn_chapter_item_${chapter.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = warmBrownTitle.copy(alpha = 0.12f),
                                            border = BorderStroke(0.8.dp, warmBrownTitle.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "${chapter.id}",
                                                color = warmBrownTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = chapter.title,
                                            fontFamily = AmiriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            lineHeight = 28.sp,
                                            color = warmBrownTitle,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldGreen.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "${chapter.duas.size} ذكر",
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // If searching, also display direct matching Dua cards below the matching chapters
                        if (normalizedQuery.length >= 2 && directSearchDuaMatches.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "نصوص الأدعية المطابقة لبحثك «$searchQuery»:",
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = EmeraldGreen
                                )
                            }
                            items(directSearchDuaMatches, key = { "search_${it.uniqueKey}" }) { dua ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = parchmentCard,
                                    border = BorderStroke(1.2.dp, GoldAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            allChapters.find { it.id == dua.categoryId }?.let {
                                                activeChapter = it
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "باب: ${dua.categoryTitle}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = EmeraldGreen,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = RoyalNavyDark
                                            ) {
                                                Text(
                                                    text = if (dua.targetCount == 1) "مرة واحدة" else "${dua.targetCount} مرات",
                                                    color = GoldAccent,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = dua.arabicText,
                                            fontFamily = AmiriFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp,
                                            lineHeight = 32.sp,
                                            color = Color(0xFF1F160A)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredChapters.isEmpty() && directSearchDuaMatches.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد نتائج مطابقة لـ «$searchQuery». جرب البحث بكلمة أخرى مثل: النوم، السفر، الاستخارة، المريض.",
                                        color = warmBrownTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
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

@Composable
private fun HisnChapterDetailView(
    chapter: HisnCategoryChapter,
    prefs: android.content.SharedPreferences,
    parchmentCard: Color,
    warmBrownTitle: Color,
    onBackToIndex: () -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val counts = remember(chapter.id) {
        mutableStateMapOf<String, Int>().apply {
            chapter.duas.forEach { d ->
                this[d.uniqueKey] = prefs.getInt(d.uniqueKey, 0)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chapter Navigation Subbar (Prev / Index / Next)
        Surface(
            color = Color(0xFFEDE0C8),
            border = BorderStroke(0.8.dp, Color(0xFFD6BA8A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrevChapter,
                    enabled = chapter.id > 1,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = warmBrownTitle)
                ) {
                    Text("الباب السابق", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onBackToIndex) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "الفهرس (١٣٢)",
                        color = EmeraldGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onNextChapter,
                    enabled = chapter.id < 132,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = warmBrownTitle)
                ) {
                    Text("الباب التالي", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chapter.duas, key = { it.uniqueKey }) { dua ->
                val currentCount = counts[dua.uniqueKey] ?: 0
                val isCompleted = currentCount >= dua.targetCount

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            if (currentCount < dua.targetCount) {
                                val next = currentCount + 1
                                counts[dua.uniqueKey] = next
                                prefs.edit().putInt(dua.uniqueKey, next).apply()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                        .border(
                            width = if (isCompleted) 1.8.dp else 1.dp,
                            color = if (isCompleted) EmeraldGreen else Color(0xFFD6BA8A),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) Color(0xFFE9F5EE) else parchmentCard
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Top Row: Number badge + Target Count badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = "الذكر (${dua.idInChapter})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isCompleted) EmeraldGreen else warmBrownTitle
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCompleted) EmeraldGreen else RoyalNavyDark
                            ) {
                                Text(
                                    text = when (dua.targetCount) {
                                        1 -> "مرة واحدة"
                                        2 -> "مرتان"
                                        in 3..10 -> "${dua.targetCount} مرات"
                                        else -> "${dua.targetCount} مرة"
                                    },
                                    color = if (isCompleted) Color.White else GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Full Authentic Vocalized Dua Text
                        Text(
                            text = dua.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            lineHeight = 36.sp,
                            color = Color(0xFF1B1309),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom Action Row: Counter + Copy + Share + Reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (currentCount < dua.targetCount) {
                                        val next = currentCount + 1
                                        counts[dua.uniqueKey] = next
                                        prefs.edit().putInt(dua.uniqueKey, next).apply()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCompleted) EmeraldGreen else RoyalNavyDark,
                                    contentColor = if (isCompleted) Color.White else GoldAccent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = if (isCompleted) "تمت القراءة ✓ ($currentCount/${dua.targetCount})"
                                    else "اضغط للعد ($currentCount / ${dua.targetCount})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }

                            // Copy Dua Button
                            OutlinedIconButton(
                                onClick = {
                                    val copyPayload = "${chapter.title}\n\n${dua.arabicText}\n\n[حصن المسلم - تطبيق محرابي]"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Hisn Dua", copyPayload))
                                    Toast.makeText(context, "تم نسخ الدعاء إلى الحافظة", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, warmBrownTitle.copy(alpha = 0.45f)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ الدعاء",
                                    tint = warmBrownTitle,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Share Dua Button
                            OutlinedIconButton(
                                onClick = {
                                    val sharePayload = buildString {
                                        append("📿 من أذكار وأدعية حصن المسلم\n")
                                        append("باب: ${chapter.title}\n")
                                        append("━━━━━━━━━━━━━━━━━━\n")
                                        append("${dua.arabicText}\n")
                                        append("━━━━━━━━━━━━━━━━━━\n")
                                        append("تطبيق محرابي | وقف لله تعالى")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, sharePayload)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الدعاء"))
                                },
                                border = BorderStroke(1.dp, EmeraldGreen),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "مشاركة الدعاء",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (currentCount > 0) {
                                OutlinedIconButton(
                                    onClick = {
                                        counts[dua.uniqueKey] = 0
                                        prefs.edit().putInt(dua.uniqueKey, 0).apply()
                                    },
                                    border = BorderStroke(1.dp, warmBrownTitle.copy(alpha = 0.35f)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تصفير العداد",
                                        tint = warmBrownTitle,
                                        modifier = Modifier.size(18.dp)
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
