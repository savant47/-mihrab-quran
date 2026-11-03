package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LocalKhatmaStorage
import com.example.ui.components.MihrabiLogo
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldLight
import com.example.ui.theme.RoyalNavyDark

/**
 * Luxury Islamic Chic Entry Screen (واجهة دخول محرابي الأنيقة).
 * Built completely without login, operating 100% offline with instant local cache storage.
 */
@Composable
fun AuthScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onGoogleSignInClicked: (Activity) -> Unit = {},
    onGuestSignInClicked: () -> Unit = {},
    onEnterClicked: ((readerName: String, dailyGoalPages: Int, skipFuture: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }

    val savedUser = remember { localStorage.guestUserFlow.value }
    var readerNameInput by remember { mutableStateOf(savedUser.displayName.ifBlank { "القارئ المبارك" }) }
    var selectedGoalPages by remember { mutableIntStateOf(localStorage.dailyGoalPages) }
    var skipWelcomeCheckbox by remember { mutableStateOf(localStorage.skipWelcomeScreen) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF031411), // Deep Islamic Emerald Green
            Color(0xFF061E1A),
            Color(0xFF04121B), // Midnight Blue
            Color(0xFF02090F)
        )
    )

    val goldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFF2C2),
            Color(0xFFFFD54F),
            GoldAccent,
            Color(0xFFB38324)
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "entry_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val handleEnter = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val finalName = readerNameInput.trim().ifBlank { "القارئ المبارك" }
        localStorage.updateReaderProfile(finalName, selectedGoalPages)
        localStorage.skipWelcomeScreen = skipWelcomeCheckbox
        localStorage.hasEnteredBefore = true
        localStorage.isGuestMode = true

        if (onEnterClicked != null) {
            onEnterClicked(finalName, selectedGoalPages, skipWelcomeCheckbox)
        } else {
            onGuestSignInClicked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("chic_welcome_entry_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Subtle Islamic Arch & Ambient Golden Halo in Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldAccent.copy(alpha = 0.14f * pulseAlpha),
                        EmeraldGreen.copy(alpha = 0.06f * pulseAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.28f),
                    radius = w * 0.8f
                )
            )

            val archBg = Path().apply {
                moveTo(w * 0.06f, h * 0.85f)
                lineTo(w * 0.06f, h * 0.28f)
                cubicTo(
                    w * 0.06f, h * 0.12f,
                    w * 0.35f, h * 0.04f,
                    w * 0.50f, h * 0.02f
                )
                cubicTo(
                    w * 0.65f, h * 0.04f,
                    w * 0.94f, h * 0.12f,
                    w * 0.94f, h * 0.28f
                )
                lineTo(w * 0.94f, h * 0.85f)
            }

            drawPath(
                path = archBg,
                color = GoldAccent.copy(alpha = 0.07f),
                style = Stroke(width = 1.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Main Illuminated Emblem
            MihrabiLogo(
                size = 110.dp,
                showIntegratedText = true,
                animatedGlow = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Majestic Calligraphic Title
            Text(
                text = "محرابي",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                lineHeight = 48.sp,
                style = TextStyle(brush = goldGradient),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Spiritual Slogan
            Text(
                text = "وِردُكَ.. خلوتُكَ.. ومحرابُك",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = GoldLight,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Inspiring Holy Ayah Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldBorder.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x28061E1A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "﴿إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ وَيُبَشِّرُ الْمُؤْمِنِينَ﴾",
                        fontFamily = AmiriFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "سورة الإسراء - آية 9",
                        fontFamily = AmiriFontFamily,
                        fontSize = 12.sp,
                        color = EmeraldGreen.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Reader Personalization Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, GoldBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF04181E).copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalNavyDark,
                            border = BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مرحباً بك في خلوتك القرآنية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Text(
                                text = "خصص اسمك المبارك وهدف وردك اليومي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reader Name Input Field
                    OutlinedTextField(
                        value = readerNameInput,
                        onValueChange = { readerNameInput = it },
                        label = { Text("اسم القارئ المبارك") },
                        placeholder = { Text("مثال: عبد الرحمن / القارئ المبارك") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = GoldAccent
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = GoldBorder.copy(alpha = 0.7f),
                            focusedLabelColor = GoldAccent,
                            cursorColor = GoldAccent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "الهدف اليومي المفضل للتلاوة:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Daily Goal Options Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val goals = listOf(
                            5 to "ربع حزب\n(5 صفحات)",
                            10 to "نصف حزب\n(10 صفحات)",
                            20 to "جزء كامل\n(20 صفحة)",
                            0 to "تلاوة حرة\n(بدون تقييد)"
                        )

                        goals.forEach { (pages, label) ->
                            val isSelected = selectedGoalPages == pages
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedGoalPages = pages
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) GoldAccent else GoldBorder.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) EmeraldGreen.copy(alpha = 0.25f) else Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GoldAccent else Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Local Cache Guarantee Note
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "حفظ فوري على كاش وذاكرة الهاتف",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                                Text(
                                    text = "يعمل 100% بدون إنترنت ودون أي حساب أو تسجيل دخول",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Show active checkpoint preview if previously read
                    if (savedUser.currentPage > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RoyalNavyDark.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "آخر نقطة توقف محفوظة:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldAccent,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "سورة ${savedUser.currentSurah} • آية ${savedUser.currentAyah} (صفحة ${savedUser.currentPage} من 604)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Luxurious Action Button: دخول المحراب
            Button(
                onClick = handleEnter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.2.dp, GoldAccent, RoundedCornerShape(16.dp))
                    .testTag("enter_mihrabi_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalNavyDark,
                    contentColor = GoldAccent
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "۞",
                        color = GoldAccent,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (savedUser.currentPage > 1) "متابعة التلاوة في المحراب" else "دخول المحراب وبدء التلاوة",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = AmiriFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Checkbox: Skip Welcome Screen directly to Mus-haf next time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { skipWelcomeCheckbox = !skipWelcomeCheckbox }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Checkbox(
                    checked = skipWelcomeCheckbox,
                    onCheckedChange = { skipWelcomeCheckbox = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldGreen,
                        checkmarkColor = Color.White,
                        uncheckedColor = GoldAccent.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "الدخول المباشر للمصحف مستقبلاً (تخطي شاشة الدخول)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dedication and Waqf Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "إعداد آسر الشربيني",
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GoldLight.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "وقف لله تعالى - لا تنسونا من خالص الدعاء لأبي وأمي",
                    fontFamily = AmiriFontFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
