package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GoldAccent

/**
 * Premium Islamic Logo Component for "محرابي" (Mihrabi).
 * Presents the newly refined official artwork featuring the worshipper, Quran, and prayer beads
 * inside the sacred Mihrab with "مِحْرَابِي" curved gracefully at the arch apex.
 * Framed in a sleek cyber-gold metallic ring with radiant spiritual luminescence.
 */
@Composable
fun MihrabiLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showIntegratedText: Boolean = true, // maintained for API compatibility
    animatedGlow: Boolean = false
) {
    val goldRing = Brush.sweepGradient(
        colors = listOf(
            Color(0xFFFFEEAA),
            GoldAccent,
            Color(0xFFE5A93C),
            Color(0xFFB38324),
            Color(0xFFFFEEAA)
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mihrabi_glow")
    val glowAlpha by if (animatedGlow) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )
    } else {
        rememberInfiniteTransition(label = "static").animateFloat(
            initialValue = 0.5f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(tween(1000)),
            label = "static_alpha"
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("mihrabi_logo_component"),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = size < 50.dp

        // Outer Radiant Cyber-Gold Aura
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GoldAccent.copy(alpha = glowAlpha * 0.45f),
                                Color(0xFF00FFC4).copy(alpha = glowAlpha * 0.12f),
                                Color.Transparent
                            )
                        ),
                        radius = this.size.minDimension / 1.7f
                    )
                }
        )

        // Main Circular Emblem Body
        Box(
            modifier = Modifier
                .fillMaxSize(if (isCompact) 0.98f else 0.92f)
                .clip(CircleShape)
                .background(Color(0xFF081535))
                .border(
                    width = if (isCompact) 1.5.dp else 2.5.dp,
                    brush = goldRing,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "شعار محرابي",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}
