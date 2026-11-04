package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.alarm.*
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PrayerTimesAndAdhanCard(
    externalSettings: AdhanSettingsState? = null,
    onExternalSettingsChanged: ((AdhanSettingsState) -> Unit)? = null
) {
    val context = LocalContext.current
    val backupVersion by remember {
        com.example.data.local.LocalKhatmaStorage.getInstance(context).backupRestoreVersionFlow
    }.collectAsState()
    var internalSettings by remember(backupVersion) {
        mutableStateOf(PrayerTimesAndAdhanManager.loadSettings(context))
    }
    val settings = externalSettings ?: internalSettings

    fun updateSettings(newSettings: AdhanSettingsState) {
        internalSettings = newSettings
        onExternalSettingsChanged?.invoke(newSettings)
    }

    var isDetectingLocation by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedPrayerForEdit by remember { mutableStateOf<PrayerKey?>(null) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    val prayerSlots = remember(settings) {
        PrayerTimesAndAdhanManager.calculateDailyPrayerTimes(settings)
    }
    val (nextPrayerSlot, remainingText) = remember(prayerSlots) {
        PrayerTimesAndAdhanManager.getNextPrayerInfo(prayerSlots)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isPlayingPreview) {
                MihrabiAlarmManager.stopRingtone()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetectingLocation = true
            PrayerTimesAndAdhanManager.detectLocationAutomatically(context) { lat, lon, locName, _ ->
                isDetectingLocation = false
                val updated = settings.copy(
                    latitude = lat,
                    longitude = lon,
                    locationName = locName,
                    isAutoLocation = true
                )
                updateSettings(updated)
                PrayerTimesAndAdhanManager.saveSettingsAndSchedule(context, updated)
                Toast.makeText(context, "تم تحديث مواقيت الصلاة والقبلة حسب موقعك: $locName", Toast.LENGTH_SHORT).show()
            }
        } else {
            isDetectingLocation = false
            Toast.makeText(context, "يمكنك اختيار مدينتك يدوياً من الإعدادات أو تفعيل صلاحية الموقع", Toast.LENGTH_LONG).show()
        }
    }

    fun triggerAutoLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            isDetectingLocation = true
            PrayerTimesAndAdhanManager.detectLocationAutomatically(context) { lat, lon, locName, _ ->
                isDetectingLocation = false
                val updated = settings.copy(
                    latitude = lat,
                    longitude = lon,
                    locationName = locName,
                    isAutoLocation = true
                )
                updateSettings(updated)
                PrayerTimesAndAdhanManager.saveSettingsAndSchedule(context, updated)
                Toast.makeText(context, "تم تحديد الموقع تلقائياً: $locName", Toast.LENGTH_SHORT).show()
            }
        } else {
            locationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Schedule 5 daily prayers on initial composition and auto-detect location if already permitted
    LaunchedEffect(Unit) {
        PrayerTimesAndAdhanManager.scheduleAllFivePrayers(context, settings)
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if ((hasFine || hasCoarse) && settings.isAutoLocation) {
            PrayerTimesAndAdhanManager.detectLocationAutomatically(context) { lat, lon, locName, success ->
                if (success) {
                    val updated = settings.copy(latitude = lat, longitude = lon, locationName = locName)
                    updateSettings(updated)
                    PrayerTimesAndAdhanManager.saveSettingsAndSchedule(context, updated)
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
            .testTag("prayer_times_adhan_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Title + Auto-Location Button
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RoyalNavyDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مواقيت الصلاة والأذان (٥ فروض)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = settings.locationName,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Auto Location GPS Button
                OutlinedButton(
                    onClick = { triggerAutoLocation() },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = RoyalNavyDark.copy(alpha = 0.6f),
                        contentColor = GoldAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .testTag("auto_detect_location_button")
                ) {
                    if (isDetectingLocation) {
                        CircularProgressIndicator(
                            color = GoldAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "تحديد الموقع تلقائياً",
                            tint = GoldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isDetectingLocation) "جاري التحديد..." else "موقع تلقائي",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Next Prayer Banner (Fixed horizontal layout so clock pill never wraps vertically)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = RoyalNavyDark,
                border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الصلاة القادمة: ${nextPrayerSlot.prayer.iconEmoji} أذان ${nextPrayerSlot.prayer.arabicName}",
                            color = GoldAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "$remainingText • (${nextPrayerSlot.alertMode.titleAr})",
                            color = Color(0xFFFDF8EE).copy(alpha = 0.88f),
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen,
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.75f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = nextPrayerSlot.formatted12Hour(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 5 Daily Prayers Expressive Time-of-Day Sky Showcase (3 Top + 2 Bottom)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    prayerSlots.take(3).forEach { slot ->
                        PrayerAtmosphericCard(
                            slot = slot,
                            isNext = slot.prayer == nextPrayerSlot.prayer,
                            onClick = {
                                selectedPrayerForEdit = slot.prayer
                                showSettingsDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("prayer_slot_${slot.prayer.id}")
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    prayerSlots.drop(3).forEach { slot ->
                        PrayerAtmosphericCard(
                            slot = slot,
                            isNext = slot.prayer == nextPrayerSlot.prayer,
                            onClick = {
                                selectedPrayerForEdit = slot.prayer
                                showSettingsDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("prayer_slot_${slot.prayer.id}")
                        )
                    }
                }
            }

            // Bottom Action Row: Preview Adhan Sound & Open Full Adhan Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasNotif = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasNotif) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        selectedPrayerForEdit = null
                        showSettingsDialog = true
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .heightIn(min = 42.dp)
                        .testTag("open_adhan_settings_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ضبط صوت الأذان والموقع",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (isPlayingPreview) {
                            MihrabiAlarmManager.stopRingtone()
                            isPlayingPreview = false
                        } else {
                            PrayerTimesAndAdhanManager.playAdhanAudio(
                                context = context,
                                mode = if (settings.globalAlertMode == AdhanAlertMode.OFF)
                                    AdhanAlertMode.FULL_ADHAN
                                else
                                    settings.globalAlertMode,
                                customUri = settings.customAdhanUri
                            )
                            isPlayingPreview = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("preview_adhan_card_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isPlayingPreview) MaterialTheme.colorScheme.error else GoldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isPlayingPreview) MaterialTheme.colorScheme.error else GoldAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingPreview) Icons.Default.StopCircle else Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isPlayingPreview) "إيقاف الأذان" else "سماع الأذان",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        AdhanSettingsDialog(
            initialSettings = settings,
            focusedPrayer = selectedPrayerForEdit,
            onRequestAutoLocation = { triggerAutoLocation() },
            onDismiss = {
                MihrabiAlarmManager.stopRingtone()
                isPlayingPreview = false
                showSettingsDialog = false
            },
            onSave = { updated ->
                MihrabiAlarmManager.stopRingtone()
                isPlayingPreview = false
                updateSettings(updated)
                PrayerTimesAndAdhanManager.saveSettingsAndSchedule(context, updated)
                showSettingsDialog = false
                Toast.makeText(context, "تم حفظ إعدادات الأذان وجدولة المواقيت الخمسة بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdhanSettingsDialog(
    initialSettings: AdhanSettingsState,
    focusedPrayer: PrayerKey?,
    onRequestAutoLocation: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (AdhanSettingsState) -> Unit
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(initialSettings) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            MihrabiAlarmManager.stopRingtone()
        }
    }

    val customAdhanFileLauncher = rememberLauncherForActivityResult(
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
            var displayName = "ملف أذان مخصص من الهاتف"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx) ?: displayName
                        }
                    }
                }
            } catch (_: Exception) {
            }
            val updatedModes = draft.perPrayerModes.mapValues { AdhanAlertMode.CUSTOM_UPLOADED }
            draft = draft.copy(
                globalAlertMode = AdhanAlertMode.CUSTOM_UPLOADED,
                customAdhanUri = uri.toString(),
                customAdhanFileName = displayName,
                perPrayerModes = updatedModes
            )
            Toast.makeText(context, "تم اختيار صوت الأذان المخصص: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إعدادات الأذان ومواقيت الصلاة الخمسة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = "تحكم في موقعك الجغرافي ونوع صوت الأذان (كامل / مختصر / إشعار فقط / نغمة مخصصة)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = GoldAccent)
                    }
                }

                HorizontalDivider(color = GoldBorder.copy(alpha = 0.4f))

                // Section 1: Location & Calculation Method
                Text(
                    text = "١. تحديد الموقع وطريقة الحساب الفلكي:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GoldAccent
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onRequestAutoLocation()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديد الموقع أوتوماتيك (GPS)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Preset City Dropdown (for quick manual override if needed)
                ExposedDropdownMenuBox(
                    expanded = cityDropdownExpanded,
                    onExpandedChange = { cityDropdownExpanded = !cityDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = draft.locationName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المدينة الحالية (أو اختر مدينة يدوياً)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = cityDropdownExpanded,
                        onDismissRequest = { cityDropdownExpanded = false }
                    ) {
                        PrayerTimesAndAdhanManager.presetCities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.nameAr, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    draft = draft.copy(
                                        locationName = city.nameAr,
                                        latitude = city.latitude,
                                        longitude = city.longitude,
                                        isAutoLocation = false
                                    )
                                    cityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Calculation Method Dropdown
                ExposedDropdownMenuBox(
                    expanded = methodDropdownExpanded,
                    onExpandedChange = { methodDropdownExpanded = !methodDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = draft.calculationMethod.titleAr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الهيئة المعتمدة لحساب المواقيت") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = methodDropdownExpanded,
                        onDismissRequest = { methodDropdownExpanded = false }
                    ) {
                        CalculationMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.titleAr, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    draft = draft.copy(calculationMethod = method)
                                    methodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = GoldBorder.copy(alpha = 0.4f))

                // Section 2: Global Adhan Mode Selection
                Text(
                    text = "٢. اختيار نوع الأذان ونغمة التنبيه للصلوات الخمس:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GoldAccent
                )

                AdhanAlertMode.values().forEach { mode ->
                    val isSelected = draft.globalAlertMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) EmeraldGreen else GoldBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val updatedMap = draft.perPrayerModes.mapValues { mode }
                                draft = draft.copy(
                                    globalAlertMode = mode,
                                    perPrayerModes = updatedMap
                                )
                                if (mode == AdhanAlertMode.CUSTOM_UPLOADED && draft.customAdhanUri.isBlank()) {
                                    customAdhanFileLauncher.launch(arrayOf("audio/*"))
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        val updatedMap = draft.perPrayerModes.mapValues { mode }
                                        draft = draft.copy(
                                            globalAlertMode = mode,
                                            perPrayerModes = updatedMap
                                        )
                                        if (mode == AdhanAlertMode.CUSTOM_UPLOADED && draft.customAdhanUri.isBlank()) {
                                            customAdhanFileLauncher.launch(arrayOf("audio/*"))
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.titleAr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val sub = if (mode == AdhanAlertMode.CUSTOM_UPLOADED && draft.customAdhanFileName.isNotBlank()) {
                                        "الملف المرفوع: ${draft.customAdhanFileName}"
                                    } else {
                                        mode.descriptionAr
                                    }
                                    Text(
                                        text = sub,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (mode == AdhanAlertMode.CUSTOM_UPLOADED) {
                                OutlinedButton(
                                    onClick = { customAdhanFileLauncher.launch(arrayOf("audio/*")) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, GoldAccent),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 6.dp)
                                        .heightIn(min = 32.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("اختيار ملف صوتي / مختصر أذان من الهاتف", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Live Preview Button inside Dialog
                OutlinedButton(
                    onClick = {
                        if (isPreviewPlaying) {
                            MihrabiAlarmManager.stopRingtone()
                            isPreviewPlaying = false
                        } else {
                            PrayerTimesAndAdhanManager.playAdhanAudio(
                                context = context,
                                mode = if (draft.globalAlertMode == AdhanAlertMode.OFF) AdhanAlertMode.FULL_ADHAN else draft.globalAlertMode,
                                customUri = draft.customAdhanUri
                            )
                            isPreviewPlaying = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isPreviewPlaying) MaterialTheme.colorScheme.error else GoldAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.StopCircle else Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = if (isPreviewPlaying) MaterialTheme.colorScheme.error else GoldAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPreviewPlaying) "إيقاف معاينة صوت الأذان" else "معاينة وسماع صوت الأذان المختار الآن",
                        color = if (isPreviewPlaying) MaterialTheme.colorScheme.error else GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = GoldBorder.copy(alpha = 0.4f))

                // Section 3: Per-Prayer Customization Table
                Text(
                    text = "٣. تخصيص كل صلاة على حدة (اضغط لتبديل وضع الصلاة):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GoldAccent
                )

                val calculatedSlots = remember(draft) {
                    PrayerTimesAndAdhanManager.calculateDailyPrayerTimes(draft)
                }

                calculatedSlots.forEach { slot ->
                    val currentMode = draft.perPrayerModes[slot.prayer] ?: draft.globalAlertMode
                    val isHighlighted = focusedPrayer == slot.prayer

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHighlighted) GoldAccent.copy(alpha = 0.15f) else RoyalNavyDark.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = if (isHighlighted) 1.5.dp else 0.7.dp,
                            color = if (isHighlighted) GoldAccent else GoldBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val allModes = AdhanAlertMode.values()
                                val nextIdx = (allModes.indexOf(currentMode) + 1) % allModes.size
                                val nextMode = allModes[nextIdx]
                                val updatedMap = draft.perPrayerModes.toMutableMap().apply {
                                    this[slot.prayer] = nextMode
                                }
                                draft = draft.copy(perPrayerModes = updatedMap)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${slot.prayer.iconEmoji} ${slot.prayer.arabicName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GoldAccent
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = slot.formatted12Hour(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = EmeraldGreen
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentMode == AdhanAlertMode.OFF)
                                    Color.Gray.copy(alpha = 0.25f)
                                else
                                    EmeraldGreen.copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, if (currentMode == AdhanAlertMode.OFF) Color.Gray else EmeraldGreen)
                            ) {
                                Text(
                                    text = currentMode.titleAr,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentMode == AdhanAlertMode.OFF) Color.LightGray else EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Save & Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onSave(draft) },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("save_adhan_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتفعيل الأذان", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Expressive Time-of-Day Sky Card for each of the 5 Daily Prayers:
 * - الفجر (Fajr): Rose-violet dawn horizon with rising morning sun & dawn star.
 * - الظهر (Dhuhr): Radiant azure midday sky with high golden sun & rays.
 * - العصر (Asr): Warm amber-gold afternoon sky with descending golden sun.
 * - المغرب (Maghrib): Crimson-purple sunset horizon with sun dipping below horizon.
 * - العشاء (Isha): Deep indigo starry night sky with luminous golden crescent moon.
 */
@Composable
private fun PrayerAtmosphericCard(
    slot: PrayerTimeSlot,
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOff = slot.alertMode == AdhanAlertMode.OFF

    val skyGradientColors = remember(slot.prayer) {
        when (slot.prayer) {
            PrayerKey.FAJR -> listOf(
                Color(0xFF1A1F4A),
                Color(0xFF5D3A6A),
                Color(0xFFE69168)
            )
            PrayerKey.DHUHR -> listOf(
                Color(0xFF0A4B78),
                Color(0xFF1976D2),
                Color(0xFF56CCF2)
            )
            PrayerKey.ASR -> listOf(
                Color(0xFF6B2D14),
                Color(0xFFC2641E),
                Color(0xFFF0B44C)
            )
            PrayerKey.MAGHRIB -> listOf(
                Color(0xFF241138),
                Color(0xFF8C284E),
                Color(0xFFE86A48)
            )
            PrayerKey.ISHA -> listOf(
                Color(0xFF060B18),
                Color(0xFF0F1E3D),
                Color(0xFF1B335F)
            )
        }
    }

    val borderColor = when {
        isNext -> Color(0xFF4ADE80)
        isOff -> Color.White.copy(alpha = 0.25f)
        else -> GoldAccent.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .height(114.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(skyGradientColors))
            .border(
                width = if (isNext) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        // Custom Canvas Sky & Celestial Art (Sun / Horizon / Crescent Moon & Stars)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (slot.prayer) {
                PrayerKey.FAJR -> {
                    // Dawn horizon silhouette + rising morning sun + dawn star
                    val sunCenter = Offset(w * 0.5f, h * 0.64f)
                    val sunRadius = 15.dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFE082).copy(alpha = 0.28f),
                        radius = sunRadius * 1.7f,
                        center = sunCenter
                    )
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = sunRadius,
                        center = sunCenter
                    )
                    // Horizon glow line
                    drawLine(
                        color = Color(0xFFFFCC80).copy(alpha = 0.65f),
                        start = Offset(w * 0.12f, h * 0.68f),
                        end = Offset(w * 0.88f, h * 0.68f),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Morning star in upper sky
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.2.dp.toPx(),
                        center = Offset(w * 0.22f, h * 0.24f)
                    )
                }

                PrayerKey.DHUHR -> {
                    // Bright Zenith Midday Sun with 8 golden rays
                    val sunCenter = Offset(w * 0.5f, h * 0.48f)
                    val sunRadius = 14.dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFF59D).copy(alpha = 0.30f),
                        radius = sunRadius * 1.85f,
                        center = sunCenter
                    )
                    for (deg in 0 until 360 step 45) {
                        val rad = Math.toRadians(deg.toDouble())
                        val rStart = sunRadius * 1.2f
                        val rEnd = sunRadius * 1.65f
                        drawLine(
                            color = Color(0xFFFFEE58).copy(alpha = 0.85f),
                            start = Offset(
                                (sunCenter.x + cos(rad) * rStart).toFloat(),
                                (sunCenter.y + sin(rad) * rStart).toFloat()
                            ),
                            end = Offset(
                                (sunCenter.x + cos(rad) * rEnd).toFloat(),
                                (sunCenter.y + sin(rad) * rEnd).toFloat()
                            ),
                            strokeWidth = 1.8.dp.toPx()
                        )
                    }
                    drawCircle(
                        color = Color(0xFFFFF176),
                        radius = sunRadius,
                        center = sunCenter
                    )
                }

                PrayerKey.ASR -> {
                    // Warm Golden Afternoon Sun tilted slightly toward horizon
                    val sunCenter = Offset(w * 0.55f, h * 0.54f)
                    val sunRadius = 15.dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFCC80).copy(alpha = 0.32f),
                        radius = sunRadius * 1.75f,
                        center = sunCenter
                    )
                    drawCircle(
                        color = Color(0xFFFFB74D),
                        radius = sunRadius,
                        center = sunCenter
                    )
                    // Soft warm cloud bands
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(w * 0.18f, h * 0.58f),
                        end = Offset(w * 0.48f, h * 0.58f),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                PrayerKey.MAGHRIB -> {
                    // Sunset crimson sun dipping below the horizon line
                    val horizonY = h * 0.64f
                    val sunCenter = Offset(w * 0.5f, horizonY)
                    val sunRadius = 16.dp.toPx()
                    drawCircle(
                        color = Color(0xFFFF8A65).copy(alpha = 0.32f),
                        radius = sunRadius * 1.7f,
                        center = sunCenter
                    )
                    drawArc(
                        color = Color(0xFFFFAB40),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(sunCenter.x - sunRadius, sunCenter.y - sunRadius),
                        size = Size(sunRadius * 2, sunRadius * 2)
                    )
                    // Sunset horizon line
                    drawLine(
                        color = Color(0xFFFFCC80).copy(alpha = 0.8f),
                        start = Offset(w * 0.10f, horizonY),
                        end = Offset(w * 0.90f, horizonY),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                PrayerKey.ISHA -> {
                    // Starry night sky with golden Crescent Moon
                    val moonCenter = Offset(w * 0.52f, h * 0.48f)
                    val moonR = 13.dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFE082).copy(alpha = 0.22f),
                        radius = moonR * 1.65f,
                        center = moonCenter
                    )
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = moonR,
                        center = moonCenter
                    )
                    // Cutout circle to form crescent moon
                    drawCircle(
                        color = Color(0xFF0E1B38),
                        radius = moonR * 0.85f,
                        center = Offset(moonCenter.x - moonR * 0.42f, moonCenter.y - moonR * 0.22f)
                    )
                    // Twinkling stars
                    drawCircle(Color.White.copy(alpha = 0.85f), radius = 1.8.dp.toPx(), center = Offset(w * 0.22f, h * 0.30f))
                    drawCircle(Color.White.copy(alpha = 0.70f), radius = 1.4.dp.toPx(), center = Offset(w * 0.78f, h * 0.28f))
                    drawCircle(Color(0xFFFFE082).copy(alpha = 0.85f), radius = 1.9.dp.toPx(), center = Offset(w * 0.30f, h * 0.56f))
                    drawCircle(Color.White.copy(alpha = 0.75f), radius = 1.5.dp.toPx(), center = Offset(w * 0.75f, h * 0.54f))
                }
            }
        }

        // Top-to-bottom subtle dark scrim at edges so text and clock are ultra-legible
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Prayer Name + Sound/Notification Status Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slot.prayer.arabicName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1
                )

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.38f)
                ) {
                    Icon(
                        imageVector = when (slot.alertMode) {
                            AdhanAlertMode.FULL_ADHAN -> Icons.Default.VolumeUp
                            AdhanAlertMode.SHORT_ADHAN -> Icons.Default.VolumeDown
                            AdhanAlertMode.NOTIFICATION_ONLY -> Icons.Default.Notifications
                            AdhanAlertMode.CUSTOM_UPLOADED -> Icons.Default.AudioFile
                            AdhanAlertMode.ISLAMIC_NASHEED -> Icons.Default.MusicNote
                            AdhanAlertMode.OFF -> Icons.Default.VolumeOff
                        },
                        contentDescription = slot.alertMode.titleAr,
                        tint = if (isOff) Color.LightGray else if (isNext) Color(0xFF4ADE80) else GoldAccent,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp)
                    )
                }
            }

            // Bottom Horizontal Clock Pill (Guaranteed single-line display)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isNext) EmeraldGreen.copy(alpha = 0.92f) else Color(0xFF0B1426).copy(alpha = 0.78f),
                border = BorderStroke(
                    width = 0.9.dp,
                    color = if (isNext) Color(0xFF4ADE80) else GoldAccent.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = slot.formatted12Hour(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

