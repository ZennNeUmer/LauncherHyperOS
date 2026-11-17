package com.hyper.shell.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hyper.shell.BuildConfig
import com.hyper.shell.core.LauncherSettings
import com.hyper.shell.ui.LauncherViewModel
import com.hyper.shell.ui.WallpaperBackground
import com.hyper.shell.ui.components.DialogTitle
import com.hyper.shell.ui.components.GlassButton
import com.hyper.shell.ui.components.GlassIconButton
import com.hyper.shell.ui.components.HyperDialog
import com.hyper.shell.ui.theme.AccentChoices
import com.hyper.shell.ui.theme.TextPrimary
import com.hyper.shell.ui.theme.TextSecondary
import com.hyper.shell.ui.theme.bounceClick
import com.hyper.shell.ui.theme.glass
import kotlin.math.roundToInt

private enum class SettingsTab(val id: String, val title: String, val icon: ImageVector) {
    APPEARANCE("appearance", "Оформление", Icons.Rounded.Wallpaper),
    GESTURES("gestures", "Жесты", Icons.Rounded.Gesture),
    WIDGETS("widgets", "Виджеты", Icons.Rounded.Widgets),
    APPS("apps", "Приложения", Icons.Rounded.Apps),
    SYSTEM("system", "Система", Icons.Rounded.Settings);

    companion object {
        fun fromId(id: String?): SettingsTab = entries.firstOrNull { it.id == id } ?: APPEARANCE
    }
}

@Composable
fun SettingsScreen(
    vm: LauncherViewModel,
    initialSection: String,
    onClose: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    val accent = Color(settings.accent)
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.toFloat()
    val heightDp = configuration.screenHeightDp.toFloat()
    var sectionId by rememberSaveable { mutableStateOf(initialSection) }
    val section = SettingsTab.fromId(sectionId)

    fun persistWallpaper(uri: Uri, shade: Boolean) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        vm.prefs.update {
            if (shade) it.copy(shadeWallpaper = uri.toString())
            else it.copy(homeWallpaper = uri.toString())
        }
    }

    val homePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { persistWallpaper(it, shade = false) }
    }
    val shadePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { persistWallpaper(it, shade = true) }
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(
            uri = settings.homeWallpaper,
            accent = accent,
            animate = settings.animations,
            scrim = 0.76f
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Назад",
                    onClick = onClose
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Настройки",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp
                    )
                    Text("Hyper Shell", color = TextSecondary, fontSize = 11.sp)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(SettingsTab.entries.toList(), key = { it.id }) { tab ->
                    val active = tab == section
                    Row(
                        modifier = Modifier
                            .glass(
                                shape = RoundedCornerShape(50),
                                alpha = if (active) 0.20f else 0.07f,
                                borderAlpha = if (active) 0.32f else 0.12f
                            )
                            .bounceClick { sectionId = tab.id }
                            .padding(horizontal = 13.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            tint = if (active) accent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tab.title,
                            color = if (active) accent else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = section,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(110)) },
                label = "settings-tab",
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { tab ->
                when (tab) {
                    SettingsTab.APPEARANCE -> AppearanceSection(
                        vm = vm,
                        settings = settings,
                        accent = accent,
                        onPickHome = { homePicker.launch(arrayOf("image/*")) },
                        onPickShade = { shadePicker.launch(arrayOf("image/*")) },
                        onResetHome = { vm.prefs.update { it.copy(homeWallpaper = null) } },
                        onResetShade = { vm.prefs.update { it.copy(shadeWallpaper = null) } }
                    )

                    SettingsTab.GESTURES -> GesturesSection(vm = vm, settings = settings, accent = accent)
                    SettingsTab.WIDGETS -> WidgetsSection(
                        vm = vm,
                        settings = settings,
                        accent = accent,
                        widthDp = widthDp,
                        heightDp = heightDp,
                        onClose = onClose
                    )

                    SettingsTab.APPS -> AppsSection(vm = vm, settings = settings, accent = accent)
                    SettingsTab.SYSTEM -> SystemSection(vm = vm, settings = settings, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    accent: Color,
    onPickHome: () -> Unit,
    onPickShade: () -> Unit,
    onResetHome: () -> Unit,
    onResetShade: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsGroup(
                title = "Обои",
                subtitle = "Главный экран и панель уведомлений используют разные изображения."
            ) {
                WallpaperRow(
                    title = "Главный экран",
                    subtitle = if (settings.homeWallpaper == null) "Базовые обои Hyper Shell"
                    else "Собственное изображение",
                    uri = settings.homeWallpaper,
                    accent = accent,
                    onPick = onPickHome,
                    onReset = onResetHome,
                    onApplySystem = settings.homeWallpaper?.let {
                        { vm.applySystemWallpaper(it) }
                    }
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                WallpaperRow(
                    title = "Панель уведомлений",
                    subtitle = if (settings.shadeWallpaper == null)
                        "Показываются обои главного экрана с затемнением"
                    else "Собственное изображение",
                    uri = settings.shadeWallpaper,
                    accent = accent,
                    onPick = onPickShade,
                    onReset = onResetShade
                )
            }
        }

        item {
            SettingsGroup(title = "Цвет акцента") {
                AccentPicker(
                    selected = accent,
                    choices = AccentChoices,
                    onSelect = { color -> vm.prefs.update { it.copy(accent = color.value.toInt()) } }
                )
            }
        }

        item {
            SettingsGroup(title = "Эффекты") {
                SettingsToggleRow(
                    title = "Плавные анимации",
                    subtitle = "Пружинные переходы панелей и иконок",
                    checked = settings.animations,
                    accent = accent,
                    onCheckedChange = { value -> vm.prefs.update { it.copy(animations = value) } }
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Затемнение панели уведомлений",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${settings.shadeBlur}%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Slider(
                        value = settings.shadeBlur.toFloat(),
                        onValueChange = { value ->
                            vm.prefs.update { it.copy(shadeBlur = value.roundToInt()) }
                        },
                        valueRange = 35f..85f,
                        modifier = Modifier.width(150.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }

        item {
            SettingsGroup(title = "Главный экран") {
                SettingsToggleRow(
                    title = "Док с приложениями",
                    subtitle = "Нижняя стеклянная панель с пятью иконками",
                    checked = settings.showDock,
                    accent = accent,
                    onCheckedChange = { value -> vm.prefs.update { it.copy(showDock = value) } }
                )
                SettingsToggleRow(
                    title = "Подписи приложений",
                    subtitle = "Названия под иконками в меню приложений",
                    checked = settings.showLabels,
                    accent = accent,
                    onCheckedChange = { value -> vm.prefs.update { it.copy(showLabels = value) } }
                )
            }
        }

        item {
            Text(
                text = "Совет: долгое нажатие на пустом месте главного экрана открывает меню оформления и виджетов.",
                color = TextSecondary.copy(alpha = 0.65f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun GesturesSection(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    accent: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsGroup(
                title = "Свайпы",
                subtitle = "Жесты работают поверх виджетов. Их можно отключить по отдельности."
            ) {
                SettingsToggleRow(
                    title = "Меню приложений от правого края",
                    subtitle = "Свайп влево, начиная с правой кромки экрана",
                    checked = settings.edgeDrawer,
                    accent = accent,
                    icon = Icons.Rounded.Apps,
                    onCheckedChange = { value -> vm.prefs.update { it.copy(edgeDrawer = value) } }
                )
                SettingsToggleRow(
                    title = "Панель уведомлений свайпом влево",
                    subtitle = "Свайп влево по главному экрану",
                    checked = settings.swipeLeftShade,
                    accent = accent,
                    icon = Icons.Rounded.NotificationsActive,
                    onCheckedChange = { value -> vm.prefs.update { it.copy(swipeLeftShade = value) } }
                )
                SettingsToggleRow(
                    title = "Меню приложений свайпом вверх",
                    subtitle = "Свайп вверх от нижней части экрана",
                    checked = settings.swipeUpDrawer,
                    accent = accent,
                    icon = Icons.Rounded.Apps,
                    onCheckedChange = { value -> vm.prefs.update { it.copy(swipeUpDrawer = value) } }
                )
                SettingsToggleRow(
                    title = "Уведомления свайпом вниз",
                    subtitle = "Свайп вниз в верхней трети экрана",
                    checked = settings.swipeDownShade,
                    accent = accent,
                    icon = Icons.Rounded.NotificationsActive,
                    onCheckedChange = { value -> vm.prefs.update { it.copy(swipeDownShade = value) } }
                )
            }
        }

        item {
            SettingsGroup(title = "Как закрыть панель") {
                Text(
                    "Меню приложений — свайп вправо от левого края или кнопка «×».\n" +
                        "Панель уведомлений — свайп вправо в любом месте или кнопка «×».\n" +
                        "Системная кнопка «Назад» тоже работает.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SystemSection(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    accent: Color
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var access by remember { mutableStateOf(vm.hasNotificationAccess()) }
    var canWrite by remember { mutableStateOf(AndroidSettings.System.canWrite(context)) }
    var confirmReset by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                access = vm.hasNotificationAccess()
                canWrite = AndroidSettings.System.canWrite(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsGroup(title = "Разрешения") {
                SettingsActionRow(
                    title = if (access) "Доступ к уведомлениям выдан" else "Доступ к уведомлениям",
                    subtitle = if (access) "Панель уведомлений активна"
                    else "Нужен для отображения уведомлений в оболочке",
                    icon = if (access) Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive,
                    accent = accent,
                    onClick = { vm.openNotificationAccess() }
                )
                SettingsActionRow(
                    title = if (canWrite) "Изменение настроек разрешено" else "Яркость и автоповорот",
                    subtitle = if (canWrite) "Ползунок яркости и переключатель поворота доступны"
                    else "Нужно разрешение «Изменение системных настроек»",
                    icon = if (canWrite) Icons.Rounded.CheckCircle else Icons.Rounded.Brightness6,
                    accent = accent,
                    onClick = { vm.openWriteSettings() }
                )
                SettingsActionRow(
                    title = "Hyper Shell по умолчанию",
                    subtitle = "Выберите оболочку в системных настройках",
                    icon = Icons.Rounded.Home,
                    accent = accent,
                    onClick = { vm.openDefaultLauncherSettings() }
                )
            }
        }

        item {
            SettingsGroup(title = "Сброс") {
                SettingsActionRow(
                    title = "Сбросить всё",
                    subtitle = "Обои, жесты, виджеты, скрытые приложения и акцент",
                    icon = Icons.Rounded.Refresh,
                    accent = Color(0xFFFF6B6B),
                    onClick = { confirmReset = true },
                    tint = Color(0xFFFF8A8A)
                )
            }
        }

        item {
            SettingsGroup(title = "О приложении") {
                Text(
                    "Hyper Shell ${BuildConfig.VERSION_NAME}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(
                    "Полноценная домашняя оболочка: меню приложений в стиле Apple Watch, " +
                        "панель уведомлений с отдельными обоями, свободно перемещаемые и " +
                        "масштабируемые виджеты. Написана на Kotlin + Jetpack Compose — " +
                        "это компилируемый в машинный код язык, самый быстрый практичный " +
                        "вариант для UI Android.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                )
            }
        }

        item {
            Text(
                text = "Управление жестами и виджетами не требует root. Системная панель уведомлений " +
                    "остаётся доступной, а Hyper Shell показывает свою.",
                color = TextSecondary.copy(alpha = 0.55f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }

    if (confirmReset) {
        HyperDialog(onDismiss = { confirmReset = false }) {
            DialogTitle("Сбросить всё?", "Действие нельзя отменить")
            Text(
                "Будут удалены все виджеты, скрытые приложения, закреплённый док, обои и настройки жестов.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = "Отмена",
                    onClick = { confirmReset = false },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Сбросить",
                    onClick = {
                        confirmReset = false
                        vm.resetEverything()
                    },
                    modifier = Modifier.weight(1f),
                    accent = Color(0xFFFF6B6B)
                )
            }
        }
    }
}
