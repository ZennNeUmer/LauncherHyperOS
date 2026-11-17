package com.hyper.shell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.shell.core.AppEntry
import com.hyper.shell.core.WidgetKind
import com.hyper.shell.ui.components.AppIcon
import com.hyper.shell.ui.components.DialogAction
import com.hyper.shell.ui.components.DialogTitle
import com.hyper.shell.ui.components.GlassButton
import com.hyper.shell.ui.components.HyperDialog
import com.hyper.shell.ui.theme.TextPrimary
import com.hyper.shell.ui.theme.TextSecondary

@Composable
fun HomeMenuDialog(
    accent: Color,
    onDismiss: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenShade: () -> Unit,
    onEditWidgets: () -> Unit,
    onAddWidget: () -> Unit,
    onAppearance: () -> Unit,
    onSettings: () -> Unit
) {
    HyperDialog(onDismiss = onDismiss) {
        DialogTitle("Hyper Shell", "Главный экран")
        Spacer(Modifier.height(10.dp))
        DialogAction("Меню приложений", Icons.Rounded.Apps, onClick = onOpenDrawer)
        DialogAction("Панель уведомлений", Icons.Rounded.Notifications, onClick = onOpenShade)
        DialogAction("Изменить виджеты", Icons.Rounded.Widgets, onClick = onEditWidgets)
        DialogAction("Добавить виджет", Icons.Rounded.Add, onClick = onAddWidget)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        DialogAction("Обои и стиль", Icons.Rounded.Wallpaper, onClick = onAppearance)
        DialogAction("Настройки", Icons.Rounded.Settings, tint = accent, onClick = onSettings)
    }
}

@Composable
fun AddWidgetDialog(
    accent: Color,
    onDismiss: () -> Unit,
    onBuiltIn: (WidgetKind) -> Unit,
    onAndroidWidget: () -> Unit
) {
    HyperDialog(onDismiss = onDismiss) {
        DialogTitle("Добавить виджет", "Встроенные или настоящие виджеты Android")
        Spacer(Modifier.height(8.dp))
        DialogAction("Часы", Icons.Rounded.AccessTime) { onBuiltIn(WidgetKind.CLOCK) }
        DialogAction("Батарея", Icons.Rounded.BatteryFull) { onBuiltIn(WidgetKind.BATTERY) }
        DialogAction("Поиск приложений", Icons.Rounded.Search) { onBuiltIn(WidgetKind.SEARCH) }
        DialogAction("Уведомления", Icons.Rounded.NotificationsActive) {
            onBuiltIn(WidgetKind.NOTIFICATIONS)
        }
        DialogAction("Виджет Android", Icons.Rounded.Widgets, tint = accent, onClick = onAndroidWidget)
        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = "Отмена",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AppActionDialog(
    app: AppEntry,
    accent: Color,
    pinned: Boolean,
    hidden: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleDock: () -> Unit,
    onToggleHidden: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    HyperDialog(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.icon, app.label, 54.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        DialogAction("Открыть", Icons.Rounded.OpenInNew, tint = accent, onClick = onOpen)
        DialogAction(
            if (pinned) "Убрать из дока" else "Закрепить в доке",
            if (pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            onClick = onToggleDock
        )
        DialogAction(
            if (hidden) "Показывать в меню" else "Скрыть из меню",
            if (hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
            onClick = onToggleHidden
        )
        DialogAction("О приложении", Icons.Rounded.Info, onClick = onInfo)
        DialogAction(
            "Удалить приложение",
            Icons.Rounded.Delete,
            tint = Color(0xFFFF6B6B),
            onClick = onUninstall
        )
    }
}
