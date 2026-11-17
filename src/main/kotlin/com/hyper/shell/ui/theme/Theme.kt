package com.hyper.shell.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val HyperBlue = Color(0xFF3482FF)
val HyperPurple = Color(0xFF8B5CF6)
val HyperGreen = Color(0xFF22C55E)
val HyperOrange = Color(0xFFFF7A1A)
val HyperPink = Color(0xFFFF3B81)
val HyperBg = Color(0xFF05060B)
val TextPrimary = Color(0xFFF4F6FF)
val TextSecondary = Color(0xFF9AA3B8)

val AccentChoices = listOf(HyperBlue, HyperPurple, HyperGreen, HyperOrange, HyperPink)

private val HyperTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Thin,
        fontSize = 82.sp,
        letterSpacing = (-4).sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 54.sp,
        letterSpacing = (-2).sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = TextPrimary
    )
)

@Composable
fun HyperTheme(accent: Color = HyperBlue, content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent,
        onSecondary = Color.White,
        background = HyperBg,
        onBackground = TextPrimary,
        surface = Color(0xFF10131E),
        onSurface = TextPrimary,
        surfaceVariant = Color(0xFF171B29),
        onSurfaceVariant = TextSecondary,
        outline = Color.White.copy(alpha = 0.18f)
    )
    MaterialTheme(
        colorScheme = colors,
        typography = HyperTypography,
        content = content
    )
}

fun Modifier.glass(
    shape: Shape = RoundedCornerShape(26.dp),
    alpha: Float = 0.10f,
    borderAlpha: Float = 0.16f
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = alpha + 0.045f),
                Color.White.copy(alpha = alpha * 0.45f)
            )
        ),
        shape
    )
    .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)

/**
 * Tactile press animation without pulling in Material ripples. Long press is
 * optional and triggers haptic feedback automatically.
 */
@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    // Recomposition must not restart the pointer detector: the old keys included
    // freshly allocated lambdas, so every frame cancelled and recreated the gesture.
    val clickState = rememberUpdatedState(onClick)
    val longClickState = rememberUpdatedState(onLongClick)
    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    scale.animateTo(0.9f, tween(80))
                    tryAwaitRelease()
                    scale.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = 620f))
                },
                onLongPress = {
                    longClickState.value?.let {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
                onTap = { clickState.value() }
            )
        }
}
