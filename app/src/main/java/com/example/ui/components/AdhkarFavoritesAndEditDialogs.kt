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
 * Compact Up (▲) and Down (▼) reorder buttons for moving any Dhikr or Chapter up/down in its list.
 */
@Composable
fun DhikrOrderArrowsPill(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFEBE1C9).copy(alpha = 0.75f),
        border = BorderStroke(0.7.dp, GoldBorder),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = canMoveUp) { onMoveUp() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "تحريك لأعلى",
                    tint = if (canMoveUp) EmeraldGreen else Color(0xFF1A110B).copy(alpha = 0.25f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(0.7.dp)
                    .height(14.dp)
                    .background(GoldBorder)
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = canMoveDown) { onMoveDown() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "تحريك لأسفل",
                    tint = if (canMoveDown) EmeraldGreen else Color(0xFF1A110B).copy(alpha = 0.25f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dialog opened either on Long-Press of any Dhikr card (to edit text, change repeat count e.g. 1 -> 3,
 * reorder, delete the dhikr, or add a new dhikr to the same section) OR when clicking "+ إضافة ذكر يدوي جديد".
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
    currentScopeKeys: List<String> = emptyList(),
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
    var insertAtStart by remember { mutableStateOf(false) }

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
                        text = if (mode == "edit") "تعديل الذكر والترتيب والتكرار" else "إضافة ذكر أو دعاء يدوي جديد",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
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

                // Quick Reorder Controls when editing an existing Dhikr
                if (mode == "edit" && initialUniqueKey.isNotBlank() && currentScopeKeys.size > 1) {
                    Text(
                        text = "تغيير ترتيب الذكر في القائمة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = EmeraldGreen
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                AdhkarCustomizationManager.moveItemInScope(
                                    context,
                                    scopeKey,
                                    currentScopeKeys,
                                    initialUniqueKey,
                                    -1
                                )
                                Toast.makeText(context, "تم تقديم الذكر خطوة لأعلى ⬆️", Toast.LENGTH_SHORT).show()
                                onSaved()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldGreen),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("لأعلى", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                AdhkarCustomizationManager.moveItemInScope(
                                    context,
                                    scopeKey,
                                    currentScopeKeys,
                                    initialUniqueKey,
                                    +1
                                )
                                Toast.makeText(context, "تم تأخير الذكر خطوة لأسفل ⬇️", Toast.LENGTH_SHORT).show()
                                onSaved()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldGreen),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("لأسفل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                AdhkarCustomizationManager.moveItemToEdgeInScope(
                                    context,
                                    scopeKey,
                                    currentScopeKeys,
                                    initialUniqueKey,
                                    toStart = true
                                )
                                Toast.makeText(context, "تم نقل الذكر إلى بداية القائمة", Toast.LENGTH_SHORT).show()
                                onSaved()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccent),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("الأول", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                AdhkarCustomizationManager.moveItemToEdgeInScope(
                                    context,
                                    scopeKey,
                                    currentScopeKeys,
                                    initialUniqueKey,
                                    toStart = false
                                )
                                Toast.makeText(context, "تم نقل الذكر إلى نهاية القائمة", Toast.LENGTH_SHORT).show()
                                onSaved()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccent),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("الأخير", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // Placement Choice when Adding a New Dhikr (End of list vs Start of list)
                if (mode == "add") {
                    Text(
                        text = "مكان إضافة الذكر الجديد:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = EmeraldGreen
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!insertAtStart) EmeraldGreen else Color(0xFFF3E9D2),
                            border = BorderStroke(1.dp, if (!insertAtStart) EmeraldGreen else GoldBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { insertAtStart = false }
                        ) {
                            Text(
                                text = "⬇️ في نهاية القائمة",
                                color = if (!insertAtStart) Color.White else Color(0xFF1A110B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (insertAtStart) EmeraldGreen else Color(0xFFF3E9D2),
                            border = BorderStroke(1.dp, if (insertAtStart) EmeraldGreen else GoldBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { insertAtStart = true }
                        ) {
                            Text(
                                text = "⬆️ في بداية القائمة",
                                color = if (insertAtStart) Color.White else Color(0xFF1A110B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }

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
                            alsoAddToFavorites = alsoFavorite || scopeKey == "favorites",
                            insertAtStart = insertAtStart
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
 * - Viewing & reordering all favorited Hisn Al-Muslim Chapters (أبواب مفضلة) & opening any chapter directly.
 * - Viewing & reordering all favorited Adhkar from across the entire app (Morning, Evening, Hisn Al-Muslim, Tasbeeh, Manual).
 * - Adding any custom manual Dhikr/Dua (read online, YouTube, TikTok, etc.) with custom repeat count and position.
 * - Long-pressing any Dhikr card to edit its text/repeat count/order or delete it.
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
        val orderedIds = AdhkarCustomizationManager.getOrderedFavoriteChapterIds(context)
        val byId = allHisnChapters.associateBy { it.id }
        orderedIds.mapNotNull { byId[it] }
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
                // 1. Top Ornate Header Bar (Clean, Non-Wrapping Layout)
                Surface(
                    color = RoyalNavyDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(FavoriteHeartCrimson.copy(alpha = 0.14f))
                                        .border(1.dp, FavoriteHeartCrimson, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = FavoriteHeartCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الأذكار والأدعية المفضلة",
                                        fontFamily = AmiriFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = EmeraldGreen,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "تصفير يومي 12:00 ص • رتب الأذكار بأسهم ▲▼",
                                        fontSize = 10.5.sp,
                                        color = darkInkText.copy(alpha = 0.78f),
                                        maxLines = 1
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
                                        .height(32.dp)
                                        .testTag("reset_all_favorites_header_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تصفير المفضلة",
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "تصفير الكل",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق المفضلة",
                                        tint = EmeraldGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Prominent Manual Add Dhikr Button ("+ إضافة ذكر يدوي جديد")
                        Button(
                            onClick = { showAddManualDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ إضافة ذكر أو دعاء يدوي جديد (مع تحديد التكرار)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📚 الأبواب المفضلة (${favoriteChapters.size}):",
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp,
                                    color = EmeraldGreen,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
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
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.8.dp, GoldAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تصفير الأبواب",
                                        fontSize = 10.5.sp,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        items(
                            count = favoriteChapters.size,
                            key = { idx -> "fav_ch_${favoriteChapters[idx].id}_$dailyResetTrigger" }
                        ) { chIndex ->
                            val chapter = favoriteChapters[chIndex]
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
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
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
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "إزالة الباب من المفضلة",
                                                tint = FavoriteHeartCrimson,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chapter.title,
                                                fontFamily = AmiriFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = darkInkText
                                            )
                                            Text(
                                                text = "الباب رقم ${chapter.id} • إنجاز $completedInChap / ${chapter.duas.size}",
                                                fontSize = 10.5.sp,
                                                color = EmeraldGreen,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (favoriteChapters.size > 1) {
                                            DhikrOrderArrowsPill(
                                                canMoveUp = chIndex > 0,
                                                canMoveDown = chIndex < favoriteChapters.lastIndex,
                                                onMoveUp = {
                                                    AdhkarCustomizationManager.moveFavoriteChapter(context, chapter.id, -1)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onMoveDown = {
                                                    AdhkarCustomizationManager.moveFavoriteChapter(context, chapter.id, +1)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            )
                                        }

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
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "تصفير الباب",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = EmeraldGreen
                                        ) {
                                            Text(
                                                text = "فتح",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
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

                    // Favorite Individual & Manual Adhkar Section (Fixed non-wrapping header!)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📿 الأذكار المفضلة واليدوية (${favoriteItems.size}):",
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp,
                                color = EmeraldGreen,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            if (favoriteItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val editor = prefs.edit()
                                        favoriteItems.forEach { item ->
                                            counts[item.uniqueKey] = 0
                                            editor.putInt("fav_cnt_${item.uniqueKey}", 0)
                                            editor.putInt(item.uniqueKey, 0)
                                        }
                                        editor.apply()
                                        Toast.makeText(context, "تم تصفير عدادات الأذكار المفضلة", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.8.dp, GoldAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تصفير الأذكار",
                                        fontSize = 10.5.sp,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
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
                                        text = "يمكنك الضغط على علامة القلب (❤️) بجانب أي ذكر أو باب في التطبيق لإضافته هنا، أو الضغط على زر «+ إضافة ذكر أو دعاء يدوي جديد» بالأعلى لإضافة أي ذكر قرأته أو سمعته مع تحديد عدد مرات تكراره وترتيبه.",
                                        fontSize = 12.5.sp,
                                        color = darkInkText.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    val allFavKeys = favoriteItems.map { it.uniqueKey }

                    items(
                        count = favoriteItems.size,
                        key = { idx -> "fav_item_${favoriteItems[idx].uniqueKey}" }
                    ) { itemIdx ->
                        val item = favoriteItems[itemIdx]
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
                                // Top Row: Heart Button + Title/Category + Up/Down Reorder + Edit Button + Target Repeat Badge
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
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "إزالة من المفضلة",
                                                tint = FavoriteHeartCrimson,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title.ifBlank { item.categoryTitle },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = EmeraldGreen,
                                                maxLines = 1
                                            )
                                            if (item.categoryTitle.isNotBlank() && item.categoryTitle != item.title) {
                                                Text(
                                                    text = item.categoryTitle,
                                                    fontSize = 10.5.sp,
                                                    color = GoldAccent,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (favoriteItems.size > 1) {
                                            DhikrOrderArrowsPill(
                                                canMoveUp = itemIdx > 0,
                                                canMoveDown = itemIdx < favoriteItems.lastIndex,
                                                onMoveUp = {
                                                    AdhkarCustomizationManager.moveItemInScope(
                                                        context,
                                                        "favorites",
                                                        allFavKeys,
                                                        item.uniqueKey,
                                                        -1
                                                    )
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onMoveDown = {
                                                    AdhkarCustomizationManager.moveItemInScope(
                                                        context,
                                                        "favorites",
                                                        allFavKeys,
                                                        item.uniqueKey,
                                                        +1
                                                    )
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            )
                                        }

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
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل الذكر والتكرار",
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "تعديل",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = darkInkText,
                                                    maxLines = 1,
                                                    softWrap = false
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
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                            val shareText = "${item.title}\n\n${item.arabicText}"
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
            currentScopeKeys = favoriteItems.map { it.uniqueKey },
            onDismiss = { showAddManualDialog = false }
        )
    }

    editingItem?.let { target ->
        AddOrEditDhikrDialog(
            isEditMode = true,
            scopeKey = "favorites",
            categoryTitle = target.categoryTitle.ifBlank { "الأذكار المفضلة" },
            initialUniqueKey = target.uniqueKey,
            initialTitle = target.title,
            initialArabicText = target.arabicText,
            initialBenefit = target.benefit,
            initialTargetCount = target.targetCount,
            currentScopeKeys = favoriteItems.map { it.uniqueKey },
            onDismiss = { editingItem = null }
        )
    }
}
