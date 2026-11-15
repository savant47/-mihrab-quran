package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class NawawiHadithEntry(
    val number: Int,
    val numberLabelAr: String,
    val title: String,
    val narrator: String,
    val arabicText: String,
    val source: String,
    val vocabulary: String,
    val benefits: String,
    val categoryKey: String
) {
    val uniqueKey: String get() = "nawawi_hadith_$number"
    val normalizedSearchBlob: String by lazy {
        QuranTextMatcher.normalizeArabic("$numberLabelAr $title $narrator $arabicText $source $vocabulary $benefits")
    }
}

enum class NawawiCategoryFilter(val key: String, val labelAr: String) {
    ALL("all", "جميع الأحاديث (٤٢ حديثاً)"),
    FAITH_FOUNDATIONS("foundations", "أصول الإسلام والعقيدة"),
    WORSHIP_PURITY("worship", "العبادات والتقرب إلى الله"),
    ETHICS_BROTHERHOOD("ethics", "الأخلاق والآداب والأخوة"),
    ZUHUD_HEART("heart", "تزكية القلوب والورع والدعاء")
}

object AlNawawiFortyRepository {
    private const val ASSET_FILE = "nawawi_forty_hadith.json"
    private const val SCOPE_KEY = "nawawi_forty"

    @Volatile
    private var cachedList: List<NawawiHadithEntry>? = null

    fun getAllHadiths(context: Context): List<NawawiHadithEntry> {
        cachedList?.let { return it }
        synchronized(this) {
            cachedList?.let { return it }
            val parsed = loadFromAssets(context.applicationContext)
            cachedList = parsed
            return parsed
        }
    }

    fun getOrderedHadiths(context: Context): List<NawawiHadithEntry> {
        val base = getAllHadiths(context)
        return AdhkarCustomizationManager.applyScopeOrder(context, SCOPE_KEY, base) { it.uniqueKey }
    }

    private fun loadFromAssets(appContext: Context): List<NawawiHadithEntry> {
        val list = mutableListOf<NawawiHadithEntry>()
        try {
            appContext.assets.open(ASSET_FILE).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val rawJson = reader.readText()
                    val arr = JSONArray(rawJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            NawawiHadithEntry(
                                number = obj.optInt("number", i + 1),
                                numberLabelAr = obj.optString("numberLabelAr", "الحديث ${i + 1}"),
                                title = obj.optString("title", ""),
                                narrator = obj.optString("narrator", ""),
                                arabicText = obj.optString("arabicText", ""),
                                source = obj.optString("source", ""),
                                vocabulary = obj.optString("vocabulary", ""),
                                benefits = obj.optString("benefits", ""),
                                categoryKey = obj.optString("categoryKey", "foundations")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

@Composable
fun AlNawawiFortyCompleteDialog(
    onDismiss: () -> Unit,
    onOpenFavorites: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val adhkarPrefs = remember { context.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE) }
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }
    val customizationVersion by AdhkarCustomizationManager.versionFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(NawawiCategoryFilter.ALL) }
    var expandedExplanationSet by remember { mutableStateOf(setOf<Int>()) }
    var countRefreshTick by remember { mutableIntStateOf(0) }

    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakingHadithNum by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("ar")
                engine?.setSpeechRate(0.92f)
            }
        }
        ttsInstance = engine
        onDispose {
            try {
                engine?.stop()
                engine?.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    val allOrderedHadiths = remember(customizationVersion, countRefreshTick) {
        AlNawawiFortyRepository.getOrderedHadiths(context)
    }

    val filteredHadiths = remember(allOrderedHadiths, searchQuery, selectedCategory) {
        val normQ = QuranTextMatcher.normalizeArabic(searchQuery.trim())
        allOrderedHadiths.filter { item ->
            val matchesCat = selectedCategory == NawawiCategoryFilter.ALL || item.categoryKey == selectedCategory.key
            val matchesSearch = normQ.isBlank() ||
                item.normalizedSearchBlob.contains(normQ) ||
                item.number.toString() == normQ
            matchesCat && matchesSearch
        }
    }

    val completedTodayCount = remember(allOrderedHadiths, countRefreshTick, customizationVersion) {
        allOrderedHadiths.count { adhkarPrefs.getInt(it.uniqueKey, 0) >= 1 }
    }

    val totalFavoritesCount = remember(customizationVersion, countRefreshTick) {
        AdhkarCustomizationManager.getTotalFavoritesCount(context)
    }

    Dialog(
        onDismissRequest = {
            try {
                ttsInstance?.stop()
            } catch (_: Exception) {}
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .border(2.dp, GoldAccent, RoundedCornerShape(24.dp))
                .testTag("alnawawi_forty_complete_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                                .border(1.dp, GoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📜 الأربعون النووية (٤٢ حديثاً)",
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                color = EmeraldGreen
                            )
                            Text(
                                text = "متن الأحاديث الشريفة مع الراوي وشرح المفردات والفوائد • قراءة اليوم: $completedTodayCount / ${allOrderedHadiths.size}",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF1A110B).copy(alpha = 0.78f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Favorites Quick Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF3E9D2),
                            border = BorderStroke(1.dp, FavoriteHeartCrimson),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenFavorites() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "المفضلة",
                                    tint = FavoriteHeartCrimson,
                                    modifier = Modifier.size(15.dp)
                                )
                                if (totalFavoritesCount > 0) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$totalFavoritesCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FavoriteHeartCrimson
                                    )
                                }
                            }
                        }

                        // Reset Daily Hadith Reading Counter Button
                        OutlinedButton(
                            onClick = {
                                val edit = adhkarPrefs.edit()
                                allOrderedHadiths.forEach { edit.putInt(it.uniqueKey, 0) }
                                edit.apply()
                                countRefreshTick++
                                Toast.makeText(
                                    context,
                                    "تم تصفير عدادات قراءة الأربعين النووية لهذا اليوم",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تصفير",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "تصفير",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    ttsInstance?.stop()
                                } catch (_: Exception) {}
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEBE1C9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث برقم الحديث أو عنوانه أو كلمة من المتن أو الشرح...",
                            fontSize = 12.sp
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
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = GoldBorder,
                        focusedContainerColor = Color(0xFFF3E9D2),
                        unfocusedContainerColor = Color(0xFFF3E9D2)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nawawi_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NawawiCategoryFilter.values().forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) EmeraldGreen else Color(0xFFF3E9D2),
                            border = BorderStroke(1.dp, if (isSelected) EmeraldGreen else GoldBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.labelAr,
                                color = if (isSelected) Color.White else EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hadiths List
                val canReorder = searchQuery.isBlank() && selectedCategory == NawawiCategoryFilter.ALL
                val allKeys = remember(allOrderedHadiths) { allOrderedHadiths.map { it.uniqueKey } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = filteredHadiths,
                        key = { _, item -> item.uniqueKey }
                    ) { index, hadith ->
                        val currentCount = remember(countRefreshTick, customizationVersion, hadith.uniqueKey) {
                            adhkarPrefs.getInt(hadith.uniqueKey, 0)
                        }
                        val isDone = currentCount >= 1
                        val isFav = remember(customizationVersion, countRefreshTick, hadith.uniqueKey) {
                            AdhkarCustomizationManager.isDhikrFavorite(context, hadith.uniqueKey)
                        }
                        val isExplanationExpanded = expandedExplanationSet.contains(hadith.number)
                        val isSpeakingThis = speakingHadithNum == hadith.number

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDone) Color(0xFFEBE1C9) else Color(0xFFF3E9D2)
                            ),
                            border = BorderStroke(
                                width = if (isDone) 1.6.dp else 1.1.dp,
                                color = if (isDone) EmeraldGreen else GoldBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top Badge + Title + Actions (Favorite, Reorder Up/Down, Reset)
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
                                                    uniqueKey = hadith.uniqueKey,
                                                    scopeKey = "nawawi_forty",
                                                    title = "${hadith.numberLabelAr}: ${hadith.title}",
                                                    categoryTitle = "الأربعون النووية • ${hadith.source}",
                                                    arabicText = hadith.arabicText,
                                                    benefit = "${hadith.vocabulary}\n\n• من فوائد الحديث: ${hadith.benefits}",
                                                    targetCount = 1,
                                                    isCustomAdded = false
                                                )
                                                val added = AdhkarCustomizationManager.toggleDhikrFavorite(context, unified)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                countRefreshTick++
                                                Toast.makeText(
                                                    context,
                                                    if (added) "تمت إضافة ${hadith.numberLabelAr} للمفضلة ❤️" else "تمت إزالته من المفضلة",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "مفضلة",
                                                tint = if (isFav) FavoriteHeartCrimson else GoldAccent,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        if (canReorder) {
                                            DhikrOrderArrowsPill(
                                                canMoveUp = index > 0,
                                                canMoveDown = index < filteredHadiths.lastIndex,
                                                onMoveUp = {
                                                    AdhkarCustomizationManager.moveItemInScope(
                                                        context = context,
                                                        scopeKey = "nawawi_forty",
                                                        currentOrderedKeys = allKeys,
                                                        targetKey = hadith.uniqueKey,
                                                        direction = -1
                                                    )
                                                    countRefreshTick++
                                                },
                                                onMoveDown = {
                                                    AdhkarCustomizationManager.moveItemInScope(
                                                        context = context,
                                                        scopeKey = "nawawi_forty",
                                                        currentOrderedKeys = allKeys,
                                                        targetKey = hadith.uniqueKey,
                                                        direction = 1
                                                    )
                                                    countRefreshTick++
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${hadith.numberLabelAr}: ${hadith.title}",
                                                fontFamily = AmiriFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp,
                                                lineHeight = 25.sp,
                                                color = EmeraldGreen
                                            )
                                            Text(
                                                text = "الراوي: ${hadith.narrator} • التخريج: ${hadith.source}",
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GoldAccent
                                            )
                                        }
                                    }

                                    // Single Hadith Reset if read
                                    if (currentCount > 0) {
                                        IconButton(
                                            onClick = {
                                                adhkarPrefs.edit().putInt(hadith.uniqueKey, 0).apply()
                                                countRefreshTick++
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "تصفير عداد الحديث",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Vocalized Hadith Matn Box
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFDF8EC),
                                    border = BorderStroke(0.9.dp, GoldBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = hadith.arabicText,
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        lineHeight = 34.sp,
                                        color = Color(0xFF1A110B),
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    )
                                }

                                // Expandable Vocabulary & Benefits Card (شرح المفردات والفوائد المستنبطة)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFDF8EC).copy(alpha = 0.9f),
                                    border = BorderStroke(0.8.dp, EmeraldGreen.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            expandedExplanationSet = if (isExplanationExpanded) {
                                                expandedExplanationSet - hadith.number
                                            } else {
                                                expandedExplanationSet + hadith.number
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "شرح المفردات والفوائد المستنبطة من الحديث",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = EmeraldGreen
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isExplanationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        AnimatedVisibility(visible = isExplanationExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                HorizontalDivider(color = GoldBorder.copy(alpha = 0.4f))
                                                if (hadith.vocabulary.isNotBlank()) {
                                                    Text(
                                                        text = "📘 معاني المفردات:",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.5.sp,
                                                        color = GoldAccent
                                                    )
                                                    Text(
                                                        text = hadith.vocabulary,
                                                        fontSize = 13.sp,
                                                        lineHeight = 22.sp,
                                                        color = Color(0xFF1A110B),
                                                        textAlign = TextAlign.Right
                                                    )
                                                }
                                                if (hadith.benefits.isNotBlank()) {
                                                    Text(
                                                        text = "🌿 من فوائد وهدايات الحديث:",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.5.sp,
                                                        color = EmeraldGreen
                                                    )
                                                    Text(
                                                        text = hadith.benefits,
                                                        fontSize = 13.sp,
                                                        lineHeight = 22.sp,
                                                        color = Color(0xFF1A110B),
                                                        textAlign = TextAlign.Right
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Action Row: Read/Count Button + Audio Listen + Copy + Share
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val next = currentCount + 1
                                            adhkarPrefs.edit().putInt(hadith.uniqueKey, next).apply()
                                            localStorage.incrementCumulativeAdhkarTasbeeh(1)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            countRefreshTick++
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldGreen,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isDone) "تمت القراءة ($currentCount)" else "تسجيل قراءة الحديث",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // TTS Listen Button
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSpeakingThis) GoldAccent else Color(0xFFFDF8EC),
                                        border = BorderStroke(1.dp, GoldAccent),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val engine = ttsInstance
                                                if (engine != null) {
                                                    if (isSpeakingThis) {
                                                        engine.stop()
                                                        speakingHadithNum = null
                                                    } else {
                                                        speakingHadithNum = hadith.number
                                                        engine.speak(
                                                            "${hadith.numberLabelAr}، ${hadith.title}. ${hadith.arabicText}",
                                                            TextToSpeech.QUEUE_FLUSH,
                                                            null,
                                                            "nawawi_${hadith.number}"
                                                        )
                                                    }
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.Default.VolumeUp,
                                                contentDescription = "استماع للحديث",
                                                tint = if (isSpeakingThis) Color.White else EmeraldGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Copy Button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFDF8EC),
                                        border = BorderStroke(1.dp, GoldBorder),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val fullCopy = buildString {
                                                    append("📜 ${hadith.numberLabelAr}: ${hadith.title}\n\n")
                                                    append("${hadith.arabicText}\n\n")
                                                    append("• الراوي: ${hadith.narrator} | التخريج: ${hadith.source}\n")
                                                    if (hadith.vocabulary.isNotBlank()) {
                                                        append("• معاني المفردات: ${hadith.vocabulary}\n")
                                                    }
                                                    if (hadith.benefits.isNotBlank()) {
                                                        append("• من فوائد الحديث: ${hadith.benefits}")
                                                    }
                                                }
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Hadith", fullCopy))
                                                Toast.makeText(context, "تم نسخ الحديث وشرحه إلى الحافظة", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "نسخ الحديث",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    // Share Button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFDF8EC),
                                        border = BorderStroke(1.dp, GoldAccent),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val shareBody = buildString {
                                                    append("📜 ${hadith.numberLabelAr}: ${hadith.title}\n\n")
                                                    append("${hadith.arabicText}\n\n")
                                                    append("• الراوي: ${hadith.narrator} (${hadith.source})\n")
                                                    if (hadith.benefits.isNotBlank()) {
                                                        append("• من فوائد الحديث: ${hadith.benefits}")
                                                    }
                                                }
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareBody)
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة الحديث الشريف"))
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "مشاركة الحديث",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(17.dp)
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
}
