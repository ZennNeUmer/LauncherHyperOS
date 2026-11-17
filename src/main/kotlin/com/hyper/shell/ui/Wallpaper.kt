package com.hyper.shell.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

/**
 * The default Hyper Shell wallpaper is generated on the GPU with Compose, so the
 * launcher looks good out of the box and needs no image assets.
 */
@Composable
fun DefaultWallpaper(accent: Color, animate: Boolean = true, modifier: Modifier = Modifier) {
    val phase: Float = if (animate) {
        val transition = rememberInfiniteTransition(label = "wallpaper")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(18000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wallpaper-phase"
        )
        value
    } else {
        0.35f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF0A1330),
                    Color(0xFF111A3F),
                    Color(0xFF231B4D),
                    Color(0xFF0A1024)
                )
            )
        )

        val primaryCenter = Offset(
            x = w * (0.08f + 0.24f * phase),
            y = h * (0.16f + 0.05f * phase)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.72f),
                    accent.copy(alpha = 0.24f),
                    Color.Transparent
                ),
                center = primaryCenter,
                radius = w * 0.95f
            ),
            radius = w * 0.95f,
            center = primaryCenter
        )

        val pinkCenter = Offset(
            x = w * (0.92f - 0.16f * phase),
            y = h * (0.60f + 0.08f * phase)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF3B81).copy(alpha = 0.30f),
                    Color.Transparent
                ),
                center = pinkCenter,
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = pinkCenter
        )

        val cyanCenter = Offset(
            x = w * (0.55f + 0.20f * phase),
            y = h * (0.92f - 0.10f * phase)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF25D0FF).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = cyanCenter,
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = cyanCenter
        )

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.20f),
                0.35f to Color.Transparent,
                0.78f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.52f)
            )
        )
    }
}

/**
 * Bundled fallback wallpaper. Coil can load it through the android.resource URI,
 * so the same code path (crossfade, scrim) works for user-picked images.
 */
const val BUNDLED_WALLPAPER = "android.resource://com.hyper.shell/drawable/wallpaper_default"

@Composable
fun WallpaperBackground(
    uri: String?,
    accent: Color,
    animate: Boolean,
    scrim: Float = 0.0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val source = uri?.takeIf { it.isNotBlank() } ?: BUNDLED_WALLPAPER
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF04050A))) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(source)
                .crossfade(350)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                else -> DefaultWallpaper(accent = accent, animate = false)
            }
        }

        if (scrim > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = scrim * 0.75f),
                                Color.Black.copy(alpha = scrim * 0.45f),
                                Color.Black.copy(alpha = scrim)
                            )
                        )
                    )
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.22f),
                        0.18f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.28f)
                    )
                )
        )
    }
}
