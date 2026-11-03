package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmConfig
import com.example.alarm.MihrabiAlarmManager
import com.example.alarm.ToneType
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoyalNavyDark

/**
 * Compact, reusable button/chip that displays the current alarm state for a given [targetId]
 * and opens [AlarmSetupDialog] when tapped.
 */
@Composable
fun AlarmTriggerChip(
    targetId: String,
    defaultTitle: String,
    defaultSubtitle: String,
    defaultHour: Int = 6,
    defaultMinute: Int = 0,
    compact: Boolean = false,
    accentColor: Color = GoldAccent,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var alarmConfig by remember(targetId) {
        mutableStateOf(
            MihrabiAlarmManager.getAlarmForTarget(
                context = context,
                targetId = targetId,
                defaultTitle = defaultTitle,
                defaultSubtitle = defaultSubtitle,
                defaultHour = defaultHour,
                defaultMinute = defaultMinute
            )
        )
    }
    var showDialog by remember { mutableStateOf(false) }

    val formattedTime = remember(alarmConfig.hour, alarmConfig.minute) {
        alarmConfig.formattedTime()
    }

    if (compact) {
        Surface(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(10.dp),
            color = if (alarmConfig.isEnabled) accentColor.copy(alpha = 0.22f) else RoyalNavyDark.copy(alpha = 0.65f),
            border = BorderStroke(
                1.dp,
                if (alarmConfig.isEnabled) accentColor else GoldAccent.copy(alpha = 0.45f)
            ),
            modifier = modifier
                .heightIn(min = 34.dp)
                .testTag("alarm_chip_$targetId")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (alarmConfig.isEnabled) Icons.Default.NotificationsActive else Icons.Default.AlarmAdd,
                    contentDescription = "منبه $defaultTitle",
                    tint = if (alarmConfig.isEnabled) accentColor else GoldAccent,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (alarmConfig.isEnabled) formattedTime else "منبه",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarmConfig.isEnabled) accentColor else GoldAccent
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (alarmConfig.isEnabled) EmeraldGreen else accentColor.copy(alpha = 0.7f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (alarmConfig.isEnabled) EmeraldGreen.copy(alpha = 0.14f) else RoyalNavyDark.copy(alpha = 0.4f),
                contentColor = if (alarmConfig.isEnabled) EmeraldGreen else GoldAccent
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = modifier
                .heightIn(min = 42.dp)
                .testTag("alarm_button_$targetId")
        ) {
            Icon(
                imageVector = if (alarmConfig.isEnabled) Icons.Default.NotificationsActive else Icons.Default.AlarmAdd,
                contentDescription = "منبه $defaultTitle",
                tint = if (alarmConfig.isEnabled) EmeraldGreen else GoldAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (alarmConfig.isEnabled) "منبه مفعّل ($formattedTime)" else "إضافة منبه تذكير",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (alarmConfig.isEnabled) EmeraldGreen else GoldAccent
            )
        }
    }

    if (showDialog) {
        AlarmSetupDialog(
            initialConfig = alarmConfig,
            onDismiss = {
                MihrabiAlarmManager.stopRingtone()
                showDialog = false
            },
            onSaved = { updated ->
                MihrabiAlarmManager.stopRingtone()
                alarmConfig = updated
                showDialog = false
            }
        )
    }
}

@Composable
fun AlarmSetupDialog(
    initialConfig: AlarmConfig,
    onDismiss: () -> Unit,
    onSaved: (AlarmConfig) -> Unit
) {
    val context = LocalContext.current

    var isEnabled by remember { mutableStateOf(initialConfig.isEnabled) }
    var hour by remember { mutableIntStateOf(initialConfig.hour) }
    var minute by remember { mutableIntStateOf(initialConfig.minute) }
    var toneType by remember { mutableStateOf(initialConfig.toneType) }
    var customToneUri by remember { mutableStateOf(initialConfig.customToneUri) }
    var customToneName by remember { mutableStateOf(initialConfig.customToneName) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // Permissions check state
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var canScheduleExact by remember {
        mutableStateOf(MihrabiAlarmManager.canScheduleExactAlarms(context))
    }

    // Stop any preview audio when dialog is disposed
    DisposableEffect(Unit) {
        onDispose {
            MihrabiAlarmManager.stopRingtone()
        }
    }

    // Launcher for Notification Permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            Toast.makeText(context, "تم تفعيل صلاحية إشعارات المنبه بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "يرجى السماح بالإشعارات ليظهر تنبيه الورد والأذكار", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher for Exact Alarm Settings (Android 12+)
    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        canScheduleExact = MihrabiAlarmManager.canScheduleExactAlarms(context)
    }

    // Launcher for System Ringtone Picker (Phone's built-in alarm / ringtones)
    val systemRingtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val title = ringtone?.getTitle(context) ?: "نغمة رنين الهاتف"
                toneType = ToneType.SYSTEM_RINGTONE
                customToneUri = uri.toString()
                customToneName = title
            }
        }
    }

    // Launcher for Custom Audio File from Phone Storage (SAF OpenDocument - Audio files)
    val customAudioFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            var displayName = "ملف صوتي مخصص من الهاتف"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                }
            } catch (_: Exception) {
            }
            toneType = ToneType.CUSTOM_AUDIO_FILE
            customToneUri = uri.toString()
            customToneName = displayName
            Toast.makeText(context, "تم اختيار النغمة: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    // Automatically prompt for notification permission on first open if not granted
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(RoyalNavyDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ضبط منبه التذكير",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Text(
                                text = initialConfig.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen
                        ),
                        modifier = Modifier.testTag("alarm_enable_switch")
                    )
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // Permissions Status Banner (Alarm, Ringtone & File Access)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (hasNotificationPermission && canScheduleExact)
                        EmeraldGreen.copy(alpha = 0.1f)
                    else
                        GoldAccent.copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        if (hasNotificationPermission && canScheduleExact)
                            EmeraldGreen.copy(alpha = 0.4f)
                        else
                            GoldAccent.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (hasNotificationPermission && canScheduleExact)
                                    Icons.Default.VerifiedUser
                                else
                                    Icons.Default.Security,
                                contentDescription = null,
                                tint = if (hasNotificationPermission && canScheduleExact) EmeraldGreen else GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (hasNotificationPermission && canScheduleExact)
                                    "صلاحيات المنبه والرنين والملفات مفعّلة وجاهزة"
                                else
                                    "تفعيل صلاحيات المنبه والتنبيه الدقيق",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!hasNotificationPermission || !canScheduleExact) {
                            Text(
                                text = "لضمان عمل المنبه في وقته المحدد وتشغيل نغمة الرنين حتى عند إغلاق التطبيق، يرجى منح الصلاحيات التالية:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Button(
                                        onClick = {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RoyalNavyDark,
                                            contentColor = GoldAccent
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("السماح بالتنبيهات", fontSize = 11.sp)
                                    }
                                }
                                if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                exactAlarmSettingsLauncher.launch(intent)
                                            } catch (_: Exception) {
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("صلاحية المنبه الدقيق", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Time Picker Section
                Text(
                    text = "وقت المنبه اليومي:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )

                val isPm = hour >= 12
                val displayHour12 = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = RoyalNavyDark.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الساعة", fontSize = 11.sp, color = GoldAccent)
                            IconButton(
                                onClick = {
                                    hour = (hour + 1) % 24
                                    isEnabled = true
                                },
                                modifier = Modifier.testTag("alarm_hour_up")
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "زيادة الساعة", tint = GoldAccent)
                            }
                            Text(
                                text = String.format("%02d", displayHour12),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    hour = if (hour - 1 < 0) 23 else hour - 1
                                    isEnabled = true
                                },
                                modifier = Modifier.testTag("alarm_hour_down")
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "إنقاص الساعة", tint = GoldAccent)
                            }
                        }

                        Text(
                            text = ":",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAccent
                        )

                        // Minute selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الدقيقة", fontSize = 11.sp, color = GoldAccent)
                            IconButton(
                                onClick = {
                                    minute = (minute + 5) % 60
                                    isEnabled = true
                                },
                                modifier = Modifier.testTag("alarm_minute_up")
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "زيادة الدقائق", tint = GoldAccent)
                            }
                            Text(
                                text = String.format("%02d", minute),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    minute = if (minute - 5 < 0) 55 else minute - 5
                                    isEnabled = true
                                },
                                modifier = Modifier.testTag("alarm_minute_down")
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "إنقاص الدقائق", tint = GoldAccent)
                            }
                        }

                        // AM / PM Toggle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = !isPm,
                                onClick = {
                                    if (isPm) hour -= 12
                                    isEnabled = true
                                },
                                label = { Text("صباحاً (ص)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = isPm,
                                onClick = {
                                    if (!isPm) hour += 12
                                    isEnabled = true
                                },
                                label = { Text("مساءً (م)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalNavyDark,
                                    selectedLabelColor = GoldAccent
                                )
                            )
                        }
                    }
                }

                // Ringtone Selection Section
                Text(
                    text = "اختيار نغمة رنين المنبه:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )

                // Option 1: Bundled Islamic Nasheed (Uploaded by user)
                ToneOptionCard(
                    selected = toneType == ToneType.ISLAMIC_BUNDLED,
                    title = "النغمة الإسلامية المرفقة (افتراضي)",
                    subtitle = "يا عباد الله جدّوا رب داع لا يرد - الحارث النمر",
                    icon = Icons.Default.MusicNote,
                    onClick = {
                        toneType = ToneType.ISLAMIC_BUNDLED
                        customToneName = "يا عباد الله جدوا - الحارث النمر"
                        isEnabled = true
                    },
                    testTag = "tone_option_islamic"
                )

                // Option 2: Phone's Built-in Alarm / Ringtone
                ToneOptionCard(
                    selected = toneType == ToneType.SYSTEM_RINGTONE,
                    title = "نغمة رنين الهاتف نفسها",
                    subtitle = if (toneType == ToneType.SYSTEM_RINGTONE && customToneName.isNotBlank())
                        "المختارة: $customToneName"
                    else
                        "استخدام نغمة المنبه الافتراضية للهاتف أو اختيار نغمة من النظام",
                    icon = Icons.Default.PhoneAndroid,
                    actionButtonText = "اختيار نغمة الهاتف",
                    onActionClick = {
                        toneType = ToneType.SYSTEM_RINGTONE
                        isEnabled = true
                        try {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة رنين المنبه")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                if (customToneUri.isNotBlank()) {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(customToneUri))
                                }
                            }
                            systemRingtoneLauncher.launch(intent)
                        } catch (_: Exception) {
                            customToneName = "نغمة رنين الهاتف الافتراضية"
                            Toast.makeText(context, "تم تفعيل نغمة رنين الهاتف الافتراضية", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClick = {
                        toneType = ToneType.SYSTEM_RINGTONE
                        if (customToneName.isBlank() || customToneName.contains("يا عباد الله")) {
                            customToneName = "نغمة رنين الهاتف الافتراضية"
                        }
                        isEnabled = true
                    },
                    testTag = "tone_option_system"
                )

                // Option 3: Custom Audio File from Phone Files
                ToneOptionCard(
                    selected = toneType == ToneType.CUSTOM_AUDIO_FILE,
                    title = "نغمة مخصصة من ملفات الهاتف (MP3/Audio)",
                    subtitle = if (toneType == ToneType.CUSTOM_AUDIO_FILE && customToneUri.isNotBlank())
                        "الملف المختار: $customToneName"
                    else
                        "فتح مدير ملفات الهاتف لاختيار أي مقطع صوتي أو أنشودة",
                    icon = Icons.Default.FolderOpen,
                    actionButtonText = "تصفح ملفات الصوت",
                    onActionClick = {
                        isEnabled = true
                        customAudioFileLauncher.launch(arrayOf("audio/*"))
                    },
                    onClick = {
                        isEnabled = true
                        if (customToneUri.isBlank() || toneType != ToneType.CUSTOM_AUDIO_FILE) {
                            customAudioFileLauncher.launch(arrayOf("audio/*"))
                        } else {
                            toneType = ToneType.CUSTOM_AUDIO_FILE
                        }
                    },
                    testTag = "tone_option_custom_file"
                )

                // Preview / Test Ringtone Button
                OutlinedButton(
                    onClick = {
                        if (isPreviewPlaying) {
                            MihrabiAlarmManager.stopRingtone()
                            isPreviewPlaying = false
                        } else {
                            MihrabiAlarmManager.playAlarmTone(context, toneType, customToneUri)
                            isPreviewPlaying = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_alarm_tone_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isPreviewPlaying) MaterialTheme.colorScheme.error else GoldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isPreviewPlaying)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else
                            GoldAccent.copy(alpha = 0.12f)
                    )
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.StopCircle else Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = if (isPreviewPlaying) MaterialTheme.colorScheme.error else GoldAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPreviewPlaying) "إيقاف معاينة النغمة" else "تجربة ومعاينة صوت الرنين الآن",
                        fontWeight = FontWeight.Bold,
                        color = if (isPreviewPlaying) MaterialTheme.colorScheme.error else GoldAccent
                    )
                }

                // Save & Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            MihrabiAlarmManager.stopRingtone()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            MihrabiAlarmManager.stopRingtone()
                            val updatedConfig = initialConfig.copy(
                                isEnabled = isEnabled,
                                hour = hour,
                                minute = minute,
                                toneType = toneType,
                                customToneUri = customToneUri,
                                customToneName = customToneName
                            )
                            MihrabiAlarmManager.saveAndScheduleAlarm(context, updatedConfig)
                            val msg = if (isEnabled) {
                                "تم حفظ وتفعيل منبه (${initialConfig.title}) بنجاح"
                            } else {
                                "تم إيقاف المنبه"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onSaved(updatedConfig)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("save_alarm_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتفعيل المنبه", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToneOptionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) EmeraldGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) EmeraldGreen else GoldAccent.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) EmeraldGreen else GoldAccent,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (actionButtonText != null && onActionClick != null) {
                OutlinedButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 34.dp)
                ) {
                    Icon(
                        Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionButtonText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                }
            }
        }
    }
}
