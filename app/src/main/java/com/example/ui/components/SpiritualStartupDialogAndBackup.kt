package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.alarm.MihrabiAlarmManager
import com.example.alarm.PrayerTimesAndAdhanManager
import com.example.audio.QuranAudioPlayerManager
import com.example.data.SpiritualMessageItem
import com.example.data.local.LocalKhatmaStorage
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun shareSpiritualMessage(context: Context, message: SpiritualMessageItem) {
    val shareBody = buildString {
        append("🕊️✉️ رسالة إيمانية من تطبيق محرابي\n")
        append("━━━━━━━━━━━━━━━━━━\n")
        append("✨ ${message.categoryTitle}\n\n")
        append("${message.arabicText}\n\n")
        append("📚 المصدر: ${message.sourceReference}\n")
        append("━━━━━━━━━━━━━━━━━━\n")
        append("تطبيق محرابي - إعداد آسر الشربيني | وقف لله تعالى")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الرسالة والدعاء"))
}

/**
 * Ornate historical Islamic manuscript-style dialog displayed on app startup
 * (and re-openable anytime from the "السجل" tab).
 */
@Composable
fun SpiritualStartupMessageDialog(
    message: SpiritualMessageItem,
    onMarkReadAndClose: () -> Unit,
    onShare: () -> Unit,
    onDismissClose: () -> Unit
) {
    val parchmentLight = Color(0xFFFDF8EE)
    val parchmentMid = Color(0xFFF5E8CE)
    val deepInk = Color(0xFF0B1426)

    Dialog(
        onDismissRequest = onDismissClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            RoyalNavyDark,
                            Color(0xFF132238),
                            RoyalNavyDark
                        )
                    )
                )
                .border(2.dp, GoldAccent, RoundedCornerShape(26.dp))
                .padding(6.dp)
                .testTag("spiritual_startup_dialog")
        ) {
            // Inner decorative gold filigree frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, GoldBorder.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                // Subtle corner Islamic geometric ornaments drawn with Canvas
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cornerRadius = 22.dp.toPx()
                    val stroke = Stroke(width = 1.4.dp.toPx())
                    val ornamentColor = GoldAccent.copy(alpha = 0.32f)

                    // 4 Corner double-arcs
                    drawArc(
                        color = ornamentColor,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(-cornerRadius, -cornerRadius),
                        size = Size(cornerRadius * 2, cornerRadius * 2),
                        style = stroke
                    )
                    drawArc(
                        color = ornamentColor,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(size.width - cornerRadius, -cornerRadius),
                        size = Size(cornerRadius * 2, cornerRadius * 2),
                        style = stroke
                    )
                    drawArc(
                        color = ornamentColor,
                        startAngle = 270f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(-cornerRadius, size.height - cornerRadius),
                        size = Size(cornerRadius * 2, cornerRadius * 2),
                        style = stroke
                    )
                    drawArc(
                        color = ornamentColor,
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(size.width - cornerRadius, size.height - cornerRadius),
                        size = Size(cornerRadius * 2, cornerRadius * 2),
                        style = stroke
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Small ornate top header: "رسالة من مطور التطبيق" with Dove & Envelope crest
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1A2B4C),
                        border = BorderStroke(1.dp, GoldAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Custom Dove + Envelope Golden Emblem
                            DoveAndMessageIcon(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "رسالة من مطور التطبيق",
                                color = GoldAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🕊️✉️",
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Category Pill (Quranic Dua / Prophetic Hadith / Qudsi Hadith)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldGreen.copy(alpha = 0.25f),
                        border = BorderStroke(0.8.dp, EmeraldGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = message.categoryTitle,
                                color = Color(0xFFFDF8EE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Historical Illuminated Parchment Scroll Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(parchmentLight, parchmentMid, parchmentLight)
                                )
                            )
                            .border(1.5.dp, GoldAccent, RoundedCornerShape(18.dp))
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top decorative Bismillah / star divider
                            Text(
                                text = "۞ ❁ ۞",
                                color = Color(0xFF9C7832),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = message.arabicText,
                                color = deepInk,
                                fontSize = 18.5.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(0.6f),
                                thickness = 1.dp,
                                color = GoldAccent.copy(alpha = 0.55f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = message.sourceReference,
                                color = EmeraldGreen,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "تُحفظ هذه الرسالة تلقائياً في تبويب «السجل» للرجوع إليها في أي وقت",
                        color = Color(0xFFFDF8EE).copy(alpha = 0.72f),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Three Action Buttons: تمت القراءة | مشاركة | غلق
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Button 1: تمت القراءة
                        Button(
                            onClick = onMarkReadAndClose,
                            modifier = Modifier
                                .weight(1.25f)
                                .height(46.dp)
                                .testTag("startup_msg_read_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "تمت القراءة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Button 2: مشاركة
                        OutlinedButton(
                            onClick = onShare,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("startup_msg_share_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, GoldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = GoldAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مشاركة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Button 3: غلق
                        OutlinedButton(
                            onClick = onDismissClose,
                            modifier = Modifier
                                .weight(0.85f)
                                .height(46.dp)
                                .testTag("startup_msg_close_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDF8EE).copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFDF8EE)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "غلق",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas illustration of a peaceful Dove carrying a golden message envelope.
 */
@Composable
fun DoveAndMessageIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Dove wing & body silhouette in warm gold
        val dovePath = Path().apply {
            moveTo(w * 0.15f, h * 0.55f)
            quadraticTo(w * 0.35f, h * 0.20f, w * 0.65f, h * 0.35f)
            quadraticTo(w * 0.48f, h * 0.10f, w * 0.28f, h * 0.15f)
            quadraticTo(w * 0.58f, h * 0.08f, w * 0.78f, h * 0.38f)
            quadraticTo(w * 0.92f, h * 0.42f, w * 0.95f, h * 0.50f)
            quadraticTo(w * 0.78f, h * 0.65f, w * 0.45f, h * 0.65f)
            close()
        }
        drawPath(path = dovePath, color = GoldAccent)

        // Small message envelope below beak
        val envWidth = w * 0.42f
        val envHeight = h * 0.28f
        val envLeft = w * 0.48f
        val envTop = h * 0.62f
        drawRoundRect(
            color = Color(0xFFFDF8EE),
            topLeft = Offset(envLeft, envTop),
            size = Size(envWidth, envHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = GoldAccent,
            topLeft = Offset(envLeft, envTop),
            size = Size(envWidth, envHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Full Local Backup & Restore Dialog allowing the user to save their progress to a JSON file
 * on their phone (or copy/paste via clipboard) and restore it anytime after updating or reinstalling the app.
 */
@Composable
fun BackupRestoreDialog(
    tracksCount: Int,
    logsCount: Int,
    currentSurah: String,
    currentPage: Int,
    onDismiss: () -> Unit,
    onRestoredSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val storage = remember { LocalKhatmaStorage.getInstance(context) }
    val restoreVersion by storage.backupRestoreVersionFlow.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showPasteCodeField by remember { mutableStateOf(false) }
    var pastedJsonCode by remember { mutableStateOf("") }

    // Live snapshot of all app settings included in the backup
    val adhanSettings = remember(restoreVersion) {
        PrayerTimesAndAdhanManager.loadSettings(context)
    }
    val customAlarms = remember(restoreVersion) {
        MihrabiAlarmManager.getAllAlarms(context)
    }
    val quranAudioState by QuranAudioPlayerManager.playbackState.collectAsState()
    val hifzCount = remember(restoreVersion) {
        context.getSharedPreferences("quran_reader_prefs", Context.MODE_PRIVATE)
            .getStringSet("memorized_ayahs_set", emptySet())?.size ?: 0
    }

    LaunchedEffect(Unit) {
        QuranAudioPlayerManager.initPreferences(context)
    }

    // 1. Export (.json file creation via Android Storage Access Framework)
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val backupJson = storage.exportFullBackupJson(context)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(backupJson.toByteArray(Charsets.UTF_8))
                }
                isError = false
                statusMessage = "تم حفظ ملف النسخة الاحتياطية الشاملة على هاتفك بنجاح (يشمل الختمة، القارئ والتكرار، إعدادات الأذان ومواقيت وإشعارات الصلوات الخمس، النغمات، والمنبهات)!"
                Toast.makeText(context, "تم حفظ النسخة الاحتياطية الشاملة بنجاح", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                isError = true
                statusMessage = "تعذر حفظ الملف: ${e.localizedMessage}"
            }
        }
    }

    // 2. Import (.json file selection via Android Storage Access Framework)
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }.orEmpty()
                val res = storage.importFullBackupJson(context, content)
                res.onSuccess { summary ->
                    isError = false
                    statusMessage = summary
                    Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                    onRestoredSuccess(summary)
                }.onFailure { err ->
                    isError = true
                    statusMessage = err.localizedMessage ?: "فشل استعادة النسخة الاحتياطية"
                }
            } catch (e: Exception) {
                isError = true
                statusMessage = "تعذر قراءة الملف المختار: ${e.localizedMessage}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "النسخ الاحتياطي الشامل للتطبيق",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
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
                // Current Progress & Complete Settings Summary Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RoyalNavyDark,
                    border = BorderStroke(1.dp, GoldBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "كل ما يتم حفظه واستعادته في النسخة الاحتياطية:",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "📖 الختمة والمصحف: سورة $currentSurah (صفحة $currentPage) • $tracksCount ورد • $logsCount سجل • $hifzCount علامة حفظ",
                            color = Color(0xFFFDF8EE),
                            fontSize = 11.5.sp
                        )
                        val repeatLabel = if (quranAudioState.targetRepeatCount == -1) "تكرار مستمر" else "${quranAudioState.targetRepeatCount} مرات"
                        Text(
                            text = "🎙️ القارئ والتكرار: الشيخ ${quranAudioState.selectedReciter.nameAr} • التكرار ($repeatLabel)",
                            color = Color(0xFFFDF8EE),
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "🕌 الأذان ومواقيت الصلاة: ${adhanSettings.locationName} • ${adhanSettings.calculationMethod.titleAr}",
                            color = Color(0xFFFDF8EE),
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "🔔 إشعارات ونغمات الصلوات الخمس: ${adhanSettings.globalAlertMode.titleAr} + تخصيص كل صلاة (${customAlarms.size} منبه مخصص)",
                            color = Color(0xFFFDF8EE).copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "احفظ نسخة احتياطية شاملة لكل تقدمك وإعدادات التطبيق (الأذان، إشعارات كل صلاة، النغمات، القارئ، التكرار، وعلامات الوقف والحفظ) واستعدها بضغطة واحدة بعد التحديث.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 17.sp
                )

                // Primary Button 1: Save Backup File (.json) to Phone
                Button(
                    onClick = {
                        val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                        exportFileLauncher.launch("mihrabi_backup_$dateTag.json")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_backup_file_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ ملف النسخة الاحتياطية على الهاتف",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Primary Button 2: Restore Backup File (.json) from Phone
                Button(
                    onClick = {
                        importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_backup_file_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalNavyDark,
                        contentColor = GoldAccent
                    ),
                    border = BorderStroke(1.2.dp, GoldAccent)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "استعادة التقدم من ملف محفوظ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider(color = GoldBorder.copy(alpha = 0.35f))

                // Quick Clipboard Copy / Paste Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val json = storage.exportFullBackupJson(context)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Mihrabi Backup", json))
                            isError = false
                            statusMessage = "تم نسخ كود التقدم بالكامل إلى الحافظة! يمكنك لصقه في الملاحظات أو الواتساب للاحتفاظ به."
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldBorder),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = GoldAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ كود التقدم", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showPasteCodeField = !showPasteCodeField },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استعادة بكود منسوخ", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                }

                if (showPasteCodeField) {
                    OutlinedTextField(
                        value = pastedJsonCode,
                        onValueChange = { pastedJsonCode = it },
                        label = { Text("ألصق كود النسخة الاحتياطية هنا") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Button(
                        onClick = {
                            val res = storage.importFullBackupJson(context, pastedJsonCode)
                            res.onSuccess { summary ->
                                isError = false
                                statusMessage = summary
                                showPasteCodeField = false
                                onRestoredSuccess(summary)
                            }.onFailure { err ->
                                isError = true
                                statusMessage = err.localizedMessage
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("تأكيد استعادة الكود", fontWeight = FontWeight.Bold)
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else EmeraldGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage!!,
                            modifier = Modifier.padding(10.dp),
                            color = if (isError) MaterialTheme.colorScheme.error else EmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = GoldAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}
