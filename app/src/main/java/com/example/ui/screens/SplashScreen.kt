package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MihrabiLogo
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight

/**
 * Redesigned Luxury Islamic Splash Screen for "محرابي" (Mihrabi).
 * Features Deep Emerald Green & Midnight Blue background, glowing Cyber-Gold accents,
 * prominent Arabic typography, and the serene motto: "وِردُكَ.. خلوتُكَ.. ومحرابُك".
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val goldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFF2C2),
            Color(0xFFFFD54F),
            GoldAccent,
            Color(0xFFB38324)
        )
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF031411), // Deep Islamic Emerald Green
            Color(0xFF061E1A),
            Color(0xFF04121B), // Midnight Blue
            Color(0xFF02090F)
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("mihrabi_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Sacred Islamic Geometry & Cyber-Gold Light Rays
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Center Ambient Cyber-Gold Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldAccent.copy(alpha = 0.15f * pulseAlpha),
                        Color(0xFF00FFB2).copy(alpha = 0.05f * pulseAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = w * 0.75f
                )
            )

            // Delicate Islamic Arch Silhouette in background
            val archBg = Path().apply {
                moveTo(w * 0.08f, h * 0.78f)
                lineTo(w * 0.08f, h * 0.35f)
                cubicTo(
                    w * 0.08f, h * 0.18f,
                    w * 0.35f, h * 0.08f,
                    w * 0.50f, h * 0.05f
                )
                cubicTo(
                    w * 0.65f, h * 0.08f,
                    w * 0.92f, h * 0.18f,
                    w * 0.92f, h * 0.35f
                )
                lineTo(w * 0.92f, h * 0.78f)
            }

            drawPath(
                path = archBg,
                color = GoldAccent.copy(alpha = 0.08f),
                style = Stroke(width = 1.5f)
            )
        }

        // Central Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Mihrabi Emblem
            MihrabiLogo(
                size = 144.dp,
                showIntegratedText = true,
                animatedGlow = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Prominent Title: محرابي
            Text(
                text = "محرابي",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 52.sp,
                style = TextStyle(brush = goldGradient),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Serene Subtitle: وِردُكَ.. خلوتُكَ.. ومحرابُك
            Text(
                text = "وِردُكَ.. خلوتُكَ.. ومحرابُك",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = GoldLight,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Decorative Divider with Glowing Islamic Star
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .alpha(pulseAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldAccent.copy(alpha = 0.6f))
                            )
                        )
                )

                Text(
                    text = " ۞ ",
                    color = GoldAccent,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldAccent.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Sleek Cyber-Gold Progress Indicator
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = GoldAccent,
                strokeWidth = 2.5.dp,
                trackColor = Color(0x33D4AF37)
            )
        }

        // Bottom Attribution & Dedication
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "إعداد آسر الشربيني",
                fontFamily = AmiriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GoldLight.copy(alpha = 0.85f),
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
