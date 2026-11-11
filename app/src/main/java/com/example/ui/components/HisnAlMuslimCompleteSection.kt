package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.QuranTextMatcher
import com.example.data.local.AdhkarCustomizationManager
import com.example.data.local.LocalKhatmaStorage
import com.example.data.local.UnifiedDhikrItem
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import kotlinx.coroutines.delay
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
    val targetCount: Int,
    val benefit: String = "",
    val isCustomAdded: Boolean = false
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

    /**
     * Returns all 132 chapters with user customizations applied:
     * - Long-press text & repeat-count overrides
     * - Deleted built-in adhkar filtered out
     * - Custom manually added adhkar for each chapter included
     */
    fun getEffectiveChapters(context: Context): List<HisnCategoryChapter> {
        val baseChapters = getChapters(context)
        val overrides = AdhkarCustomizationManager.getOverridesMap(context)
        val deletedKeys = AdhkarCustomizationManager.getDeletedKeys(context)
        val allCustomAdded = AdhkarCustomizationManager.getCustomAddedAdhkar(context)

        return baseChapters.map { ch ->
            val scopeKey = "hisn_ch_${ch.id}"
            val chapterCustomDuas = allCustomAdded
                .filter { it.scopeKey == scopeKey && !deletedKeys.contains(it.uniqueKey) }
                .mapIndexed { idx, custom ->
                    val ov = overrides[custom.uniqueKey]
                    val finalText = ov?.arabicText?.ifBlank { custom.arabicText } ?: custom.arabicText
                    HisnDuaEntry(
                        uniqueKey = custom.uniqueKey,
                        idInChapter = idx + 1,
                        categoryId = ch.id,
                        categoryTitle = ch.title,
                        arabicText = finalText,
                        normalizedText = QuranTextMatcher.normalizeArabic(finalText),
                        targetCount = ov?.targetCount ?: custom.targetCount,
                        benefit = ov?.benefit ?: custom.benefit,
                        isCustomAdded = true
                    )
                }

            val modifiedBuiltIn = ch.duas
                .filter { !deletedKeys.contains(it.uniqueKey) }
                .map { dua ->
                    val ov = overrides[dua.uniqueKey]
                    if (ov != null) {
                        val newText = ov.arabicText.ifBlank { dua.arabicText }
                        dua.copy(
                            arabicText = newText,
                            normalizedText = QuranTextMatcher.normalizeArabic(newText),
                            targetCount = ov.targetCount.coerceAtLeast(1),
                            benefit = ov.benefit
                        )
                    } else {
                        dua
                    }
                }

            val combined = (chapterCustomDuas + modifiedBuiltIn).mapIndexed { index, entry ->
                entry.copy(idInChapter = index + 1)
            }
            ch.copy(duas = combined)
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
 * Full-screen Offline Hisn Al-Muslim Encyclopedia Dialog (132 Chapters, Authentic Duas)
 * with Heart Favorites on every Chapter & every Dhikr, and Long-Press editing of text & repeat counts.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HisnAlMuslimCompleteDialog(
    initialGroupKey: String = HisnSectionGroup.ALL.key,
    initialChapterId: Int? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE) }
    val customizationVersion by AdhkarCustomizationManager.versionFlow.collectAsState()
    var dailyResetTrigger by remember { mutableStateOf(0) }

    // Automatic Daily Reset at 12:00 AM midnight while Hisn Al-Muslim is open
    LaunchedEffect(Unit) {
        ensureDailyAdhkarResetIfNewDay(prefs, context)
        dailyResetTrigger++
        while (true) {
            delay(20_000L)
            if (ensureDailyAdhkarResetIfNewDay(prefs, context)) {
                dailyResetTrigger++
            }
        }
    }

    val allChapters = remember(customizationVersion, dailyResetTrigger) {
        HisnAlMuslimRepository.getEffectiveChapters(context)
    }
    val favoriteChapterIds = remember(customizationVersion) {
        AdhkarCustomizationManager.getFavoriteChapterIds(context)
    }
    val favoriteItemKeys = remember(customizationVersion) {
        AdhkarCustomizationManager.getFavoriteItems(context).map { it.uniqueKey }.toSet()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember {
        mutableStateOf(
            HisnSectionGroup.values().find { it.key == initialGroupKey } ?: HisnSectionGroup.ALL
        )
    }
    var activeChapterId by remember { mutableStateOf(initialChapterId) }
    val activeChapter = remember(allChapters, activeChapterId) {
        activeChapterId?.let { targetId -> allChapters.find { it.id == targetId } }
    }

    var showFavoritesDialogFromInside by remember { mutableStateOf(false) }
    var showAddDhikrInActiveChapter by remember { mutableStateOf(false) }
    var editingDuaEntry by remember { mutableStateOf<HisnDuaEntry?>(null) }

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

    val unifiedLightBg = Color(0xFFFDF8EC)
    val unifiedLightCard = Color(0xFFF5EFE0)
    val unifiedGoldTitle = EmeraldGreen
    val unifiedGoldDivider = GoldBorder
    val darkInkText = Color(0xFF1A110B)

    Dialog(
        onDismissRequest = {
            if (activeChapterId != null && initialChapterId == null) {
                activeChapterId = null
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
            color = unifiedLightBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Ornate Header Bar (with Heart Icon next to X button as marked in user's screenshot)
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
                                        onClick = {
                                            if (initialChapterId != null) onDismiss() else activeChapterId = null
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "العودة للفهرس",
                                            tint = EmeraldGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GoldAccent.copy(alpha = 0.15f))
                                            .border(1.dp, GoldAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeChapter?.title ?: "فهرس حصن المسلم الكامل",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        lineHeight = 26.sp,
                                        color = EmeraldGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (activeChapter != null)
                                            "الباب رقم ${activeChapter.id} من ١٣٢ • ${activeChapter.duas.size} ذكر ودعاء"
                                        else
                                            "جميع الأبواب والأدعية (${allChapters.size} باباً) أوفلاين بالكامل",
                                        fontSize = 11.sp,
                                        color = darkInkText.copy(alpha = 0.78f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Action Icons on the left of the Header (Heart Icon in the red box + Add + Close X)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (activeChapter != null) {
                                    val isChapFav = favoriteChapterIds.contains(activeChapter.id)
                                    // Heart Button for the entire Chapter (exact position marked in Screenshot 2)
                                    IconButton(
                                        onClick = {
                                            val added = AdhkarCustomizationManager.toggleChapterFavorite(context, activeChapter.id)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            Toast.makeText(
                                                context,
                                                if (added) "تمت إضافة باب «${activeChapter.title}» إلى المفضلة ❤️"
                                                else "تمت إزالة الباب من المفضلة",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFDF8EC))
                                            .border(1.dp, if (isChapFav) FavoriteHeartCrimson else GoldBorder, CircleShape)
                                            .testTag("toggle_chapter_favorite_header_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isChapFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "إضافة الباب للمفضلة",
                                            tint = if (isChapFav) FavoriteHeartCrimson else GoldAccent,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Quick Add Custom Dhikr to this Chapter
                                    IconButton(
                                        onClick = { showAddDhikrInActiveChapter = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFDF8EC))
                                            .border(1.dp, EmeraldGreen, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "إضافة ذكر لهذا الباب",
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                } else {
                                    // In Index View: Manual Reset All Hisn Chapters Button + Quick Heart Button to open Favorites Dialog
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFFDF8EC),
                                        border = BorderStroke(1.dp, GoldAccent),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                val editor = prefs.edit()
                                                allChapters.forEach { ch ->
                                                    ch.duas.forEach { d ->
                                                        editor.putInt(d.uniqueKey, 0)
                                                    }
                                                }
                                                editor.apply()
                                                dailyResetTrigger++
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(
                                                    context,
                                                    "تم تصفير عدادات كافة أبواب حصن المسلم بنجاح",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .testTag("reset_all_hisn_chapters_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "تصفير الكل",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "تصفير الكل",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFFDF8EC),
                                        border = BorderStroke(1.dp, FavoriteHeartCrimson),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { showFavoritesDialogFromInside = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "الأذكار المفضلة",
                                                tint = FavoriteHeartCrimson,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "المفضلة",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FavoriteHeartCrimson
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (activeChapterId != null && initialChapterId == null) {
                                            activeChapterId = null
                                        } else {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = EmeraldGreen
                                    )
                                }
                            }
                        }

                        // Search Bar (Visible in Index view — concise single-line placeholder as marked in Screenshot 1)
                        if (activeChapter == null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "ابحث عن أي دعاء أو باب (الاستخارة، السفر، النوم...)",
                                        fontSize = 12.sp,
                                        color = darkInkText.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = EmeraldGreen
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
                                    focusedTextColor = darkInkText,
                                    unfocusedTextColor = darkInkText,
                                    focusedContainerColor = Color(0xFFFDF8EC),
                                    unfocusedContainerColor = Color(0xFFFDF8EC),
                                    focusedBorderColor = EmeraldGreen,
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
                                        color = if (isSelected) EmeraldGreen else Color(0xFFFDF8EC),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) EmeraldGreen else GoldBorder
                                        ),
                                        modifier = Modifier.clickable {
                                            selectedGroup = group
                                        }
                                    ) {
                                        Text(
                                            text = group.labelAr,
                                            color = if (isSelected) Color.White else darkInkText,
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
                        resetTrigger = dailyResetTrigger,
                        parchmentCard = unifiedLightCard,
                        favoriteItemKeys = favoriteItemKeys,
                        onBackToIndex = {
                            if (initialChapterId != null) onDismiss() else activeChapterId = null
                        },
                        onNextChapter = {
                            val idx = allChapters.indexOfFirst { it.id == selectedChapter.id }
                            if (idx in 0 until allChapters.size - 1) {
                                activeChapterId = allChapters[idx + 1].id
                            }
                        },
                        onPrevChapter = {
                            val idx = allChapters.indexOfFirst { it.id == selectedChapter.id }
                            if (idx > 0) {
                                activeChapterId = allChapters[idx - 1].id
                            }
                        },
                        onAddDhikrClicked = { showAddDhikrInActiveChapter = true },
                        onEditDhikrClicked = { dua -> editingDuaEntry = dua }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                        tint = unifiedGoldTitle,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الفهرس (${filteredChapters.size} باباً)",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 23.sp,
                                        color = unifiedGoldTitle
                                    )
                                }
                                if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = "نتائج الأدعية المطابقة: ${directSearchDuaMatches.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 1.2.dp, color = unifiedGoldDivider)
                        }

                        // Index List of Chapters — Each Chapter has an external Heart Icon to add/remove from Favorites
                        items(filteredChapters, key = { "ch_${it.id}" }) { chapter ->
                            val isChapFav = favoriteChapterIds.contains(chapter.id)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = unifiedLightCard,
                                border = BorderStroke(
                                    width = if (isChapFav) 1.4.dp else 1.dp,
                                    color = if (isChapFav) FavoriteHeartCrimson.copy(alpha = 0.65f) else unifiedGoldDivider
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeChapterId = chapter.id
                                    }
                                    .testTag("hisn_chapter_item_${chapter.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldGreen.copy(alpha = 0.12f),
                                            border = BorderStroke(0.8.dp, EmeraldGreen)
                                        ) {
                                            Text(
                                                text = "${chapter.id}",
                                                color = EmeraldGreen,
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
                                            fontSize = 19.sp,
                                            lineHeight = 27.sp,
                                            color = darkInkText,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = EmeraldGreen,
                                            border = BorderStroke(0.6.dp, GoldBorder)
                                        ) {
                                            Text(
                                                text = "${chapter.duas.size} ذكر",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        // External Heart Icon for each Chapter (باب)
                                        IconButton(
                                            onClick = {
                                                val added = AdhkarCustomizationManager.toggleChapterFavorite(context, chapter.id)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(
                                                    context,
                                                    if (added) "تمت إضافة باب «${chapter.title}» إلى المفضلة ❤️"
                                                    else "تمت إزالة الباب من المفضلة",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("favorite_chapter_button_${chapter.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isChapFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "إضافة الباب للمفضلة",
                                                tint = if (isChapFav) FavoriteHeartCrimson else GoldAccent,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

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
                                val isDuaFav = favoriteItemKeys.contains(dua.uniqueKey)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = unifiedLightCard,
                                    border = BorderStroke(1.2.dp, GoldAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                activeChapterId = dua.categoryId
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                editingDuaEntry = dua
                                            }
                                        )
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val unified = UnifiedDhikrItem(
                                                            uniqueKey = dua.uniqueKey,
                                                            scopeKey = "hisn_ch_${dua.categoryId}",
                                                            title = "${dua.categoryTitle} (${dua.idInChapter})",
                                                            categoryTitle = dua.categoryTitle,
                                                            arabicText = dua.arabicText,
                                                            benefit = dua.benefit,
                                                            targetCount = dua.targetCount
                                                        )
                                                        val added = AdhkarCustomizationManager.toggleDhikrFavorite(context, unified)
                                                        Toast.makeText(
                                                            context,
                                                            if (added) "تمت إضافة الذكر إلى المفضلة ❤️" else "تمت إزالة الذكر من المفضلة",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isDuaFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "إضافة للمفضلة",
                                                        tint = if (isDuaFav) FavoriteHeartCrimson else GoldAccent,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "باب: ${dua.categoryTitle}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = EmeraldGreen,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = EmeraldGreen
                                            ) {
                                                Text(
                                                    text = formatRepeatCountArabic(dua.targetCount),
                                                    color = Color.White,
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
                                            color = darkInkText
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
                                        color = EmeraldGreen,
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

    if (showFavoritesDialogFromInside) {
        FavoriteAdhkarDialog(
            onOpenHisnChapter = { ch ->
                showFavoritesDialogFromInside = false
                activeChapterId = ch.id
            },
            onDismiss = { showFavoritesDialogFromInside = false }
        )
    }

    if (showAddDhikrInActiveChapter && activeChapter != null) {
        AddOrEditDhikrDialog(
            isEditMode = false,
            scopeKey = "hisn_ch_${activeChapter.id}",
            categoryTitle = activeChapter.title,
            onDismiss = { showAddDhikrInActiveChapter = false }
        )
    }

    editingDuaEntry?.let { target ->
        AddOrEditDhikrDialog(
            isEditMode = true,
            scopeKey = "hisn_ch_${target.categoryId}",
            categoryTitle = target.categoryTitle,
            initialUniqueKey = target.uniqueKey,
            initialTitle = "${target.categoryTitle} - الذكر (${target.idInChapter})",
            initialArabicText = target.arabicText,
            initialBenefit = target.benefit,
            initialTargetCount = target.targetCount,
            onDismiss = { editingDuaEntry = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HisnChapterDetailView(
    chapter: HisnCategoryChapter,
    prefs: android.content.SharedPreferences,
    resetTrigger: Int = 0,
    parchmentCard: Color,
    favoriteItemKeys: Set<String>,
    onBackToIndex: () -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    onAddDhikrClicked: () -> Unit,
    onEditDhikrClicked: (HisnDuaEntry) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }

    val counts = remember(chapter.id, chapter.duas, resetTrigger) {
        ensureDailyAdhkarResetIfNewDay(prefs, context)
        mutableStateMapOf<String, Int>().apply {
            chapter.duas.forEach { d ->
                this[d.uniqueKey] = prefs.getInt(d.uniqueKey, 0)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chapter Navigation Subbar (Prev / Index / Next)
        Surface(
            color = Color(0xFFF3E9D2),
            border = BorderStroke(0.8.dp, GoldBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrevChapter,
                        enabled = chapter.id > 1,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen)
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
                        border = BorderStroke(1.dp, GoldBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen)
                    ) {
                        Text("الباب التالي", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Helper Hint Bar: Long-press to edit text/repeat count + Manual Reset Chapter button + Add Dhikr button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 اضغط مطولاً على أي ذكر لتعديل نصه أو تكراره أو حذفه",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Manual Reset Button for this Chapter (تصفير الباب)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFDF8EC),
                            border = BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val editor = prefs.edit()
                                    chapter.duas.forEach { d ->
                                        counts[d.uniqueKey] = 0
                                        editor.putInt(d.uniqueKey, 0)
                                    }
                                    editor.apply()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(
                                        context,
                                        "تم تصفير عدادات باب «${chapter.title}»",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("reset_hisn_chapter_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تصفير الباب",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "تصفير الباب",
                                    color = GoldAccent,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddDhikrClicked() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "إضافة ذكر",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
                val isFav = favoriteItemKeys.contains(dua.uniqueKey)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .combinedClickable(
                            onClick = {
                                if (currentCount < dua.targetCount) {
                                    val next = currentCount + 1
                                    counts[dua.uniqueKey] = next
                                    prefs.edit().putInt(dua.uniqueKey, next).apply()
                                    localStorage.incrementCumulativeAdhkarTasbeeh(1)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEditDhikrClicked(dua)
                            }
                        )
                        .border(
                            width = if (isCompleted) 1.8.dp else 1.dp,
                            color = if (isCompleted) EmeraldGreen else GoldBorder,
                            shape = RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) EmeraldGreen.copy(alpha = 0.10f) else parchmentCard
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Top Row: Heart Favorite Icon + Number badge + Edit chip + Target Count badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Heart Button to Favorite/Unfavorite this individual Dhikr
                                IconButton(
                                    onClick = {
                                        val unified = UnifiedDhikrItem(
                                            uniqueKey = dua.uniqueKey,
                                            scopeKey = "hisn_ch_${chapter.id}",
                                            title = "${chapter.title} (${dua.idInChapter})",
                                            categoryTitle = chapter.title,
                                            arabicText = dua.arabicText,
                                            benefit = dua.benefit,
                                            targetCount = dua.targetCount,
                                            isCustomAdded = dua.isCustomAdded
                                        )
                                        val added = AdhkarCustomizationManager.toggleDhikrFavorite(context, unified)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(
                                            context,
                                            if (added) "تمت إضافة الذكر إلى المفضلة ❤️" else "تمت إزالة الذكر من المفضلة",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("favorite_dua_button_${dua.uniqueKey}")
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "إضافة الذكر للمفضلة",
                                        tint = if (isFav) FavoriteHeartCrimson else GoldAccent,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = "الذكر (${dua.idInChapter})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = EmeraldGreen
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Edit / Repeat Count Chip (also triggered on Long-Press)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEBE1C9),
                                    border = BorderStroke(0.8.dp, GoldBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onEditDhikrClicked(dua) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل الذكر والتكرار",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "تعديل",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A110B)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCompleted) GoldAccent else EmeraldGreen,
                                    border = BorderStroke(0.8.dp, GoldAccent)
                                ) {
                                    Text(
                                        text = formatRepeatCountArabic(dua.targetCount),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
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
                            color = Color(0xFF1A110B),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (dua.benefit.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "الفضل: ${dua.benefit}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF1A110B).copy(alpha = 0.72f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom Action Row: Counter (unclipped single-line) + Copy + Share + Reset
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
                                        localStorage.incrementCumulativeAdhkarTasbeeh(1)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCompleted) GoldAccent else EmeraldGreen,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, GoldAccent),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 46.dp)
                            ) {
                                Text(
                                    text = if (isCompleted) "تمت القراءة ✓ ($currentCount/${dua.targetCount})"
                                    else "اضغط للعد ($currentCount / ${dua.targetCount})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                border = BorderStroke(1.dp, GoldBorder),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ الدعاء",
                                    tint = GoldAccent,
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
                                        append("تطبيق محرابي | إعداد: آسر الشربيني • وقفٌ لله تعالى وصدقةٌ جارية عني وعن والديّ")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, sharePayload)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الدعاء"))
                                },
                                border = BorderStroke(1.dp, GoldAccent),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "مشاركة الدعاء",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (currentCount > 0) {
                                OutlinedIconButton(
                                    onClick = {
                                        counts[dua.uniqueKey] = 0
                                        prefs.edit().putInt(dua.uniqueKey, 0).apply()
                                    },
                                    border = BorderStroke(1.dp, GoldBorder),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تصفير العداد",
                                        tint = GoldAccent,
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
