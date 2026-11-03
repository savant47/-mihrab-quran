package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alarm.PrayerTimesAndAdhanManager
import com.example.ui.components.PrayerTimesAndAdhanCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import java.util.Locale
import kotlin.math.*

private const val KAABA_LAT = 21.422487
private const val KAABA_LON = 39.826206

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerAndQiblaScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var settings by remember { mutableStateOf(PrayerTimesAndAdhanManager.loadSettings(context)) }
    var isDetectingGps by remember { mutableStateOf(false) }
    var showOfflineCitySheet by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Sensor state
    var hasCompassSensor by remember { mutableStateOf(true) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var continuousDeviceAzimuth by remember { mutableFloatStateOf(0f) }
    var manualAzimuthOffset by remember { mutableFloatStateOf(0f) }

    val qiblaBearing = remember(settings.latitude, settings.longitude) {
        calculateQiblaBearing(settings.latitude, settings.longitude).toFloat()
    }
    val distanceToKaabaKm = remember(settings.latitude, settings.longitude) {
        calculateDistanceToKaabaKm(settings.latitude, settings.longitude).roundToInt()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            isDetectingGps = true
            PrayerTimesAndAdhanManager.detectLocationAutomatically(context) { lat, lon, locName, _ ->
                isDetectingGps = false
                val updated = settings.copy(
                    latitude = lat,
                    longitude = lon,
                    locationName = locName,
                    isAutoLocation = true
                )
                settings = updated
                PrayerTimesAndAdhanManager.saveSettingsAndSchedule(context, updated)
                Toast.makeText(context, "تم تحديد موقعك وتحديث اتجاه القبلة ومواقيت الصلاة: $locName", Toast.LENGTH_SHORT).show()
            }
        } else {
            isDetectingGps = false
            Toast.makeText(
                context,
                "يمكنك اختيار مدينتك بدون إنترنت من زر (اختيار المدينة أوفلاين) لتحديد القبلة والمواقيت بدقة",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestGpsLocation() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fine || coarse
        if (hasLocationPermission) {
            isDetectingGps = true
            PrayerTimesAndAdhanManager.detectLocationAutomatically(context) { lat, lon, locName, _ ->
                isDetectingGps = false
                val updated = settings.copy(
                    latitude = lat,
                    longitude = lon,
                    locationName = locName,
                    isAutoLocation = true
                )
                settings = updated
                PrayerTimesAndAdhanManager.saveSettingsAndSchedule(context, updated)
                Toast.makeText(context, "تم تحديث موقع القبلة والمواقيت: $locName", Toast.LENGTH_SHORT).show()
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Register hardware orientation / compass sensors (works 100% offline)
    DisposableEffect(settings.latitude, settings.longitude) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val sensorAvailable = rotationVectorSensor != null || (accelerometer != null && magnetometer != null)
        hasCompassSensor = sensorAvailable

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val geomagneticField = GeomagneticField(
            settings.latitude.toFloat(),
            settings.longitude.toFloat(),
            0f,
            System.currentTimeMillis()
        )
        val magneticDeclination = geomagneticField.declination

        fun updateContinuousAzimuth(newRawDeg: Float) {
            val trueNorthAzimuth = ((newRawDeg + magneticDeclination) % 360f + 360f) % 360f
            val currentMod = ((continuousDeviceAzimuth % 360f) + 360f) % 360f
            var delta = trueNorthAzimuth - currentMod
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            // Low-pass filter to keep arrow movement silky smooth
            if (abs(delta) > 0.35f) {
                continuousDeviceAzimuth += delta * 0.22f
            }
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        updateContinuousAzimuth(azimuthDeg)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val alpha = 0.85f
                        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                        hasGravity = true
                        if (hasGeomagnetic && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                            updateContinuousAzimuth(azimuthDeg)
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val alpha = 0.85f
                        geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                        geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                        geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
                        hasGeomagnetic = true
                        if (hasGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                            updateContinuousAzimuth(azimuthDeg)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    sensorAccuracy = accuracy
                }
            }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val animatedAzimuth by animateFloatAsState(
        targetValue = continuousDeviceAzimuth + manualAzimuthOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "qibla_azimuth"
    )

    val normalizedHeading = ((animatedAzimuth % 360f) + 360f) % 360f
    // Relative angle from device top (0 deg) to Qibla in [-180, +180]
    val relativeQiblaDelta = remember(qiblaBearing, normalizedHeading) {
        var diff = qiblaBearing - normalizedHeading
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        diff
    }

    val isAlignedWithQibla = abs(relativeQiblaDelta) <= 5.5f

    // Trigger gentle haptic pulse when user aligns with the Qibla
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAlignedWithQibla) {
        if (isAlignedWithQibla && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = isAlignedWithQibla
    }

    val archHighlightColor by animateColorAsState(
        targetValue = if (isAlignedWithQibla) EmeraldGreen else GoldAccent,
        label = "mihrab_highlight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sacred Mihrab Qibla Compass Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isAlignedWithQibla) 2.dp else 1.dp,
                    color = archHighlightColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("qibla_mihrab_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row: Title + Sensor/GPS Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(RoyalNavyDark)
                                .border(1.dp, GoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "محراب القبلة المشرفة",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Text(
                                text = "يعمل أوفلاين وأونلاين عبر مستشعرات الجهاز والـ GPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasCompassSensor) EmeraldGreen.copy(alpha = 0.16f) else GoldAccent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, if (hasCompassSensor) EmeraldGreen else GoldAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasCompassSensor) Icons.Default.Sensors else Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (hasCompassSensor) EmeraldGreen else GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasCompassSensor) "المستشعر نشط" else "ضبط يدوي",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasCompassSensor) EmeraldGreen else GoldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location & Permissions Action Bar (GPS + Offline City Selector)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RoyalNavyDark.copy(alpha = 0.7f),
                    border = BorderStroke(0.8.dp, GoldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = settings.locationName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "المسافة للكعبة المشرفة: $distanceToKaabaKm كم",
                                        color = GoldAccent.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { requestGpsLocation() },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .testTag("qibla_gps_permission_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                if (isDetectingGps) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(15.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isDetectingGps) "جاري تحديد الموقع..." else "تحديد الموقع (GPS)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { showOfflineCitySheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .testTag("qibla_offline_city_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GoldAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اختيار مدينة (أوفلاين)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Large Animated Direction Instruction Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isAlignedWithQibla)
                        EmeraldGreen.copy(alpha = 0.22f)
                    else
                        RoyalNavyDark,
                    border = BorderStroke(
                        width = if (isAlignedWithQibla) 2.dp else 1.dp,
                        color = if (isAlignedWithQibla) EmeraldGreen else GoldAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val directionInstruction = when {
                            isAlignedWithQibla -> "أنت الآن باتجاه القبلة المشرفة 🕋"
                            relativeQiblaDelta > 0 -> "اتجه بالهاتف لليمين ${relativeQiblaDelta.roundToInt()}° ↱"
                            else -> "اتجه بالهاتف لليسار ${abs(relativeQiblaDelta).roundToInt()}° ↰"
                        }
                        Text(
                            text = directionInstruction,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAlignedWithQibla) EmeraldGreen else GoldAccent,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAlignedWithQibla)
                                "تقبل الله طاعتكم وصلاتكم — المحراب مضيء باتجاه الكعبة المشرفة"
                            else
                                "أمسك الهاتف بشكل مسطح وأدره حتى ينطبق سهم القبلة الذهبي مع قمة المحراب",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reverent Architectural Mihrab & Animated Qibla Compass Graphic
                Box(
                    modifier = Modifier
                        .size(310.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MihrabQiblaCompassCanvas(
                        deviceHeadingDeg = normalizedHeading,
                        qiblaBearingDeg = qiblaBearing,
                        isAligned = isAlignedWithQibla,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Large Telemetry Badges: Qibla Angle from North & Current Phone Heading
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RoyalNavyDark,
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "زاوية القبلة من الشمال",
                                fontSize = 11.sp,
                                color = GoldAccent.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${qiblaBearing.roundToInt()}°",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccent
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RoyalNavyDark,
                        border = BorderStroke(
                            1.dp,
                            if (isAlignedWithQibla) EmeraldGreen else GoldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "اتجاه هاتفك الحالي",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${normalizedHeading.roundToInt()}° ${cardinalNameAr(normalizedHeading)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAlignedWithQibla) EmeraldGreen else Color.White
                            )
                        }
                    }
                }

                // Sensor Calibration Note or Manual Rotation Assist
                if (!hasCompassSensor || sensorAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldAccent.copy(alpha = 0.1f),
                        border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (!hasCompassSensor)
                                    "جهازك لا يحتوي على مستشعر بوصلة مغناطيسي — يمكنك تدوير البوصلة يدوياً لمحاكاة الشمال:"
                                else
                                    "لزيادة دقة البوصلة: حرك الهاتف في الهواء على شكل رقم (8) أو استخدم المعايرة اليدوية:",
                                fontSize = 11.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Slider(
                                value = manualAzimuthOffset,
                                onValueChange = { manualAzimuthOffset = it },
                                valueRange = -180f..180f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = EmeraldGreen
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Full 5 Daily Prayer Times & Adhan Settings Card (Moved from Home Tab)
        PrayerTimesAndAdhanCard(
            externalSettings = settings,
            onExternalSettingsChanged = { updated ->
                settings = updated
            }
        )
    }

    if (showOfflineCitySheet) {
        AlertDialog(
            onDismissRequest = { showOfflineCitySheet = false },
            title = {
                Text(
                    text = "اختيار المدينة لحساب القبلة والمواقيت (بدون إنترنت)",
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PrayerTimesAndAdhanManager.presetCities.forEach { city ->
                        val isSelected = settings.locationName == city.nameAr
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldGreen.copy(alpha = 0.2f) else RoyalNavyDark.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.7.dp,
                                color = if (isSelected) EmeraldGreen else GoldBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val updated = settings.copy(
                                    latitude = city.latitude,
                                    longitude = city.longitude,
                                    locationName = city.nameAr,
                                    isAutoLocation = false
                                )
                                settings = updated
                                PrayerTimesAndAdhanManager.saveSettingsAndSchedule(context, updated)
                                showOfflineCitySheet = false
                                Toast.makeText(
                                    context,
                                    "تم ضبط اتجاه القبلة ومواقيت الصلاة على: ${city.nameAr}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = city.nameAr,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) EmeraldGreen else Color.White,
                                    fontSize = 13.5.sp
                                )
                                val cityQibla = calculateQiblaBearing(city.latitude, city.longitude).roundToInt()
                                Text(
                                    text = "القبلة $cityQibla°",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOfflineCitySheet = false }) {
                    Text("إغلاق", color = GoldAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Custom Reverent Islamic Mihrab & Compass Canvas with a prominent animated Qibla Arrow.
 */
@Composable
private fun MihrabQiblaCompassCanvas(
    deviceHeadingDeg: Float,
    qiblaBearingDeg: Float,
    isAligned: Boolean,
    modifier: Modifier = Modifier
) {
    val relativeQiblaRotation = qiblaBearingDeg - deviceHeadingDeg

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerRadius = min(cx, cy) * 0.94f

        // 1. Deep Sacred Mihrab Night Background Circle with Golden & Emerald Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (isAligned) Color(0xFF0E3B2E) else Color(0xFF101D35),
                    Color(0xFF070E1A)
                ),
                center = Offset(cx, cy),
                radius = outerRadius
            ),
            radius = outerRadius,
            center = Offset(cx, cy)
        )

        // Outer ornamental double ring
        drawCircle(
            color = if (isAligned) EmeraldGreen else GoldAccent,
            radius = outerRadius,
            center = Offset(cx, cy),
            style = Stroke(width = if (isAligned) 4.dp.toPx() else 2.5.dp.toPx())
        )
        drawCircle(
            color = GoldAccent.copy(alpha = 0.35f),
            radius = outerRadius * 0.92f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )

        // 2. Stationary Sacred Mihrab Arch (محراب الصلاة) inside the compass
        drawSacredMihrabArch(
            cx = cx,
            cy = cy,
            radius = outerRadius * 0.72f,
            isAligned = isAligned
        )

        // 3. Rotating Compass Dial (rotates by -deviceHeadingDeg so North points to real North)
        rotate(degrees = -deviceHeadingDeg, pivot = Offset(cx, cy)) {
            val tickOuter = outerRadius * 0.90f
            for (deg in 0 until 360 step 5) {
                val rad = Math.toRadians((deg - 90).toDouble())
                val isCardinal = deg % 90 == 0
                val isMajor = deg % 30 == 0
                val tickLen = when {
                    isCardinal -> 14.dp.toPx()
                    isMajor -> 9.dp.toPx()
                    else -> 4.5.dp.toPx()
                }
                val startR = tickOuter - tickLen
                val sx = cx + (startR * cos(rad)).toFloat()
                val sy = cy + (startR * sin(rad)).toFloat()
                val ex = cx + (tickOuter * cos(rad)).toFloat()
                val ey = cy + (tickOuter * sin(rad)).toFloat()

                drawLine(
                    color = when {
                        deg == 0 -> EmeraldGreen
                        isCardinal -> GoldAccent
                        isMajor -> GoldAccent.copy(alpha = 0.75f)
                        else -> Color.White.copy(alpha = 0.28f)
                    },
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = if (isCardinal) 3.dp.toPx() else if (isMajor) 1.8.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw Cardinal Arabic Labels (ش، شر، ج، غ)
            val labelRadius = outerRadius * 0.76f
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 14.sp.toPx()
                isFakeBoldText = true
            }

            val cardinals = listOf(
                Triple(0, "ش", EmeraldGreen),
                Triple(90, "شر", GoldAccent),
                Triple(180, "ج", GoldAccent),
                Triple(270, "غ", GoldAccent)
            )
            for ((deg, label, col) in cardinals) {
                val rad = Math.toRadians((deg - 90).toDouble())
                val lx = cx + (labelRadius * cos(rad)).toFloat()
                val ly = cy + (labelRadius * sin(rad)).toFloat() + 5.dp.toPx()
                paint.color = android.graphics.Color.argb(
                    255,
                    (col.red * 255).toInt(),
                    (col.green * 255).toInt(),
                    (col.blue * 255).toInt()
                )
                drawContext.canvas.nativeCanvas.drawText(label, lx, ly, paint)
            }
        }

        // 4. Rotating Large Prominent Qibla Arrow & Kaaba Emblem (points directly to relativeQiblaRotation)
        rotate(degrees = relativeQiblaRotation, pivot = Offset(cx, cy)) {
            val kaabaCenterY = cy - outerRadius * 0.80f

            // Glowing aura around Kaaba indicator on the rim
            drawCircle(
                color = if (isAligned) EmeraldGreen.copy(alpha = 0.45f) else GoldAccent.copy(alpha = 0.28f),
                radius = 22.dp.toPx(),
                center = Offset(cx, kaabaCenterY)
            )

            // Draw stylized Kaaba Cube icon at the tip of the Qibla bearing
            val cubeSize = 20.dp.toPx()
            drawRoundRect(
                color = Color(0xFF111111),
                topLeft = Offset(cx - cubeSize / 2f, kaabaCenterY - cubeSize / 2f),
                size = Size(cubeSize, cubeSize),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = GoldAccent,
                topLeft = Offset(cx - cubeSize / 2f, kaabaCenterY - cubeSize / 2f),
                size = Size(cubeSize, cubeSize),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            // Golden Kiswah band on the Kaaba
            drawLine(
                color = GoldAccent,
                start = Offset(cx - cubeSize / 2f + 2.dp.toPx(), kaabaCenterY - cubeSize * 0.18f),
                end = Offset(cx + cubeSize / 2f - 2.dp.toPx(), kaabaCenterY - cubeSize * 0.18f),
                strokeWidth = 3.dp.toPx()
            )

            // Large Bold Qibla Arrow Shaft & Spearhead
            val arrowTipY = cy - outerRadius * 0.62f
            val arrowTailY = cy + outerRadius * 0.46f
            val headWidth = 28.dp.toPx()
            val headHeight = 36.dp.toPx()
            val shaftHalfWidth = 6.dp.toPx()

            val arrowColor = if (isAligned) EmeraldGreen else GoldAccent

            // Outer glow behind the arrow
            drawLine(
                color = arrowColor.copy(alpha = 0.3f),
                start = Offset(cx, arrowTailY),
                end = Offset(cx, arrowTipY + headHeight * 0.4f),
                strokeWidth = 20.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Arrow Shaft
            val shaftPath = Path().apply {
                moveTo(cx - shaftHalfWidth, arrowTipY + headHeight * 0.7f)
                lineTo(cx + shaftHalfWidth, arrowTipY + headHeight * 0.7f)
                lineTo(cx + shaftHalfWidth * 1.4f, arrowTailY)
                lineTo(cx, arrowTailY - 10.dp.toPx())
                lineTo(cx - shaftHalfWidth * 1.4f, arrowTailY)
                close()
            }
            drawPath(
                path = shaftPath,
                brush = Brush.verticalGradient(
                    colors = listOf(arrowColor, GoldAccent),
                    startY = arrowTipY,
                    endY = arrowTailY
                )
            )

            // Large Ornate Islamic Spearhead pointing to Qibla
            val headPath = Path().apply {
                moveTo(cx, arrowTipY)
                lineTo(cx + headWidth, arrowTipY + headHeight)
                lineTo(cx, arrowTipY + headHeight * 0.72f)
                lineTo(cx - headWidth, arrowTipY + headHeight)
                close()
            }
            drawPath(
                path = headPath,
                color = arrowColor
            )
            drawPath(
                path = headPath,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
            )
        }

        // 5. Center Pivot Medallion (سرة المحراب)
        drawCircle(
            color = RoyalNavyDark,
            radius = 16.dp.toPx(),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = if (isAligned) EmeraldGreen else GoldAccent,
            radius = 16.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = if (isAligned) EmeraldGreen else GoldAccent,
            radius = 6.dp.toPx(),
            center = Offset(cx, cy)
        )

        // 6. Top Fixed Mihrab Crown Pointer (marks the top of the user's phone)
        val topCrownY = cy - outerRadius - 2.dp.toPx()
        val crownPath = Path().apply {
            moveTo(cx, topCrownY + 14.dp.toPx())
            lineTo(cx - 10.dp.toPx(), topCrownY)
            lineTo(cx + 10.dp.toPx(), topCrownY)
            close()
        }
        drawPath(
            path = crownPath,
            color = if (isAligned) EmeraldGreen else GoldAccent
        )
    }
}

/**
 * Draws a reverent architectural Mihrab arch (قوس المحراب وأعمدته ومشكاته) inside the compass.
 */
private fun DrawScope.drawSacredMihrabArch(
    cx: Float,
    cy: Float,
    radius: Float,
    isAligned: Boolean
) {
    val archColor = if (isAligned) EmeraldGreen else GoldAccent
    val archWidth = radius * 1.12f
    val archHeight = radius * 1.38f
    val leftX = cx - archWidth / 2f
    val rightX = cx + archWidth / 2f
    val bottomY = cy + archHeight * 0.46f
    val springY = cy - archHeight * 0.06f
    val apexY = cy - archHeight * 0.52f

    // Outer Pointed Islamic Mihrab Arch Path
    val outerArchPath = Path().apply {
        moveTo(leftX, bottomY)
        lineTo(leftX, springY)
        cubicTo(
            leftX, apexY + archHeight * 0.16f,
            cx - archWidth * 0.18f, apexY + archHeight * 0.04f,
            cx, apexY
        )
        cubicTo(
            cx + archWidth * 0.18f, apexY + archHeight * 0.04f,
            rightX, apexY + archHeight * 0.16f,
            rightX, springY
        )
        lineTo(rightX, bottomY)
        close()
    }

    drawPath(
        path = outerArchPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                archColor.copy(alpha = if (isAligned) 0.25f else 0.12f),
                Color.Transparent
            ),
            startY = apexY,
            endY = bottomY
        )
    )

    drawPath(
        path = outerArchPath,
        color = archColor.copy(alpha = 0.65f),
        style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round)
    )

    // Inner recessed Mihrab niche arch
    val innerMargin = 10.dp.toPx()
    val innerArchPath = Path().apply {
        moveTo(leftX + innerMargin, bottomY - innerMargin * 0.5f)
        lineTo(leftX + innerMargin, springY + innerMargin * 0.4f)
        cubicTo(
            leftX + innerMargin, apexY + archHeight * 0.22f,
            cx - archWidth * 0.14f, apexY + innerMargin * 1.3f,
            cx, apexY + innerMargin * 1.1f
        )
        cubicTo(
            cx + archWidth * 0.14f, apexY + innerMargin * 1.3f,
            rightX - innerMargin, apexY + archHeight * 0.22f,
            rightX - innerMargin, springY + innerMargin * 0.4f
        )
        lineTo(rightX - innerMargin, bottomY - innerMargin * 0.5f)
    }

    drawPath(
        path = innerArchPath,
        color = GoldAccent.copy(alpha = 0.35f),
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Hanging Mosque Lamp (مشكاة المحراب) from the apex of the arch
    val lampChainBottom = apexY + archHeight * 0.24f
    drawLine(
        color = GoldAccent.copy(alpha = 0.5f),
        start = Offset(cx, apexY + innerMargin * 1.1f),
        end = Offset(cx, lampChainBottom),
        strokeWidth = 1.5.dp.toPx()
    )
    drawCircle(
        color = archColor.copy(alpha = 0.35f),
        radius = 9.dp.toPx(),
        center = Offset(cx, lampChainBottom + 6.dp.toPx())
    )
}

/**
 * Calculates exact Great-Circle Qibla bearing in degrees [0, 360) from True North.
 */
fun calculateQiblaBearing(userLatDeg: Double, userLonDeg: Double): Double {
    val lat1 = Math.toRadians(userLatDeg)
    val lon1 = Math.toRadians(userLonDeg)
    val lat2 = Math.toRadians(KAABA_LAT)
    val lon2 = Math.toRadians(KAABA_LON)

    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val initialBearingRad = atan2(y, x)
    return (Math.toDegrees(initialBearingRad) + 360.0) % 360.0
}

/**
 * Calculates Haversine distance in kilometers between the user's location and the Holy Kaaba.
 */
fun calculateDistanceToKaabaKm(userLatDeg: Double, userLonDeg: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(KAABA_LAT - userLatDeg)
    val dLon = Math.toRadians(KAABA_LON - userLonDeg)
    val a = sin(dLat / 2).pow(2.0) +
        cos(Math.toRadians(userLatDeg)) * cos(Math.toRadians(KAABA_LAT)) *
        sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

private fun cardinalNameAr(deg: Float): String {
    val d = ((deg % 360f) + 360f) % 360f
    return when {
        d >= 337.5f || d < 22.5f -> "شمال"
        d < 67.5f -> "شمال شرق"
        d < 112.5f -> "شرق"
        d < 157.5f -> "جنوب شرق"
        d < 202.5f -> "جنوب"
        d < 247.5f -> "جنوب غرب"
        d < 292.5f -> "غرب"
        else -> "شمال غرب"
    }
}
