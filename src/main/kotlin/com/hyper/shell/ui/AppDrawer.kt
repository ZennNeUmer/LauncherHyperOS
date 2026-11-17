package com.hyper.shell.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.shell.core.AppEntry
import com.hyper.shell.core.LauncherSettings
import com.hyper.shell.ui.components.AppIcon
import com.hyper.shell.ui.components.GlassIconButton
import com.hyper.shell.ui.theme.TextPrimary
import com.hyper.shell.ui.theme.TextSecondary
import com.hyper.shell.ui.theme.bounceClick
import com.hyper.shell.ui.theme.glass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppDrawerPanel(
    apps: List<AppEntry>,
    settings: LauncherSettings,
    accent: Color,
    startSearch: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchVisible by remember { mutableStateOf(startSearch) }
    var query by remember { mutableStateOf("") }
    var pull by remember { mutableFloatStateOf(0f) }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(startSearch) {
        if (startSearch) searchVisible = true
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Box(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = pull }
        ) {
            // A flat, GPU-cheap background. The old implementation decoded a second
            // full-screen wallpaper and blurred it every frame, which froze the main
            // thread on mid-range devices.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xF2070A12))
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = 950f
                        )
                    )
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
                        .padding(start = 22.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Приложения",
                            color = TextPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = pluralApps(apps.size),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = "Поиск",
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) {
                                query = ""
                                keyboard?.hide()
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Закрыть",
                        onClick = onClose
                    )
                }

                AnimatedVisibility(
                    visible = searchVisible,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        accent = accent,
                        onDismiss = {
                            query = ""
                            searchVisible = false
                            keyboard?.hide()
                        }
                    )
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (query.isNotBlank()) {
                        SearchResults(
                            apps = filtered,
                            accent = accent,
                            onLaunch = onLaunch,
                            onLongPress = onLongPress
                        )
                    } else {
                        AppGrid(
                            apps = filtered,
                            labels = settings.showLabels,
                            onLaunch = onLaunch,
                            onLongPress = onLongPress
                        )
                    }
                }

                Text(
                    text = "Свайп вправо — закрыть",
                    color = TextSecondary.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Narrow edge handle: closing the drawer must not fight with taps on icons.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(26.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                pull = (pull + dragAmount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (pull > 90.dp.toPx()) {
                                    keyboard?.hide()
                                    onClose()
                                } else {
                                    scope.launch {
                                        animate(
                                            pull,
                                            0f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        ) { value, _ -> pull = value }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    animate(
                                        pull,
                                        0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                    ) { value, _ -> pull = value }
                                }
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    accent: Color,
    onDismiss: () -> Unit
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(120)
        focus.requestFocus()
        keyboard?.show()
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .focusRequester(focus)
            .glass(shape = RoundedCornerShape(50), alpha = 0.10f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Поиск по приложениям",
                            color = TextSecondary.copy(alpha = 0.8f),
                            fontSize = 15.sp
                        )
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    GlassIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Очистить",
                        onClick = onDismiss,
                        size = 30.dp,
                        alpha = 0.08f
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchResults(
    apps: List<AppEntry>,
    accent: Color,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit
) {
    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ничего не найдено", color = TextPrimary, fontSize = 18.sp)
                Text(
                    "Попробуйте часть названия",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        listItems(apps, key = { "${it.packageName}/${it.activityName}" }) { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(shape = RoundedCornerShape(22.dp), alpha = 0.08f)
                    .bounceClick(
                        onClick = { onLaunch(app) },
                        onLongClick = { onLongPress(app) }
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(app.icon, app.label, 42.dp)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        app.packageName,
                        color = TextSecondary.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppEntry>,
    labels: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit
) {
    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет приложений", color = TextSecondary, fontSize = 15.sp)
        }
        return
    }

    // LazyVerticalGrid only composes the visible rows (unlike the old honeycomb,
    // which laid out and animated all 80+ icons during every opening frame).
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(
            items = apps,
            key = { "${it.packageName}/${it.activityName}" }
        ) { app ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .bounceClick(
                        onClick = { onLaunch(app) },
                        onLongClick = { onLongPress(app) }
                    )
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                AppIcon(
                    icon = app.icon,
                    label = app.label,
                    size = 56.dp
                )
                if (labels) {
                    Text(
                        text = app.label,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp, start = 1.dp, end = 1.dp)
                    )
                }
            }
        }
    }
}

private fun pluralApps(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "приложение"
        mod10 in 2..4 && mod100 !in 12..14 -> "приложения"
        else -> "приложений"
    }
    return "$count $word"
}
