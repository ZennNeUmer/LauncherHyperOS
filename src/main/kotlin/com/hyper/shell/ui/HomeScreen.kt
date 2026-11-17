package com.hyper.shell.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.shell.core.AppEntry
import com.hyper.shell.core.LauncherSettings
import com.hyper.shell.core.WidgetConfig
import com.hyper.shell.ui.components.AppIcon
import com.hyper.shell.ui.components.GlassButton
import com.hyper.shell.ui.components.GlassIconButton
import com.hyper.shell.ui.theme.TextPrimary
import com.hyper.shell.ui.theme.TextSecondary
import com.hyper.shell.ui.theme.bounceClick
import com.hyper.shell.ui.theme.glass

@Composable
fun HomeContent(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    apps: List<AppEntry>,
    widgets: List<WidgetConfig>,
    notificationsCount: Int,
    accent: Color,
    editing: Boolean,
    onEditDone: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenDrawerSearch: () -> Unit,
    onOpenShade: () -> Unit,
    onHomeMenu: () -> Unit,
    onAppLongPress: (AppEntry) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val canvasWidth = maxWidth.value
        val canvasHeight = (maxHeight.value - 100f).coerceAtLeast(220f)

        LaunchedEffect(settings.widgetsSeeded, canvasWidth, canvasHeight) {
            if (!settings.widgetsSeeded) {
                vm.ensureDefaultWidgets(canvasWidth, canvasHeight)
            }
        }

        // Empty-space long press. It sits below widgets and the dock, so it only
        // receives taps that are not handled by real content.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onHomeMenu() })
                }
        )

        widgets.forEach { config ->
            androidx.compose.runtime.key(config.id) {
                WidgetFrame(
                    config = config,
                    editing = editing,
                    canvasWidthDp = canvasWidth,
                    canvasHeightDp = canvasHeight,
                    accent = accent,
                    onUpdate = { vm.updateWidget(it) },
                    onRemove = { vm.removeWidget(it) }
                ) {
                    HomeWidgetContent(
                        config = config,
                        notificationsCount = notificationsCount,
                        accent = accent,
                        onOpenSearch = onOpenDrawerSearch,
                        onOpenShade = onOpenShade
                    )
                }
            }
        }

        if (editing) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .glass(shape = RoundedCornerShape(50), alpha = 0.16f)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GlassButton(
                    text = "Готово",
                    onClick = onEditDone,
                    accent = accent
                )
                GlassButton(
                    text = "Виджет",
                    onClick = onAddWidget,
                    icon = Icons.Rounded.Add
                )
            }
        }

        GlassIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Настройки",
            onClick = { vm.openSettings() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 16.dp),
            size = 42.dp,
            alpha = 0.14f
        )

        Dock(
            apps = apps,
            settings = settings,
            accent = accent,
            modifier = Modifier.align(Alignment.BottomCenter),
            onOpenDrawer = onOpenDrawer,
            onLaunch = vm::launch,
            onAppLongPress = onAppLongPress
        )
    }
}

@Composable
private fun Dock(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    accent: Color,
    onOpenDrawer: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!settings.showDock) {
        Box(modifier.navigationBarsPadding().padding(bottom = 20.dp)) {
            DockDrawerButton(accent = accent, onClick = onOpenDrawer)
        }
        return
    }

    val visible = remember(apps, settings.hiddenApps) {
        apps.filterNot { it.packageName in settings.hiddenApps }
    }
    val pinned = remember(visible, settings.dockApps) {
        settings.dockApps.mapNotNull { pkg -> visible.firstOrNull { it.packageName == pkg } }
    }
    val dockApps = remember(visible, pinned) {
        if (pinned.isNotEmpty()) pinned.take(5) else visible.take(5)
    }

    Box(modifier.navigationBarsPadding().padding(bottom = 13.dp)) {
        Row(
            modifier = Modifier
                .glass(shape = RoundedCornerShape(34.dp), alpha = 0.13f)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockApps.forEach { app ->
                AppIcon(
                    icon = app.icon,
                    label = app.label,
                    size = 49.dp,
                    modifier = Modifier.bounceClick(
                        onClick = { onLaunch(app) },
                        onLongClick = { onAppLongPress(app) }
                    )
                )
            }
            if (dockApps.isEmpty()) {
                Text(
                    "Приложения",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
            DockDrawerButton(accent = accent, onClick = onOpenDrawer)
        }
    }
}

@Composable
private fun DockDrawerButton(accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(49.dp)
            .glass(shape = CircleShape, alpha = 0.12f)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = "Все приложения",
            tint = accent,
            modifier = Modifier.size(23.dp)
        )
    }
}
