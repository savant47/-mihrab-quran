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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AdhkarCustomizationManager
import com.example.data.local.LocalKhatmaStorage
import com.example.data.local.UnifiedDhikrItem
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import kotlinx.coroutines.delay

val FavoriteHeartCrimson = Color(0xFFC62828)

fun formatRepeatCountArabic(count: Int): String {
    return when (count) {
        1 -> "مرة واحدة"
        2 -> "مرتان"
        in 3..10 -> "$count مرات"
        else -> "$count مرة"
    }
}

/**
 * Dialog opened either on Long-Press of any Dhikr card (to edit text, change repeat count e.g. 1 -> 3,
 * delete the dhikr, or add a new dhikr to the same section) OR when clicking "+ إضافة ذكر يدوي جديد".
 */
@Composable
fun AddOrEditDhikrDialog(
    isEditMode: Boolean,
    scopeKey: String,
    categoryTitle: String,
    initialUniqueKey: String = "",
    initialTitle: String = "",
    initialArabicText: String = "",
    initialBenefit: String = "",
    initialTargetCount: Int = 3,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(if (isEditMode) "edit" else "add") }

    var titleInput by remember(mode) {
        mutableStateOf(if (mode == "edit") initialTitle else "")
    }
    var textInput by remember(mode) {
        mutableStateOf(if (mode == "edit") initialArabicText else "")
    }
    var benefitInput by remember(mode) {
        mutableStateOf(if (mode == "edit") initialBenefit else "")
    }
    var countInput by remember(mode) {
        mutableStateOf((if (mode == "edit") initialTargetCount else 3).coerceAtLeast(1).toString())
    }
    var alsoFavorite by remember { mutableStateOf(true) }

    val quickCounts = listOf(1, 3, 7, 10, 33, 100)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFDF8EC),
        titleContentColor = EmeraldGreen,
        textContentColor = Color(0xFF1A110B),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (mode == "edit") "تعديل الذكر والتكرار أو الحذف" else "إضافة ذكر أو دعاء يدوي جديد",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = EmeraldGreen
                    )
                }

                // When opened via Long-Press on an existing Dhikr, let user switch between "تعديل الذكر الحالي" and "+ إضافة ذكر جديد"
                if (isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (mode == "edit") EmeraldGreen else Color(0xFFEBE1C9),
                            border = BorderStroke(1.dp, if (mode == "edit") EmeraldGreen else GoldBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { mode = "edit" }
                        ) {
                            Text(
                                text = "✏️ تعديل الذكر الحالي",
                                color = if (mode == "edit") Color.White else Color(0xFF1A110B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (mode == "add") EmeraldGreen else Color(0xFFEBE1C9),
                            border = BorderStroke(1.dp, if (mode == "add") EmeraldGreen else GoldBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { mode = "add" }
                        ) {
                            Text(
                                text = "➕ إضافة ذكر جديد",
                                color = if (mode == "add") Color.White else Color(0xFF1A110B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "القسم: $categoryTitle",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )

                // 1. Repeat Count Section (Prominent at top so user can quickly change 1 -> 3 times)
                Text(
                    text = "عدد مرات التكرار:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = EmeraldGreen
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val selectedInt = countInput.toIntOrNull() ?: 1
                    quickCounts.forEach { preset ->
                        val isSelected = selectedInt == preset
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldGreen else Color(0xFFF3E9D2),
                            border = BorderStroke(1.dp, if (isSelected) EmeraldGreen else GoldBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { countInput = preset.toString() }
                        ) {
                            Text(
                                text = formatRepeatCountArabic(preset),
                                color = if (isSelected) Color.White else Color(0xFF1A110B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = countInput,
                    onValueChange = { countInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("أو اكتب عدد مرات التكرار يدوياً") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dhikr_repeat_count_input")
                )

                // 2. Title / Source Input
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("عنوان الذكر أو المصدر (اختياري)") },
                    placeholder = { Text("مثال: دعاء من الفجر، ذكر مأثور، دعاء سمعته...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dhikr_title_input")
                )

                // 3. Full Arabic Dhikr Text Input
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("نص الذكر أو الدعاء") },
                    placeholder = { Text("اكتب أو الصق نص الذكر أو الدعاء هنا...") },
                    minLines = 3,
                    maxLines = 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dhikr_arabic_text_input")
                )

                // 4. Optional Benefit / Note Input
                OutlinedTextField(
                    value = benefitInput,
                    onValueChange = { benefitInput = it },
                    label = { Text("الفضل أو الملاحظة (اختياري)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode == "add" && scopeKey != "favorites") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alsoFavorite = !alsoFavorite }
                    ) {
                        Checkbox(
                            checked = alsoFavorite,
                            onCheckedChange = { alsoFavorite = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                        )
                        Text(
                            text = "إضافة هذا الذكر أيضاً إلى «الأذكار المفضلة ❤️»",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A110B)
                        )
                    }
                }

                // 5. Delete Dhikr Button (when in Edit mode)
                if (mode == "edit" && initialUniqueKey.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            AdhkarCustomizationManager.deleteDhikr(context, initialUniqueKey)
                            Toast.makeText(context, "تم حذف الذكر بنجاح", Toast.LENGTH_SHORT).show()
                            onSaved()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_dhikr_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFC62828)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFC62828).copy(alpha = 0.08f),
                            contentColor = Color(0xFFC62828)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الذكر",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حذف هذا الذكر من القائمة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanText = textInput.trim()
                    if (cleanText.isEmpty()) {
                        Toast.makeText(context, "يرجى كتابة نص الذكر أولاً", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val targetRepeat = (countInput.toIntOrNull() ?: 1).coerceIn(1, 10000)
                    if (mode == "edit" && initialUniqueKey.isNotBlank()) {
                        AdhkarCustomizationManager.saveOverride(
                            context = context,
                            uniqueKey = initialUniqueKey,
                            title = titleInput.trim().ifBlank { initialTitle },
                            arabicText = cleanText,
                            benefit = benefitInput.trim(),
                            targetCount = targetRepeat
                        )
                        Toast.makeText(
                            context,
                            "تم حفظ تعديل الذكر وتحديث التكرار إلى (${formatRepeatCountArabic(targetRepeat)})",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        AdhkarCustomizationManager.addCustomDhikr(
                            context = context,
                            scopeKey = scopeKey,
                            title = titleInput.trim().ifBlank { "ذكر مضاف" },
                            categoryTitle = categoryTitle,
                            arabicText = cleanText,
                            benefit = benefitInput.trim(),
                            targetCount = targetRepeat,
                            alsoAddToFavorites = alsoFavorite || scopeKey == "favorites"
                        )
                        Toast.makeText(
                            context,
                            "تمت إضافة الذكر الجديد بنجاح (${formatRepeatCountArabic(targetRepeat)})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    onSaved()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_dhikr_edit_button")
            ) {
                Text(
                    text = if (mode == "edit") "حفظ التعديلات" else "إضافة الذكر",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = GoldAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Full-Screen Dialog for "الأذكار والأدعية المفضلة ❤️"
 * Accessible directly from the outside button below Hisn Al-Muslim Index, or from the heart icons.
 * Supports:
 * - Viewing all favorited Hisn Al-Muslim Chapters (أبواب مفضلة) & opening any chapter directly.
 * - Viewing all favorited Adhkar from across the entire app (Morning, Evening, Hisn Al-Muslim, Tasbeeh, Manual).
 * - Adding any custom manual Dhikr/Dua (read online, YouTube, TikTok, etc.) with custom repeat count.
 * - Long-pressing any Dhikr card to edit its text/repeat count or delete it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteAdhkarDialog(
    onOpenHisnChapter: (HisnCategoryChapter) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE) }
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }
    val customizationVersion by AdhkarCustomizationManager.versionFlow.collectAsState()
    var dailyResetTrigger by remember { mutableStateOf(0) }

    // Automatic Daily Reset at 12:00 AM midnight while FavoriteAdhkarDialog is open
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

    val allHisnChapters = remember(customizationVersion, dailyResetTrigger) {
        HisnAlMuslimRepository.getEffectiveChapters(context)
    }

    val favoriteChapters = remember(customizationVersion, allHisnChapters) {
        val favIds = AdhkarCustomizationManager.getFavoriteChapterIds(context)
        allHisnChapters.filter { favIds.contains(it.id) }
    }

    val favoriteItems = remember(customizationVersion, dailyResetTrigger) {
        AdhkarCustomizationManager.getFavoriteItems(context)
    }

    var showAddManualDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<UnifiedDhikrItem?>(null) }

    val counts = remember(favoriteItems, dailyResetTrigger) {
        ensureDailyAdhkarResetIfNewDay(prefs, context)
        mutableStateMapOf<String, Int>().apply {
            favoriteItems.forEach { item ->
                this[item.uniqueKey] = prefs.getInt("fav_cnt_${item.uniqueKey}", 0)
            }
        }
    }

    val unifiedLightBg = Color(0xFFFDF8EC)
    val unifiedLightCard = Color(0xFFF5EFE0)
    val darkInkText = Color(0xFF1A110B)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("favorite_adhkar_dialog"),
            color = unifiedLightBg
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
                            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(FavoriteHeartCrimson.copy(alpha = 0.14f))
                                        .border(1.dp, FavoriteHeartCrimson, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = FavoriteHeartCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الأذكار والأدعية المفضلة",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 21.sp,
                                        color = EmeraldGreen
                                    )
                                    Text(
                                        text = "تصفير تلقائي يومياً 12:00 ص • اضغط مطولاً للتعديل أو الحذف",
                                        fontSize = 11.sp,
                                        color = darkInkText.copy(alpha = 0.78f)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Manual Reset All Favorites Button in Header
                                OutlinedButton(
                                    onClick = {
                                        val editor = prefs.edit()
                                        favoriteItems.forEach { item ->
                                            counts[item.uniqueKey] = 0
                                            editor.putInt("fav_cnt_${item.uniqueKey}", 0)
                                            editor.putInt(item.uniqueKey, 0)
                                        }
                                        favoriteChapters.forEach { ch ->
                                            ch.duas.forEach { d ->
                                                editor.putInt(d.uniqueKey, 0)
                                            }
                                        }
                                        editor.apply()
                                        dailyResetTrigger++
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(
                                            context,
                                            "تم تصفير وإعادة ضبط عدادات كافة الأذكار والأبواب المفضلة",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("reset_all_favorites_header_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تصفير المفضلة",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("تصفير الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق المفضلة",
                                        tint = EmeraldGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Prominent Manual Add Dhikr Button ("+ إضافة ذكر يدوي جديد")
                        Button(
                            onClick = { showAddManualDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("add_manual_dhikr_in_favorites_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ إضافة ذكر أو دعاء يدوي جديد (مع تحديد التكرار)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 2. Main List of Favorite Chapters + Favorite & Custom Adhkar
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Favorite Chapters Section
                    if (favoriteChapters.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📚 الأبواب المفضلة من حصن المسلم (${favoriteChapters.size}):",
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = EmeraldGreen
                                )
                                TextButton(
                                    onClick = {
                                        val editor = prefs.edit()
                                        favoriteChapters.forEach { ch ->
                                            ch.duas.forEach { d ->
                                                editor.putInt(d.uniqueKey, 0)
                                            }
                                        }
                                        editor.apply()
                                        dailyResetTrigger++
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, "تم تصفير عدادات الأبواب المفضلة", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تصفير الأبواب", fontSize = 11.5.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        items(favoriteChapters, key = { "fav_ch_${it.id}_$dailyResetTrigger" }) { chapter ->
                            val completedInChap = chapter.duas.count { d ->
                                prefs.getInt(d.uniqueKey, 0) >= d.targetCount
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = unifiedLightCard,
                                border = BorderStroke(1.2.dp, GoldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onOpenHisnChapter(chapter)
                                    }
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
                                        IconButton(
                                            onClick = {
                                                AdhkarCustomizationManager.toggleChapterFavorite(context, chapter.id)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "إزالة الباب من المفضلة",
                                                tint = FavoriteHeartCrimson,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chapter.title,
                                                fontFamily = AmiriFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = darkInkText
                                            )
                                            Text(
                                                text = "الباب رقم ${chapter.id} • إنجاز $completedInChap / ${chapter.duas.size} ذكر ودعاء",
                                                fontSize = 11.sp,
                                                color = EmeraldGreen,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val editor = prefs.edit()
                                                chapter.duas.forEach { d ->
                                                    editor.putInt(d.uniqueKey, 0)
                                                }
                                                editor.apply()
                                                dailyResetTrigger++
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(context, "تم تصفير باب «${chapter.title}»", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "تصفير الباب",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = EmeraldGreen
                                        ) {
                                            Text(
                                                text = "فتح الباب",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = GoldBorder)
                        }
                    }

                    // Favorite Individual & Manual Adhkar Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📿 الأذكار والأدعية المفضلة واليدوية (${favoriteItems.size}):",
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = EmeraldGreen
                            )
                            if (favoriteItems.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        val editor = prefs.edit()
                                        favoriteItems.forEach { item ->
                                            counts[item.uniqueKey] = 0
                                            editor.putInt("fav_cnt_${item.uniqueKey}", 0)
                                            editor.putInt(item.uniqueKey, 0)
                                        }
                                        editor.apply()
                                        Toast.makeText(context, "تم تصفير عدادات المفضلة", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تصفير الكل", fontSize = 11.5.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (favoriteItems.isEmpty() && favoriteChapters.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = unifiedLightCard,
                                border = BorderStroke(1.dp, GoldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = FavoriteHeartCrimson,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Text(
                                        text = "قائمة الأذكار المفضلة فارغة حالياً",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = EmeraldGreen,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "يمكنك الضغط على علامة القلب (❤️) بجانب أي ذكر أو باب في التطبيق لإضافته هنا، أو الضغط على زر «+ إضافة ذكر أو دعاء يدوي جديد» بالأعلى لإضافة أي ذكر قرأته أو سمعته مع تحديد عدد مرات تكراره.",
                                        fontSize = 12.5.sp,
                                        color = darkInkText.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    items(favoriteItems, key = { "fav_item_${it.uniqueKey}" }) { item ->
                        val currentCount = counts[item.uniqueKey] ?: 0
                        val isCompleted = currentCount >= item.targetCount

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (currentCount < item.targetCount) {
                                            val next = currentCount + 1
                                            counts[item.uniqueKey] = next
                                            prefs.edit().putInt("fav_cnt_${item.uniqueKey}", next).apply()
                                            localStorage.incrementCumulativeAdhkarTasbeeh(1)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        editingItem = item
                                    }
                                )
                                .border(
                                    width = if (isCompleted) 1.8.dp else 1.dp,
                                    color = if (isCompleted) EmeraldGreen else GoldBorder,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) EmeraldGreen.copy(alpha = 0.10f) else unifiedLightCard
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                // Top Row: Heart Button + Title/Category + Edit Button + Target Repeat Badge
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
                                                AdhkarCustomizationManager.toggleDhikrFavorite(context, item)
                                                Toast.makeText(context, "تمت إزالة الذكر من المفضلة", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "إزالة من المفضلة",
                                                tint = FavoriteHeartCrimson,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title.ifBlank { item.categoryTitle },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = EmeraldGreen
                                            )
                                            if (item.categoryTitle.isNotBlank() && item.categoryTitle != item.title) {
                                                Text(
                                                    text = item.categoryTitle,
                                                    fontSize = 11.sp,
                                                    color = GoldAccent,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Quick Edit Icon (also triggered by Long-Press)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEBE1C9),
                                            border = BorderStroke(0.8.dp, GoldBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { editingItem = item }
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
                                                    color = darkInkText
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isCompleted) GoldAccent else EmeraldGreen
                                        ) {
                                            Text(
                                                text = formatRepeatCountArabic(item.targetCount),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Arabic Dhikr Text
                                Text(
                                    text = item.arabicText,
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    lineHeight = 35.sp,
                                    color = darkInkText,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (item.benefit.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "الفضل: ${item.benefit}",
                                        fontSize = 11.5.sp,
                                        color = darkInkText.copy(alpha = 0.72f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bottom Counter + Copy + Share + Reset Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (currentCount < item.targetCount) {
                                                val next = currentCount + 1
                                                counts[item.uniqueKey] = next
                                                prefs.edit().putInt("fav_cnt_${item.uniqueKey}", next).apply()
                                                localStorage.incrementCumulativeAdhkarTasbeeh(1)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCompleted) GoldAccent else EmeraldGreen,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 46.dp)
                                    ) {
                                        Text(
                                            text = if (isCompleted) "تمت القراءة ✓ ($currentCount/${item.targetCount})"
                                            else "اضغط للعد ($currentCount / ${item.targetCount})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }

                                    OutlinedIconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("ذكر مفضل", item.arabicText))
                                            Toast.makeText(context, "تم نسخ الذكر إلى الحافظة", Toast.LENGTH_SHORT).show()
                                        },
                                        border = BorderStroke(1.dp, GoldBorder),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "نسخ",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    OutlinedIconButton(
                                        onClick = {
                                            val shareText = "${item.title}\n\n${item.arabicText}\n\n— تطبيق محرابي"
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الذكر"))
                                        },
                                        border = BorderStroke(1.dp, GoldAccent),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "مشاركة",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (currentCount > 0) {
                                        OutlinedIconButton(
                                            onClick = {
                                                counts[item.uniqueKey] = 0
                                                prefs.edit().putInt("fav_cnt_${item.uniqueKey}", 0).apply()
                                            },
                                            border = BorderStroke(1.dp, GoldBorder),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "تصفير",
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
    }

    if (showAddManualDialog) {
        AddOrEditDhikrDialog(
            isEditMode = false,
            scopeKey = "favorites",
            categoryTitle = "الأذكار والأدعية المفضلة",
            onDismiss = { showAddManualDialog = false }
        )
    }

    editingItem?.let { target ->
        AddOrEditDhikrDialog(
            isEditMode = true,
            scopeKey = target.scopeKey,
            categoryTitle = target.categoryTitle.ifBlank { "الأذكار المفضلة" },
            initialUniqueKey = target.uniqueKey,
            initialTitle = target.title,
            initialArabicText = target.arabicText,
            initialBenefit = target.benefit,
            initialTargetCount = target.targetCount,
            onDismiss = { editingItem = null }
        )
    }
}
