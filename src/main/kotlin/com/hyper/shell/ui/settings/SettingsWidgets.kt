package com.hyper.shell.ui.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.shell.core.LauncherSettings
import com.hyper.shell.core.WidgetConfig
import com.hyper.shell.core.WidgetKind
import com.hyper.shell.ui.LauncherViewModel
import com.hyper.shell.ui.components.GlassButton
import com.hyper.shell.ui.theme.TextPrimary
import com.hyper.shell.ui.theme.TextSecondary
import com.hyper.shell.ui.theme.bounceClick
import com.hyper.shell.ui.theme.glass

private data class BuiltInWidget(
    val kind: WidgetKind,
    val title: String,
    val icon: ImageVector
)

private val builtInWidgets = listOf(
    BuiltInWidget(WidgetKind.CLOCK, "Часы", Icons.Rounded.AccessTime),
    BuiltInWidget(WidgetKind.SEARCH, "Поиск", Icons.Rounded.Search),
    BuiltInWidget(WidgetKind.BATTERY, "Батарея", Icons.Rounded.BatteryFull),
    BuiltInWidget(WidgetKind.NOTIFICATIONS, "Уведомления", Icons.Rounded.NotificationsActive)
)

private fun kindTitle(kind: WidgetKind): String = when (kind) {
    WidgetKind.CLOCK -> "Часы"
    WidgetKind.SEARCH -> "Поиск приложений"
    WidgetKind.BATTERY -> "Батарея"
    WidgetKind.NOTIFICATIONS -> "Уведомления"
    WidgetKind.APPWIDGET -> "Виджет Android"
}

private fun kindIcon(kind: WidgetKind): ImageVector = when (kind) {
    WidgetKind.CLOCK -> Icons.Rounded.AccessTime
    WidgetKind.SEARCH -> Icons.Rounded.Search
    WidgetKind.BATTERY -> Icons.Rounded.BatteryFull
    WidgetKind.NOTIFICATIONS -> Icons.Rounded.NotificationsActive
    WidgetKind.APPWIDGET -> Icons.Rounded.Widgets
}

@Composable
fun WidgetsSection(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    accent: Color,
    widthDp: Float,
    heightDp: Float,
    onClose: () -> Unit
) {
    val widgets by vm.widgets.collectAsState()
    var pendingId by remember { mutableIntStateOf(-1) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        val id = if (resultId >= 0) resultId else pendingId
        if (result.resultCode == Activity.RESULT_OK && id >= 0) {
            val added = vm.addPickedWidget(id, widthDp, heightDp)
            if (!added) {
                vm.deleteWidgetId(id)
                vm.toast("Не удалось привязать виджет")
            } else {
                vm.toast("Виджет добавлен на главный экран")
            }
        } else if (pendingId >= 0) {
            vm.deleteWidgetId(pendingId)
        }
        pendingId = -1
    }

    fun launchSystemPicker() {
        val id = vm.allocateWidgetId()
        pendingId = id
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        val launched = runCatching { picker.launch(intent) }.isSuccess
        if (!launched) {
            vm.deleteWidgetId(id)
            pendingId = -1
            vm.toast("Системный выбор виджетов недоступен на этом устройстве")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsGroup(
                title = "На главном экране",
                subtitle = if (widgets.isEmpty()) "Пока нет виджетов. Добавьте первый ниже."
                else "${widgets.size} шт. • перетаскивайте и меняйте размер прямо на рабочем столе"
            ) {
                if (widgets.isEmpty()) {
                    Text(
                        "Пусто",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    )
                } else {
                    widgets.forEach { widget ->
                        WidgetRow(
                            widget = widget,
                            accent = accent,
                            onRemove = { vm.removeWidget(widget) }
                        )
                    }
                }
            }
        }

        item {
            SettingsGroup(
                title = "Встроенные виджеты",
                subtitle = "Созданы специально для Hyper Shell и поддерживают свободное перемещение."
            ) {
                builtInWidgets.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            GlassButton(
                                text = item.title,
                                icon = item.icon,
                                accent = accent,
                                modifier = Modifier.weight(1f),
                                onClick = { vm.addBuiltInWidget(item.kind, widthDp, heightDp) }
                            )
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            SettingsGroup(
                title = "Виджеты Android",
                subtitle = "Часы, календарь, музыка и другие виджеты установленных приложений."
            ) {
                SettingsActionRow(
                    title = "Добавить виджет приложения",
                    subtitle = "Откроется системный выбор виджетов",
                    icon = Icons.Rounded.Widgets,
                    accent = accent,
                    onClick = { launchSystemPicker() }
                )
            }
        }

        item {
            SettingsGroup(title = "Расположение") {
                SettingsActionRow(
                    title = "Режим редактирования",
                    subtitle = "Перемещение и изменение размера на главном экране",
                    icon = Icons.Rounded.Tune,
                    accent = accent,
                    onClick = {
                        vm.prefs.requestWidgetEdit()
                        onClose()
                    }
                )
                SettingsActionRow(
                    title = "Сбросить расположение",
                    subtitle = "Удалить все виджеты и вернуть стандартный набор",
                    icon = Icons.Rounded.Refresh,
                    accent = Color(0xFFFF8A8A),
                    tint = Color(0xFFFF8A8A),
                    onClick = {
                        vm.resetWidgets(widthDp, heightDp)
                        vm.toast("Расположение сброшено")
                    }
                )
            }
        }

        item {
            Text(
                text = "Совет: зажмите пустое место на главном экране, чтобы войти в режим " +
                    "редактирования и добавить виджет без перехода в настройки.",
                color = TextSecondary.copy(alpha = 0.65f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun WidgetRow(
    widget: WidgetConfig,
    accent: Color,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .glass(shape = CircleShape, alpha = 0.09f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = kindIcon(widget.kind),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                kindTitle(widget.kind),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${widget.w.toInt()} × ${widget.h.toInt()} dp • x ${widget.x.toInt()}, y ${widget.y.toInt()}",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE5484D).copy(alpha = 0.16f))
                .bounceClick(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Удалить виджет",
                tint = Color(0xFFFF8A8A),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
