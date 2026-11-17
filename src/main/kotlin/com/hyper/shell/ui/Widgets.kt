package com.hyper.shell.ui

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.hyper.shell.HyperShellApp
import com.hyper.shell.core.WidgetConfig
import com.hyper.shell.core.WidgetKind
import com.hyper.shell.ui.components.GlassCard
import com.hyper.shell.ui.theme.TextPrimary
import com.hyper.shell.ui.theme.TextSecondary
import com.hyper.shell.ui.theme.bounceClick
import com.hyper.shell.ui.theme.glass
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun WidgetFrame(
    config: WidgetConfig,
    editing: Boolean,
    canvasWidthDp: Float,
    canvasHeightDp: Float,
    accent: Color,
    onUpdate: (WidgetConfig) -> Unit,
    onRemove: (WidgetConfig) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    var x by remember(config.id) { mutableFloatStateOf(config.x) }
    var y by remember(config.id) { mutableFloatStateOf(config.y) }
    var w by remember(config.id) { mutableFloatStateOf(config.w) }
    var h by remember(config.id) { mutableFloatStateOf(config.h) }
    var interacting by remember { mutableStateOf(false) }

    LaunchedEffect(config.id, config.x, config.y, config.w, config.h) {
        if (!interacting) {
            x = config.x
            y = config.y
            w = config.w
            h = config.h
        }
    }

    val minW = when (config.kind) {
        WidgetKind.CLOCK -> 220f
        WidgetKind.SEARCH -> 170f
        WidgetKind.BATTERY, WidgetKind.NOTIFICATIONS -> 118f
        WidgetKind.APPWIDGET -> 150f
    }
    val minH = when (config.kind) {
        WidgetKind.CLOCK -> 128f
        WidgetKind.SEARCH -> 58f
        WidgetKind.BATTERY, WidgetKind.NOTIFICATIONS -> 104f
        WidgetKind.APPWIDGET -> 110f
    }

    fun commit() {
        onUpdate(config.copy(x = x, y = y, w = w, h = h))
    }

    fun clampPosition() {
        x = x.coerceIn(0f, max(0f, canvasWidthDp - w))
        y = y.coerceIn(0f, max(0f, canvasHeightDp - h))
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(x.dp.roundToPx(), y.dp.roundToPx()) }
            .size(w.dp, h.dp)
            .zIndex(if (interacting) 30f else 2f)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp))
        ) {
            content()
        }

        if (editing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(accent.copy(alpha = 0.045f))
                    .border(1.5.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
                    .pointerInput(config.id, w, h) {
                        detectDragGestures(
                            onDragStart = {
                                interacting = true
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                x += with(density) { drag.x.toDp().value }
                                y += with(density) { drag.y.toDp().value }
                                clampPosition()
                            },
                            onDragEnd = {
                                interacting = false
                                clampPosition()
                                commit()
                            },
                            onDragCancel = {
                                interacting = false
                                x = config.x
                                y = config.y
                                commit()
                            }
                        )
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .background(Color(0xFFE5484D), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .bounceClick { onRemove(config) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Удалить виджет",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            ResizeHandle(
                alignment = Alignment.BottomEnd,
                accent = accent,
                onStart = { interacting = true },
                onDragDelta = { delta ->
                    w = (w + with(density) { delta.x.toDp().value })
                        .coerceIn(minW, max(minW, canvasWidthDp - x))
                    h = (h + with(density) { delta.y.toDp().value })
                        .coerceIn(minH, max(minH, canvasHeightDp - y))
                },
                onEnd = {
                    interacting = false
                    clampPosition()
                    commit()
                }
            )
            ResizeHandle(
                alignment = Alignment.BottomCenter,
                accent = accent,
                onStart = { interacting = true },
                onDragDelta = { delta ->
                    h = (h + with(density) { delta.y.toDp().value })
                        .coerceIn(minH, max(minH, canvasHeightDp - y))
                },
                onEnd = {
                    interacting = false
                    clampPosition()
                    commit()
                }
            )
            ResizeHandle(
                alignment = Alignment.CenterEnd,
                accent = accent,
                onStart = { interacting = true },
                onDragDelta = { delta ->
                    w = (w + with(density) { delta.x.toDp().value })
                        .coerceIn(minW, max(minW, canvasWidthDp - x))
                },
                onEnd = {
                    interacting = false
                    clampPosition()
                    commit()
                }
            )
        }
    }
}

@Composable
private fun BoxScope.ResizeHandle(
    alignment: Alignment,
    accent: Color,
    onStart: () -> Unit,
    onDragDelta: (Offset) -> Unit,
    onEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(7.dp)
            .size(22.dp)
            .background(accent, CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onStart() },
                    onDrag = { change, drag ->
                        change.consume()
                        onDragDelta(drag)
                    },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onEnd() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(5.dp).background(Color.White, CircleShape))
    }
}

@Composable
fun HomeWidgetContent(
    config: WidgetConfig,
    notificationsCount: Int,
    accent: Color,
    onOpenSearch: () -> Unit,
    onOpenShade: () -> Unit
) {
    when (config.kind) {
        WidgetKind.CLOCK -> ClockWidget(accent)
        WidgetKind.BATTERY -> BatteryWidget(accent)
        WidgetKind.SEARCH -> SearchWidget(accent, onOpenSearch)
        WidgetKind.NOTIFICATIONS -> NotificationsWidget(accent, notificationsCount, onOpenShade)
        WidgetKind.APPWIDGET -> AndroidWidget(config)
    }
}

@Composable
private fun ClockWidget(accent: Color) {
    val now = rememberNow()
    val greeting = when (now.hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .glass(shape = RoundedCornerShape(26.dp), alpha = 0.10f)
            .padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        val timeSize = (maxHeight.value * 0.40f).coerceIn(36f, 68f).sp
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(accent, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = greeting,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                )
            }
            Text(
                text = formatClock(now),
                color = TextPrimary,
                fontSize = timeSize,
                fontWeight = FontWeight.Thin,
                letterSpacing = (-3).sp,
                lineHeight = (timeSize.value + 2).sp
            )
            Text(
                text = formatDateRu(now),
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BatteryWidget(accent: Color) {
    val battery = rememberBattery()
    val tint = if (battery.charging) Color(0xFF4ADE80) else accent

    Row(
        modifier = Modifier
            .fillMaxSize()
            .glass(shape = RoundedCornerShape(26.dp), alpha = 0.09f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2f
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 360f * battery.level / 100f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Icon(
                imageVector = if (battery.charging) Icons.Rounded.BatteryChargingFull
                else Icons.Rounded.BatteryFull,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Column {
            Text(
                text = "${battery.level}%",
                color = TextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = if (battery.charging) "Зарядка" else "Батарея",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SearchWidget(accent: Color, onOpenSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .glass(shape = RoundedCornerShape(23.dp), alpha = 0.10f)
            .bounceClick(onClick = onOpenSearch)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(11.dp))
        Text(
            text = "Поиск приложений",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Все",
            color = accent.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NotificationsWidget(accent: Color, count: Int, onOpenShade: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glass(shape = RoundedCornerShape(26.dp), alpha = 0.09f)
            .bounceClick(onClick = onOpenShade)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = if (count > 0) accent else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .background(
                        if (count > 0) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.07f),
                        CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = if (count > 0) accent else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Column {
            Text(
                text = "Уведомления",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (count == 0) "Нет новых" else "Нажмите, чтобы открыть",
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AndroidWidget(config: WidgetConfig) {
    val context = LocalContext.current
    val host = remember { (context.applicationContext as HyperShellApp).widgetHost }
    val manager = remember { AppWidgetManager.getInstance(context) }
    val info = remember(config.appWidgetId) {
        runCatching { manager.getAppWidgetInfo(config.appWidgetId) }.getOrNull()
    }

    if (info == null) {
        Box(
            Modifier
                .fillMaxSize()
                .glass(shape = RoundedCornerShape(26.dp), alpha = 0.08f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Виджет недоступен", color = TextPrimary, fontSize = 13.sp)
                Text(
                    "Удалите его в режиме редактирования",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            host.createView(ctx, config.appWidgetId, info).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            if (view is AppWidgetHostView) {
                val width = config.w.roundToInt()
                val height = config.h.roundToInt()
                runCatching {
                    view.updateAppWidgetSize(null, width, height, width, height)
                }
            }
        }
    )
}
