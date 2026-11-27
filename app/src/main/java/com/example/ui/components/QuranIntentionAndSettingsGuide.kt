package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.alarm.AdhanAlertMode
import com.example.alarm.MihrabiAlarmManager
import com.example.alarm.PrayerTimesAndAdhanManager
import com.example.data.SpiritualMessageItem
import com.example.data.local.AdhkarCustomizationManager
import com.example.data.local.LocalKhatmaStorage
import com.example.data.local.UnifiedDhikrItem
import com.example.data.model.ReadingLog
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val QURAN_INTENTION_DUA_UNIQUE_KEY = "quran_intention_renewal_comprehensive_dua"

val QURAN_INTENTION_DUA_TITLE = "🤲 الدعاء الجامع لتجديد النوايا وبركة القرآن الكريم"

val QURAN_INTENTION_DUA_PARAGRAPHS = listOf(
    "«اللَّهُمَّ إِنِّي عَبْدُكَ، ابْنُ عَبْدِكَ، ابْنُ أَمَتِكَ، نَاصِيَتِي بِيَدِكَ، مَاضٍ فِيَّ حُكْمُكَ، عَدْلٌ فِيَّ قَضَاؤُكَ؛ أَسْأَلُكَ بِكُلِّ اسْمٍ هُوَ لَكَ، سَمَّيْتَ بِهِ نَفْسَكَ، أَوْ أَنْزَلْتَهُ فِي كِتَابِكَ، أَوْ عَلَّمْتَهُ أَحَدًا مِنْ خَلْقِكَ، أَوِ اسْتَأْثَرْتَ بِهِ فِي عِلْمِ الْغَيْبِ عِنْدَكَ، أَنْ تَتَقَبَّلَ مِنِّي تِلَاوَتِي خَالِصَةً لِوَجْهِكَ الْكَرِيمِ.",
    "اللَّهُمَّ إِنِّي أَنْوِي بِقِرَاءَتِي لِكِتَابِكَ امْتِثَالَ أَمْرِكَ، وَمُنَاجَاتَكَ، وَنَيْلَ مَحَبَّتِكَ وَمَحَبَّةِ رَسُولِكَ ﷺ، وَتَحْصِيلَ الْحَسَنَاتِ الْمُضَاعَفَةِ، وَالْبَرَاءَةَ مِنْ هَجْرِ الْقُرْآنِ.",
    "اللَّهُمَّ اجْعَلْهُ رَبِيعَ قَلْبِي، وَنُورَ صَدْرِي، وَجَلَاءَ حُزْنِي، وَذَهَابَ هَمِّي، وَشِفَاءً لِبَدَنِي وَرُوحِي، وَسَكِينَةً وَحِصْنًا لِبَيْتِي وَأَهْلِي.",
    "اللَّهُمَّ ارْفَعْنِي بِهِ فِي الدُّنْيَا وَالْآخِرَةِ، وَاجْعَلْنِي بِهِ مِنْ أَهْلِكَ وَخَاصَّتِكَ، وَاحْشُرْنِي مَعَ السَّفَرَةِ الْكِرَامِ الْبَرَرَةِ، وَقِنِي بِهِ أَرْذَلَ الْعُمُرِ وَسُوءَ الْكِبَرِ، وَاجْعَلْهُ شَفِيعًا لِي يَوْمَ أَلْقَاكَ، وَسَبَبًا لِارْتِقَائِي فِي دَرَجَاتِ الْجَنَّةِ.",
    "اللَّهُمَّ وَاجْعَلْ نُورَ تِلَاوَتِي وَثَوَابَهَا بَرَكَةً وَرَحْمَةً وَنُورًا وَشِفَاءً وَعَافِيَةً لِأَبِي وَلِأُمِّي، وَأَلْبِسْهُمَا بِهِ حُلَّتَيِ الْكَرَامَةِ وَتَاجَ الْوَقَارِ يَوْمَ الْقِيَامَةِ، وَأَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي، وَدُنْيَايَ الَّتِي فِيهَا مَعَاشِي، وَآخِرَتِي الَّتِي فِيهَا مَعَادِي، يَا أَرْحَمَ الرَّاحِمِينَ»"
)

val QURAN_INTENTION_DUA_FULL_TEXT: String = QURAN_INTENTION_DUA_PARAGRAPHS.joinToString("\n\n")

/**
 * Prominent button placed at the top of the Awrad tab ("الورد والتدبر") to open the
 * Comprehensive Quran Intention Renewal Dua Dialog.
 */
@Composable
fun QuranIntentionRenewalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF3E9D2),
        border = BorderStroke(1.3.dp, GoldAccent),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("quran_intention_renewal_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFDF8EC), Color(0xFFF3E9D2))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen)
                        .border(1.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤲",
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "دعاء تجديد نوايا قراءة القرآن الكريم",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp,
                    lineHeight = 22.sp,
                    color = EmeraldGreen,
                    softWrap = true
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EmeraldGreen,
                border = BorderStroke(1.dp, GoldAccent)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "فتح الدعاء",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Ornate Mushaf-styled dialog displaying the full vocalized text of
 * «الدعاء الجامع لتجديد النوايا وبركة القرآن الكريم».
 */
@Composable
fun QuranIntentionRenewalDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }
    val customizationVersion by AdhkarCustomizationManager.versionFlow.collectAsState()

    val isFav = remember(customizationVersion) {
        AdhkarCustomizationManager.isDhikrFavorite(context, QURAN_INTENTION_DUA_UNIQUE_KEY)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 690.dp)
                .border(2.dp, GoldAccent, RoundedCornerShape(24.dp))
                .testTag("quran_intention_renewal_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val unified = UnifiedDhikrItem(
                                uniqueKey = QURAN_INTENTION_DUA_UNIQUE_KEY,
                                scopeKey = "manual_fav",
                                title = "دعاء تجديد نوايا قراءة القرآن الكريم",
                                categoryTitle = "أدعية القرآن الكريم",
                                arabicText = QURAN_INTENTION_DUA_FULL_TEXT,
                                benefit = "الدعاء الجامع لتجديد النوايا وبركة القرآن الكريم",
                                targetCount = 1,
                                isCustomAdded = false
                            )
                            val added = AdhkarCustomizationManager.toggleDhikrFavorite(context, unified)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(
                                context,
                                if (added) "تمت إضافة الدعاء إلى الأذكار المفضلة ❤️" else "تمت إزالة الدعاء من المفضلة",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "إضافة للمفضلة",
                            tint = if (isFav) FavoriteHeartCrimson else GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = QURAN_INTENTION_DUA_TITLE,
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.5.sp,
                        lineHeight = 28.sp,
                        color = EmeraldGreen,
                        textAlign = TextAlign.Center,
                        softWrap = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    IconButton(
                        onClick = onDismiss,
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

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Vocalized Dua Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFDF8EC), Color(0xFFF3E9D2), Color(0xFFFDF8EC))
                            )
                        )
                        .border(1.4.dp, GoldAccent, RoundedCornerShape(18.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "۞ بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۞",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = GoldAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    QURAN_INTENTION_DUA_PARAGRAPHS.forEachIndexed { index, paragraph ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFDF8EC).copy(alpha = 0.85f),
                            border = BorderStroke(0.8.dp, GoldBorder)
                        ) {
                            Text(
                                text = paragraph,
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.5.sp,
                                lineHeight = 34.sp,
                                color = Color(0xFF1A110B),
                                textAlign = TextAlign.Right,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons: عقدت النية ✓ | نسخ | مشاركة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            localStorage.incrementCumulativeAdhkarTasbeeh(1)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(
                                context,
                                "تقبّل الله منك تلاوتك ونواياك الصالحة وجعل القرآن ربيع قلبك 🤲",
                                Toast.LENGTH_SHORT
                            ).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp)
                            .testTag("confirm_intention_dua_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "عقدت النية • آمين",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val payload = "$QURAN_INTENTION_DUA_TITLE\n\n$QURAN_INTENTION_DUA_FULL_TEXT"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Quran Intention Dua", payload))
                            Toast.makeText(context, "تم نسخ الدعاء إلى الحافظة", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.85f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ الدعاء",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "نسخ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val sharePayload = "$QURAN_INTENTION_DUA_TITLE\n\n$QURAN_INTENTION_DUA_FULL_TEXT"
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, sharePayload)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الدعاء"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة الدعاء",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مشاركة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Comprehensive Settings & User Guide Tab ("⚙️ إعدادات") replacing the old "السجل" tab.
 * Organizes all app settings, interactive user guide, backup & restore, Adhan settings,
 * reading logs, and spiritual messages into clean expandable/quick-open cards.
 */
@Composable
fun ComprehensiveSettingsAndGuideTab(
    cumulativeAdhkarTasbeehTotal: Long,
    lastReadSurah: String,
    lastReadAyah: Int,
    lastReadPage: Int,
    readingLogs: List<ReadingLog>,
    spiritualMessages: List<SpiritualMessageItem>,
    onResumeReadingAtLastStop: () -> Unit,
    onOpenBackupDialog: () -> Unit,
    onOpenSpiritualMessage: (SpiritualMessageItem) -> Unit,
    onGenerateNewSpiritualMessage: () -> Unit,
    onOpenPrayerAndQiblaTab: () -> Unit,
    onOpenIntentionDuaDialog: () -> Unit
) {
    val context = LocalContext.current
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }
    val restoreVersion by localStorage.backupRestoreVersionFlow.collectAsState()

    var expandedSection by remember { mutableStateOf<String?>(null) }
    var showAdhanSettingsModal by remember { mutableStateOf(false) }
    var isPlayingAdhanPreview by remember { mutableStateOf(false) }

    var adhanSettings by remember(restoreVersion) {
        mutableStateOf(PrayerTimesAndAdhanManager.loadSettings(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isPlayingAdhanPreview) {
                MihrabiAlarmManager.stopRingtone()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Title Header
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background( Color(0xFFEBE1C9))
                        .border(1.2.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚙️ الإعدادات ودليل الاستخدام والسجلات",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = EmeraldGreen
                    )
                    Text(
                        text = "دليل استخدام التطبيق • النسخ الاحتياطي • إعدادات الأذان • سجل القراءة والرسائل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A110B).copy(alpha = 0.72f)
                    )
                }
            }
        }

        // 2. Cumulative Adhkar & Tasbeeh Counter Summary Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF3E9D2),
            border = BorderStroke(1.4.dp, GoldAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cumulative_adhkar_tasbeeh_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            .background(EmeraldGreen.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إجمالي الأذكار والتسبيحات التراكمي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "مجموع كل تسبيحة وذكر قرأته في التطبيق (محفوظ دائماً)",
                            fontSize = 10.5.sp,
                            color = Color(0xFF1A110B).copy(alpha = 0.72f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreen,
                    border = BorderStroke(1.dp, GoldAccent)
                ) {
                    Text(
                        text = "$cumulativeAdhkarTasbeehTotal",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 3. CARD 1: Comprehensive User Guide (دليل المستخدم الشامل وطريقة الضغطات - مغلق افتراضياً وبلون مميز)
        SettingsExpandableSectionCard(
            title = "📖 دليل استخدام البرنامج والضغطات الذكية",
            subtitle = "اضغط هنا لفتح الدليل وشرح كل ميزة وكل ضغطة داخل المصحف والأذكار والأوراد",
            icon = Icons.Default.MenuBook,
            badgeText = if (expandedSection == "user_guide") "إغلاق الدليل" else "اضغط للفتح",
            isExpanded = expandedSection == "user_guide",
            isFeaturedDistinct = true,
            onToggle = {
                expandedSection = if (expandedSection == "user_guide") null else "user_guide"
            },
            testTag = "settings_card_user_guide"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UserGuideTopicCard(
                    numberBadge = "١",
                    title = "حركات وضغطات المصحف الشريف (٣ ضغطات ذكية)",
                    items = listOf(
                        "• ضغطة واحدة على الآية: يتم تظليل الآية بلون رمادي فاتح لتركيز العين أثناء القراءة، وحتى لو تشتت نظرك خارج المصحف تعود عينك لمكان وقوفك بسهولة.",
                        "• ضغطتان متتاليتان على الآية: تفتح قائمة الآية العائمة التي تحتوي على (التفسير الميسر، الاستماع للآية، حفظ علامة الوقف الخضراء، تمييز الحفظ، ونسخ ومشاركة الآية).",
                        "• ضغطة مطولة على كلمة بعينها: إذا استشكل عليك نطق كلمة أثناء القراءة، اضغط ضغطة مطولة على الكلمة فقط؛ سيتم تظليلها باللون الأخضر الزمردي الفاتح مع نبض بصري وسماع نطقها الصحيح مرتين متتاليتين تلقائياً وبدون أي قوائم.",
                        "• شريط أدوات المصحف العلوي: زر (Tt) لتكبير وتصغير خط المصحف حسب حاجتك، زر الفهرس والبحث، زر اختيار القارئ والتكرار، الوضع الليلي، وملء الشاشة."
                    )
                )

                UserGuideTopicCard(
                    numberBadge = "٢",
                    title = "الأوراد المخصصة، دعاء تجديد النوايا، وتسجيل الوقف",
                    items = listOf(
                        "• دعاء تجديد نوايا قراءة القرآن الكريم: زر بارز أعلى تبويب الأوراد يفتح الدعاء الجامع لتجديد النوايا وبركة القرآن الكريم قبل بدء التلاوة.",
                        "• إضافة ورد أو سورة ثابتة: يمكنك إنشاء أكثر من ورد ختمة، أو إنشاء ورد سورة ثابتة (مثل سورة الكهف يوم الجمعة) يفتح مباشرة على السورة.",
                        "• تسجيل الوقف أو تصوير الصفحة: يمكنك تسجيل موضع توقفك يدوياً أو بتصوير صفحة المصحف الورقي بالكاميرا ليتعرف التطبيق تلقائياً على رقم الصفحة والآية."
                    )
                )

                UserGuideTopicCard(
                    numberBadge = "٣",
                    title = "الأذكار، حصن المسلم، المفضلة، والترتيب الحر",
                    items = listOf(
                        "• الإضافة للمفضلة (❤️): بجانب كل ذكر وكل باب من أبواب حصن المسلم توجد علامة القلب لإضافته إلى «الأذكار المفضلة» والوصول إليه فوراً من الزر الخارجي.",
                        "• إضافة ذكر يدوي: داخل الأذكار أو المفضلة يمكنك إضافة أي ذكر أو دعاء قرأته على الإنترنت مع تحديد عدد مرات تكراره وموضع إضافته.",
                        "• ترتيب الأذكار بحرية (▲ / ▼): استخدم أسهم الصعود والنزول بجانب أي ذكر لترتيب الأذكار صعوداً أو نزولاً حسب رغبتك في أي قائمة.",
                        "• تعديل الذكر بالضغط المطول: اضغط ضغطة مطولة على أي ذكر (أو زر تعديل) لتعديل نصه، أو تغيير عدد مرات تكراره (مثلاً من مرة إلى ٣ مرات)، أو حذفه.",
                        "• التصفير اليومي واليدوي: يتم تصفير جميع عدادات الأذكار تلقائياً كل يوم الساعة 12:00 منتصف الليل، مع وجود زر تصفير يدوي لكل باب ولكل ذكر."
                    )
                )

                UserGuideTopicCard(
                    numberBadge = "٤",
                    title = "مواقيت الصلاة، الأذان، القبلة، والنسخ الاحتياطي",
                    items = listOf(
                        "• مواقيت الصلاة والقبلة: تحديد موقعك تلقائياً عبر الـ GPS أو البحث عن أي مدينة، مع بوصلة محراب القبلة التفاعلية.",
                        "• المنبهات الذكية: يمكنك ضبط منبه خاص لكل ورد قرآني ولكل ذكر ولأذكار الصباح والمساء.",
                        "• النسخ الاحتياطي والاستعادة: احفظ نسخة احتياطية شاملة لبياناتك وأورادك وأذكارك المفضلة على هاتفك لاستعادتها في أي وقت."
                    )
                )
            }
        }

        // 4. CARD 2: Backup & Restore (النسخ الاحتياطي والاستعادة الشاملة)
        SettingsExpandableSectionCard(
            title = "☁️ النسخ الاحتياطي والاستعادة الشاملة",
            subtitle = "حفظ واسترجاع الأوراد، علامات الوقف، الأذكار المفضلة والمضافة، وإعدادات الأذان",
            icon = Icons.Default.Backup,
            badgeText = "حفظ واستعادة",
            isExpanded = expandedSection == "backup",
            onToggle = {
                expandedSection = if (expandedSection == "backup") null else "backup"
            },
            testTag = "settings_card_backup"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "احفظ نسخة احتياطية كاملة في ملف على هاتفك أو عبر الحافظة لاستعادتها في أي وقت عند تحديث التطبيق أو تغيير الهاتف.",
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF1A110B).copy(alpha = 0.85f)
                )
                Button(
                    onClick = onOpenBackupDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("history_backup_restore_button")
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فتح نافذة النسخ الاحتياطي والاستعادة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 5. CARD 3: Adhan & Prayer Times Settings (إعدادات الأذان ومواقيت الصلاة)
        SettingsExpandableSectionCard(
            title = "🕌 إعدادات الأذان ومواقيت الصلاة والموقع",
            subtitle = "المدينة الحالية: ${adhanSettings.locationName} • ${adhanSettings.globalAlertMode.titleAr}",
            icon = Icons.Default.NotificationsActive,
            badgeText = "الأذان والموقع",
            isExpanded = expandedSection == "adhan",
            onToggle = {
                expandedSection = if (expandedSection == "adhan") null else "adhan"
            },
            testTag = "settings_card_adhan"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFDF8EC),
                    border = BorderStroke(1.dp, GoldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📍 الموقع المحفوظ: ${adhanSettings.locationName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "🔔 وضع التنبيه: ${adhanSettings.globalAlertMode.titleAr}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = GoldAccent
                        )
                        Text(
                            text = "🧭 طريقة الحساب: ${adhanSettings.calculationMethod.titleAr}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF1A110B).copy(alpha = 0.78f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAdhanSettingsModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ضبط الأذان والموقع",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (isPlayingAdhanPreview) {
                                MihrabiAlarmManager.stopRingtone()
                                isPlayingAdhanPreview = false
                            } else {
                                PrayerTimesAndAdhanManager.playAdhanAudio(
                                    context = context,
                                    mode = if (adhanSettings.globalAlertMode == AdhanAlertMode.OFF)
                                        AdhanAlertMode.FULL_ADHAN
                                    else
                                        adhanSettings.globalAlertMode,
                                    customUri = adhanSettings.customAdhanUri
                                )
                                isPlayingAdhanPreview = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingAdhanPreview) Icons.Default.StopCircle else Icons.Default.PlayCircleFilled,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isPlayingAdhanPreview) "إيقاف الصوت" else "سماع الأذان",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenPrayerAndQiblaTab,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فتح شاشة مواقيت الصلاة وبوصلة القبلة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 6. CARD 4: Mushaf Reading Logs & Current Stop Point (سجل القراءة ونقطة التوقف)
        SettingsExpandableSectionCard(
            title = "📜 سجل قراءة المصحف ونقطة التوقف",
            subtitle = "آخر توقف: سورة $lastReadSurah (آية $lastReadAyah • ص $lastReadPage) • السجلات (${readingLogs.size})",
            icon = Icons.Default.MenuBook,
            badgeText = "${readingLogs.size} سجل",
            isExpanded = expandedSection == "reading_logs",
            onToggle = {
                expandedSection = if (expandedSection == "reading_logs") null else "reading_logs"
            },
            testTag = "history_subtab_mushaf"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFDF8EC),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
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
                                fontSize = 11.5.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "سورة $lastReadSurah • آية $lastReadAyah • صفحة $lastReadPage",
                                fontSize = 13.5.sp,
                                color = Color(0xFF1A110B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onResumeReadingAtLastStop,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "متابعة القراءة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (readingLogs.isEmpty()) {
                    Text(
                        text = "لا توجد سجلات قراءة سابقة بعد.",
                        fontSize = 13.sp,
                        color = Color(0xFF1A110B).copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                } else {
                    readingLogs.take(25).forEach { log ->
                        val dateFormatted = remember(log.timestampMillis) {
                            try {
                                SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(log.timestampMillis))
                            } catch (_: Exception) {
                                ""
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFDF8EC),
                            border = BorderStroke(0.8.dp, GoldBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سورة ${log.surah} - آية ${log.ayah}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EmeraldGreen
                                    )
                                    Text(
                                        text = "+${log.pagesReadToday} صفحة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = GoldAccent
                                    )
                                }
                                if (dateFormatted.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateFormatted,
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF1A110B).copy(alpha = 0.55f)
                                    )
                                }
                                if (log.reflectionSummary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.reflectionSummary,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF1A110B),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. CARD 5: Spiritual Messages History (سجل الرسائل الإيمانية)
        SettingsExpandableSectionCard(
            title = "🕊️ سجل الرسائل الإيمانية",
            subtitle = "استعراض ومشاركة الرسائل الإيمانية المحفوظة (${spiritualMessages.size} رسالة)",
            icon = Icons.Default.AutoAwesome,
            badgeText = "${spiritualMessages.size} رسالة",
            isExpanded = expandedSection == "spiritual_messages",
            onToggle = {
                expandedSection = if (expandedSection == "spiritual_messages") null else "spiritual_messages"
            },
            testTag = "history_subtab_messages"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اضغط على أي رسالة لفتحها أو مشاركتها:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A110B).copy(alpha = 0.78f),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onGenerateNewSpiritualMessage,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "رسالة جديدة الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (spiritualMessages.isEmpty()) {
                    Text(
                        text = "لا توجد رسائل إيمانية مسجلة بعد.",
                        fontSize = 13.sp,
                        color = Color(0xFF1A110B).copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                } else {
                    spiritualMessages.take(25).forEach { msg ->
                        val dateStr = remember(msg.timestampMillis) {
                            try {
                                SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(msg.timestampMillis))
                            } catch (_: Exception) {
                                ""
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFDF8EC),
                            border = BorderStroke(1.dp, GoldBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenSpiritualMessage(msg) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldGreen.copy(alpha = 0.12f),
                                        border = BorderStroke(0.6.dp, EmeraldGreen)
                                    ) {
                                        Text(
                                            text = msg.categoryTitle,
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (dateStr.isNotBlank()) {
                                            Text(
                                                text = dateStr,
                                                fontSize = 10.sp,
                                                color = Color(0xFF1A110B).copy(alpha = 0.55f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        IconButton(
                                            onClick = { shareSpiritualMessage(context, msg) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "مشاركة الرسالة",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = msg.arabicText,
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp,
                                    lineHeight = 27.sp,
                                    color = Color(0xFF1A110B),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.sourceReference,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Quick Shortcut Card to Quran Intention Renewal Dua
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF3E9D2),
            border = BorderStroke(1.dp, GoldBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenIntentionDuaDialog() }
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
                    Text("🤲", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "دعاء تجديد نوايا قراءة القرآن الكريم",
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "فتح الدعاء الجامع لتجديد النوايا وبركة القرآن الكريم",
                            fontSize = 11.sp,
                            color = Color(0xFF1A110B).copy(alpha = 0.72f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = GoldAccent
                )
            }
        }
    }

    if (showAdhanSettingsModal) {
        AdhanSettingsDialog(
            initialSettings = adhanSettings,
            focusedPrayer = null,
            onRequestAutoLocation = {
                showAdhanSettingsModal = false
                onOpenPrayerAndQiblaTab()
            },
            onDismiss = {
                MihrabiAlarmManager.stopRingtone()
                isPlayingAdhanPreview = false
                showAdhanSettingsModal = false
            },
            onSave = { updated ->
                MihrabiAlarmManager.stopRingtone()
                isPlayingAdhanPreview = false
                adhanSettings = updated
                PrayerTimesAndAdhanManager.saveSettingsAndSchedule(context, updated)
                showAdhanSettingsModal = false
                Toast.makeText(context, "تم حفظ إعدادات الأذان والمواقيت بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun SettingsExpandableSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    isExpanded: Boolean,
    isFeaturedDistinct: Boolean = false,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val cardBorderWidth = when {
        isFeaturedDistinct -> 2.dp
        isExpanded -> 1.6.dp
        else -> 1.dp
    }
    val cardBorderColor = when {
        isFeaturedDistinct -> GoldAccent
        isExpanded -> EmeraldGreen
        else -> GoldBorder
    }
    val cardContainerColor = if (isFeaturedDistinct) {
        Color(0xFFF7EED8)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = cardBorderWidth,
                color = cardBorderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFeaturedDistinct) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val headerModifier = if (isFeaturedDistinct) {
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF09423A),
                                Color(0xFF0F6256),
                                Color(0xFF167264)
                            )
                        )
                    )
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            }

            Row(
                modifier = headerModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isFeaturedDistinct -> GoldAccent
                                    isExpanded -> EmeraldGreen
                                    else -> Color(0xFFEBE1C9)
                                }
                            )
                            .border(
                                width = 1.2.dp,
                                color = when {
                                    isFeaturedDistinct -> Color(0xFFFFF3C4)
                                    isExpanded -> GoldAccent
                                    else -> GoldBorder
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = when {
                                isFeaturedDistinct -> Color(0xFF09423A)
                                isExpanded -> Color.White
                                else -> EmeraldGreen
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isFeaturedDistinct) 15.sp else 14.5.sp,
                            lineHeight = 20.sp,
                            color = if (isFeaturedDistinct) Color.White else EmeraldGreen,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = if (isFeaturedDistinct) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isFeaturedDistinct) Color(0xFFFBE7A1) else Color(0xFF1A110B).copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isFeaturedDistinct -> GoldAccent
                            isExpanded -> EmeraldGreen.copy(alpha = 0.12f)
                            else -> Color(0xFFEBE1C9)
                        },
                        border = BorderStroke(
                            width = 0.9.dp,
                            color = if (isFeaturedDistinct) Color(0xFFFFF3C4) else GoldBorder
                        )
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFeaturedDistinct) Color(0xFF1A110B) else EmeraldGreen,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "طي القسم" else "فتح القسم",
                        tint = if (isFeaturedDistinct) Color(0xFFFFE082) else GoldAccent
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                ) {
                    HorizontalDivider(
                        color = if (isFeaturedDistinct) GoldAccent.copy(alpha = 0.65f) else GoldBorder.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun UserGuideTopicCard(
    numberBadge: String,
    title: String,
    items: List<String>
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFDF8EC),
        border = BorderStroke(1.dp, GoldBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldGreen,
                    border = BorderStroke(0.8.dp, GoldAccent)
                ) {
                    Text(
                        text = numberBadge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp,
                    color = EmeraldGreen
                )
            }

            items.forEach { bullet ->
                Text(
                    text = bullet,
                    fontSize = 12.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A110B),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
