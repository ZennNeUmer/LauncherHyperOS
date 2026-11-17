package com.hyper.shell.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.shell.core.AppEntry
import com.hyper.shell.core.LauncherSettings
import com.hyper.shell.ui.LauncherViewModel
import com.hyper.shell.ui.components.AppIcon
import com.hyper.shell.ui.theme.TextPrimary
import com.hyper.shell.ui.theme.TextSecondary
import com.hyper.shell.ui.theme.bounceClick
import com.hyper.shell.ui.theme.glass

private const val MAX_DOCK_APPS = 5

@Composable
fun AppsSection(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    accent: Color
) {
    val apps by vm.apps.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var showHidden by rememberSaveable { mutableStateOf(false) }

    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val hiddenCount = apps.count { it.packageName in settings.hiddenApps }
    val dockEntries = remember(settings.dockApps, byPackage) {
        settings.dockApps.mapNotNull { pkg -> byPackage[pkg] }
    }
    val filtered = remember(apps, settings.hiddenApps, query, showHidden) {
        val needle = query.trim()
        apps.filter { app ->
            val matches = needle.isEmpty() ||
                app.label.contains(needle, ignoreCase = true) ||
                app.packageName.contains(needle, ignoreCase = true)
            val visible = showHidden || app.packageName !in settings.hiddenApps
            matches && visible
        }
    }

    Column(Modifier.fillMaxWidth()) {
        SearchField(
            query = query,
            accent = accent,
            onQueryChange = { query = it }
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsGroup(
                    title = "Док",
                    subtitle = if (dockEntries.size >= MAX_DOCK_APPS)
                        "Док заполнен (${MAX_DOCK_APPS} из $MAX_DOCK_APPS)"
                    else "Закреплено ${dockEntries.size} из $MAX_DOCK_APPS • отображается внизу главного экрана"
                ) {
                    if (dockEntries.isEmpty()) {
                        Text(
                            "Добавьте приложения кнопкой со значком «+» в списке ниже.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                        )
                    } else {
                        dockEntries.forEachIndexed { index, app ->
                            val pinned = app.packageName in settings.dockApps
                            AppManageRow(
                                app = app,
                                accent = accent,
                                pinned = pinned,
                                hidden = app.packageName in settings.hiddenApps,
                                showReorder = true,
                                canMoveBack = index > 0,
                                canMoveForward = index < dockEntries.lastIndex,
                                onToggleDock = { vm.setDock(app.packageName, false) },
                                onMove = { direction -> vm.moveDock(app.packageName, direction) },
                                onToggleHidden = { vm.setHidden(app.packageName, true) }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ВСЕ ПРИЛОЖЕНИЯ".uppercase(),
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (hiddenCount > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .glass(shape = RoundedCornerShape(50), alpha = 0.07f)
                                .bounceClick { showHidden = !showHidden }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showHidden) Icons.Rounded.Visibility
                                else Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = if (showHidden) accent else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (showHidden) "Скрыть скрытые" else "Скрытые ($hiddenCount)",
                                color = if (showHidden) accent else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = if (query.isNotBlank()) "Ничего не найдено"
                        else "Список приложений пуст",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(filtered, key = { "${it.packageName}/${it.activityName}" }) { app ->
                    val pinned = app.packageName in settings.dockApps
                    val hidden = app.packageName in settings.hiddenApps
                    Column(Modifier.fillMaxWidth()) {
                        AppManageRow(
                            app = app,
                            accent = accent,
                            pinned = pinned,
                            hidden = hidden,
                            showReorder = false,
                            canMoveBack = false,
                            canMoveForward = false,
                            onToggleDock = {
                                if (!pinned && settings.dockApps.size >= MAX_DOCK_APPS) {
                                    vm.toast("В доке максимум $MAX_DOCK_APPS приложений")
                                } else {
                                    vm.setDock(app.packageName, !pinned)
                                }
                            },
                            onMove = {},
                            onToggleHidden = { vm.setHidden(app.packageName, !hidden) }
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 50.dp, top = 4.dp)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Скрытые приложения не показываются в меню и поиске, но остаются " +
                        "установленными. Закреплённые приложения появляются в доке в выбранном порядке.",
                    color = TextSecondary.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    accent: Color,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .glass(shape = RoundedCornerShape(50), alpha = 0.08f)
            .padding(horizontal = 15.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(9.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Поиск приложений",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(accent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .bounceClick { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Очистить",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppManageRow(
    app: AppEntry,
    accent: Color,
    pinned: Boolean,
    hidden: Boolean,
    showReorder: Boolean,
    canMoveBack: Boolean,
    canMoveForward: Boolean,
    onToggleDock: () -> Unit,
    onMove: (Int) -> Unit,
    onToggleHidden: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(icon = app.icon, label = app.label, size = 40.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = if (hidden) TextSecondary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(app.packageName)
                    when {
                        hidden -> append(" • скрыто")
                        pinned -> append(" • в доке")
                    }
                },
                color = TextSecondary.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showReorder) {
            SmallIconAction(
                icon = Icons.Rounded.ChevronLeft,
                contentDescription = "Левее",
                tint = if (canMoveBack) TextSecondary else TextSecondary.copy(alpha = 0.25f),
                enabled = canMoveBack,
                onClick = { onMove(-1) }
            )
            SmallIconAction(
                icon = Icons.Rounded.ChevronRight,
                contentDescription = "Правее",
                tint = if (canMoveForward) TextSecondary else TextSecondary.copy(alpha = 0.25f),
                enabled = canMoveForward,
                onClick = { onMove(1) }
            )
        }

        SmallIconAction(
            icon = if (pinned) Icons.Rounded.PushPin else Icons.Rounded.Add,
            contentDescription = if (pinned) "Убрать из дока" else "Закрепить в доке",
            tint = if (pinned) accent else TextSecondary,
            background = if (pinned) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
            onClick = onToggleDock
        )
        SmallIconAction(
            icon = if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = if (hidden) "Показать приложение" else "Скрыть приложение",
            tint = if (hidden) Color(0xFFFF8A8A) else TextSecondary,
            background = if (hidden) Color(0xFFE5484D).copy(alpha = 0.14f)
            else Color.White.copy(alpha = 0.06f),
            onClick = onToggleHidden
        )
    }
}

@Composable
private fun SmallIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    background: Color = Color.White.copy(alpha = 0.06f)
) {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(background)
            .bounceClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
    }
}
