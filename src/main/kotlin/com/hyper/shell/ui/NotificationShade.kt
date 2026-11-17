package com.hyper.shell.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hyper.shell.core.LauncherSettings
import com.hyper.shell.core.NotifItem
import com.hyper.shell.core.NotificationCenter
import com.hyper.shell.ui.components.AppIcon
import com.hyper.shell.ui.components.GlassButton
import com.hyper.shell.ui.components.GlassIconButton
import com.hyper.shell.ui.theme.TextPrimary
import com.hyper.shell.ui.theme.TextSecondary
import com.hyper.shell.ui.theme.bounceClick
import com.hyper.shell.ui.theme.glass
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NotificationShadePanel(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    accent: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by vm.notifications.collectAsState()
    val now = rememberNow()
    val battery = rememberBattery()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pull by remember { mutableFloatStateOf(0f) }
    val hasAccess = vm.hasNotificationAccess()

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationX = pull }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        if (dragAmount > 0f) {
                            change.consume()
                            pull = (pull + dragAmount).coerceAtLeast(0f)
                        }
                    },
                    onDragEnd = {
                        if (pull > 100.dp.toPx()) {
                            onClose()
                        } else {
                            scope.launch {
                                animate(
                                    pull,
                                    0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                ) { v, _ ->
                                    pull = v
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            animate(
                                pull,
                                0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) { v, _ ->
                                pull = v
                            }
                        }
                    }
                )
            }
    ) {
        WallpaperBackground(
            uri = settings.shadeWallpaper ?: settings.homeWallpaper,
            accent = accent,
            animate = settings.animations,
            scrim = (settings.shadeBlur / 100f).coerceIn(0.35f, 0.85f)
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.28f),
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.30f)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ShadeHeader(
                    now = now,
                    battery = battery,
                    onClose = onClose
                )
            }

            item { QuickToggles(vm = vm, accent = accent) }

            item { BrightnessCard(vm = vm, accent = accent) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Уведомления",
                        color = TextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (notifications.any { it.clearable }) {
                        Text(
                            text = "Очистить",
                            color = accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .bounceClick { NotificationCenter.clearAll() }
                                .padding(8.dp)
                        )
                    }
                }
            }

            if (!hasAccess) {
                item { NotificationAccessCard(vm = vm, accent = accent) }
            } else if (notifications.isEmpty()) {
                item { EmptyNotifications() }
            } else {
                items(notifications, key = { it.key }) { item ->
                    SwipeNotification(
                        item = item,
                        accent = accent,
                        onClick = { NotificationCenter.open(item, context) },
                        onDismiss = { NotificationCenter.dismiss(item) }
                    )
                }
            }

            item {
                Text(
                    text = "Свайп по уведомлению — удалить\nСвайп вправо по панели — закрыть",
                    color = TextSecondary.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ShadeHeader(
    now: java.time.LocalDateTime,
    battery: BatteryInfo,
    onClose: () -> Unit
) {
    Column(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatClock(now),
                    color = TextPrimary,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = (-3).sp,
                    lineHeight = 64.sp
                )
                Text(
                    text = formatDateRu(now),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .glass(shape = RoundedCornerShape(50), alpha = 0.09f)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (battery.charging) Icons.Rounded.BatteryChargingFull
                        else Icons.Rounded.BatteryFull,
                        contentDescription = null,
                        tint = if (battery.charging) Color(0xFF4ADE80) else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${battery.level}%",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Закрыть",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun QuickToggles(vm: LauncherViewModel, accent: Color) {
    val context = LocalContext.current
    val connectivity = remember { context.getSystemService(ConnectivityManager::class.java) }
    var torchOn by remember { mutableStateOf(TorchController.isOn) }
    var dndOn by remember { mutableStateOf(isDndEnabled(context)) }
    var rotationOn by remember { mutableStateOf(isRotationEnabled(context)) }

    val wifiActive = runCatching {
        val manager = connectivity ?: return@runCatching false
        val network = manager.activeNetwork ?: return@runCatching false
        manager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }.getOrDefault(false)

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            torchOn = TorchController.toggle(context)
        } else {
            vm.toast("Для фонарика нужно разрешение на камеру")
        }
    }

    fun toggleTorch() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            torchOn = TorchController.toggle(context)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun openWifi() {
        val action = if (Build.VERSION.SDK_INT >= 29) {
            AndroidSettings.Panel.ACTION_INTERNET_CONNECTIVITY
        } else {
            AndroidSettings.ACTION_WIFI_SETTINGS
        }
        openSystemScreen(context, action, AndroidSettings.ACTION_WIFI_SETTINGS)
    }

    fun openBluetooth() {
        if (Build.VERSION.SDK_INT >= 33) {
            openSystemScreen(
                context,
                "android.settings.BLUETOOTH_CONNECTIVITY",
                AndroidSettings.ACTION_BLUETOOTH_SETTINGS
            )
        } else {
            openSystemScreen(context, AndroidSettings.ACTION_BLUETOOTH_SETTINGS, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
        }
    }

    fun toggleDnd() {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        if (manager == null || !manager.isNotificationPolicyAccessGranted) {
            vm.toast("Разрешите доступ к режиму «Не беспокоить»")
            vm.openNotificationPolicyAccess()
            return
        }
        val next = !dndOn
        val result = runCatching {
            manager.setInterruptionFilter(
                if (next) android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
        if (result.isSuccess) dndOn = next else vm.toast("Не удалось изменить режим")
    }

    fun toggleRotation() {
        if (!AndroidSettings.System.canWrite(context)) {
            vm.toast("Разрешите Hyper Shell изменять настройки")
            vm.openWriteSettings()
            return
        }
        val next = !rotationOn
        val result = runCatching {
            AndroidSettings.System.putInt(
                context.contentResolver,
                AndroidSettings.System.ACCELEROMETER_ROTATION,
                if (next) 1 else 0
            )
        }
        if (result.isSuccess) rotationOn = next else vm.toast("Не удалось изменить автоповорот")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickTile(
                icon = Icons.Rounded.Wifi,
                label = "Wi‑Fi",
                active = wifiActive,
                accent = accent,
                onClick = { openWifi() },
                modifier = Modifier.weight(1f)
            )
            QuickTile(
                icon = Icons.Rounded.Bluetooth,
                label = "Bluetooth",
                active = false,
                accent = accent,
                onClick = { openBluetooth() },
                modifier = Modifier.weight(1f)
            )
            QuickTile(
                icon = Icons.Rounded.FlashlightOn,
                label = "Фонарик",
                active = torchOn,
                accent = accent,
                onClick = { toggleTorch() },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickTile(
                icon = Icons.Rounded.DoNotDisturbOn,
                label = "Не беспокоить",
                active = dndOn,
                accent = accent,
                onClick = { toggleDnd() },
                modifier = Modifier.weight(1f)
            )
            QuickTile(
                icon = Icons.Rounded.ScreenRotation,
                label = "Автоповорот",
                active = rotationOn,
                accent = accent,
                onClick = { toggleRotation() },
                modifier = Modifier.weight(1f)
            )
            QuickTile(
                icon = Icons.Rounded.Settings,
                label = "Настройки",
                active = false,
                accent = accent,
                onClick = { vm.openSettings() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(82.dp)
            .glass(
                shape = RoundedCornerShape(23.dp),
                alpha = if (active) 0.22f else 0.075f,
                borderAlpha = if (active) 0.40f else 0.12f
            )
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) accent else TextPrimary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (active) accent else TextPrimary.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BrightnessCard(vm: LauncherViewModel, accent: Color) {
    val context = LocalContext.current
    val canWrite = AndroidSettings.System.canWrite(context)
    var value by remember { mutableFloatStateOf(readBrightness(context)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(shape = RoundedCornerShape(23.dp), alpha = 0.09f)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Brightness6,
            contentDescription = null,
            tint = TextPrimary.copy(alpha = 0.9f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = {
                if (canWrite) {
                    runCatching {
                        AndroidSettings.System.putInt(
                            context.contentResolver,
                            AndroidSettings.System.SCREEN_BRIGHTNESS,
                            (value * 255f).roundToInt().coerceIn(10, 255)
                        )
                    }
                } else {
                    vm.toast("Нужно разрешение на изменение яркости")
                    vm.openWriteSettings()
                }
            },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            )
        )
        if (!canWrite) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Нет разрешения",
                tint = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = "${(value * 100).roundToInt()}%",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.width(34.dp)
        )
    }
}

@Composable
private fun NotificationAccessCard(vm: LauncherViewModel, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(shape = RoundedCornerShape(24.dp), alpha = 0.09f)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.NotificationsOff,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                "Нет доступа к уведомлениям",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            "Разрешите Hyper Shell читать уведомления, чтобы показывать их здесь.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        GlassButton(
            text = "Открыть настройки доступа",
            onClick = { vm.openNotificationAccess() },
            accent = accent,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun EmptyNotifications() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(shape = RoundedCornerShape(24.dp), alpha = 0.07f)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.NotificationsActive,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Пока тихо", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "Новые уведомления появятся здесь",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SwipeNotification(
    item: NotifItem,
    accent: Color,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var offsetX by remember(item.key) { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFB4232A).copy(alpha = 0.85f),
                            Color(0xFFE5484D)
                        )
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Удалить",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 22.dp)
                    .size(22.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .glass(shape = RoundedCornerShape(24.dp), alpha = 0.095f)
                .pointerInput(item.key) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-700f, 0f)
                        },
                        onDragEnd = {
                            if (offsetX < -90.dp.toPx()) {
                                onDismiss()
                            } else {
                                scope.launch {
                                    animate(
                                        offsetX,
                                        0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                    ) { v, _ ->
                                        offsetX = v
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animate(
                                offsetX,
                                0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) { v, _ ->
                                    offsetX = v
                                }
                            }
                        }
                    )
                }
                .bounceClick(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top
        ) {
            AppIcon(
                icon = item.icon,
                label = item.appLabel,
                size = 40.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.appLabel,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatNotificationTime(item.time),
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
                if (item.text.isNotBlank()) {
                    Text(
                        text = item.text,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun readBrightness(context: Context): Float {
    val value = runCatching {
        AndroidSettings.System.getInt(
            context.contentResolver,
            AndroidSettings.System.SCREEN_BRIGHTNESS,
            128
        )
    }.getOrDefault(128)
    return (value / 255f).coerceIn(0.03f, 1f)
}

private fun isRotationEnabled(context: Context): Boolean = runCatching {
    AndroidSettings.System.getInt(
        context.contentResolver,
        AndroidSettings.System.ACCELEROMETER_ROTATION,
        0
    ) == 1
}.getOrDefault(false)

private fun isDndEnabled(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(android.app.NotificationManager::class.java)
    manager != null &&
        manager.currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
}.getOrDefault(false)

private fun openSystemScreen(context: Context, action: String, fallback: String) {
    val opened = runCatching {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
    if (!opened) {
        runCatching {
            context.startActivity(Intent(fallback).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private object TorchController {
    private var cameraId: String? = null
    var isOn by mutableStateOf(false)
        private set

    fun toggle(context: Context): Boolean {
        val manager = context.getSystemService(CameraManager::class.java) ?: return isOn
        val id = cameraId ?: findCameraWithFlash(manager)?.also { cameraId = it } ?: return isOn
        val next = !isOn
        return runCatching {
            manager.setTorchMode(id, next)
            isOn = next
            next
        }.getOrElse { isOn }
    }

    private fun findCameraWithFlash(manager: CameraManager): String? = runCatching {
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()
}
