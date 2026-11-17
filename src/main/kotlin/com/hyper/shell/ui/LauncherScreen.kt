package com.hyper.shell.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import com.hyper.shell.core.AppEntry
import com.hyper.shell.core.WidgetKind
import com.hyper.shell.ui.theme.HyperBlue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val settings by vm.settings.collectAsState()
    val apps by vm.apps.collectAsState()
    val widgets by vm.widgets.collectAsState()
    val notifications by vm.notifications.collectAsState()
    val accent = Color(settings.accent)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val screenHeightDp = configuration.screenHeightDp.toFloat()

    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressJob by remember { mutableStateOf<Job?>(null) }
    var editing by remember { mutableStateOf(false) }
    var homeMenu by remember { mutableStateOf(false) }
    var addWidgetMenu by remember { mutableStateOf(false) }
    var appAction by remember { mutableStateOf<AppEntry?>(null) }
    var drawerSearch by remember { mutableStateOf(false) }
    var pendingWidgetId by remember { mutableIntStateOf(-1) }

    val visibleApps = remember(apps, settings.hiddenApps) {
        apps.filterNot {
            it.packageName in settings.hiddenApps ||
                it.activityName == LauncherActivity::class.java.name
        }
    }

    fun animateProgress(target: Float, spec: AnimationSpec<Float>, onDone: () -> Unit = {}) {
        progressJob?.cancel()
        progressJob = scope.launch {
            animate(progress, target, animationSpec = spec) { value, _ -> progress = value }
            onDone()
        }
    }

    fun openOverlay(target: Overlay, search: Boolean = false) {
        if (overlay != null) return
        drawerSearch = search && target == Overlay.DRAWER
        overlay = target
        animateProgress(1f, spring(dampingRatio = 0.87f, stiffness = 420f))
    }

    fun closeOverlay() {
        if (overlay == null) return
        animateProgress(0f, tween(260, easing = FastOutSlowInEasing)) {
            if (progress <= 0.01f) {
                overlay = null
                drawerSearch = false
            }
        }
    }

    fun launchFromShell(app: AppEntry) {
        // Hide the drawer before starting the activity, otherwise it is still
        // open when the user returns to the home screen.
        progressJob?.cancel()
        overlay = null
        progress = 0f
        drawerSearch = false
        vm.launch(app)
    }

    LaunchedEffect(settings.editRequest) {
        if (settings.editRequest > 0L) {
            editing = true
            vm.prefs.update { it.copy(editRequest = 0L) }
        }
    }

    val widgetPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        val id = if (resultId >= 0) resultId else pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id >= 0) {
            val added = vm.addPickedWidget(id, screenWidthDp, screenHeightDp)
            if (!added) {
                vm.deleteWidgetId(id)
                vm.toast("Не удалось привязать виджет")
            } else {
                editing = true
            }
        } else if (pendingWidgetId >= 0) {
            vm.deleteWidgetId(pendingWidgetId)
        }
        pendingWidgetId = -1
        addWidgetMenu = false
    }

    fun launchWidgetPicker() {
        val id = vm.allocateWidgetId()
        pendingWidgetId = id
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        val launched = runCatching { widgetPicker.launch(intent) }.isSuccess
        if (!launched) {
            vm.deleteWidgetId(id)
            pendingWidgetId = -1
            vm.toast("Системный выбор виджетов недоступен на этом устройстве")
        }
        addWidgetMenu = false
    }

    val gestureModifier = Modifier
        .fillMaxSize()
        .launcherGestures(
            enabled = overlay == null && !editing && !homeMenu && !addWidgetMenu && appAction == null,
            settings = settings,
            onStart = { target ->
                progressJob?.cancel()
                drawerSearch = false
                overlay = target
                progress = 0f
            },
            onDrag = { _, value ->
                progressJob?.cancel()
                progress = value
            },
            onEnd = { _, value, velocity ->
                val shouldOpen = velocity > 850f || (velocity > -850f && value > 0.32f)
                progress = value
                animateProgress(
                    target = if (shouldOpen) 1f else 0f,
                    spec = if (shouldOpen) {
                        spring(dampingRatio = 0.88f, stiffness = 420f)
                    } else {
                        tween(230, easing = FastOutSlowInEasing)
                    }
                ) {
                    if (!shouldOpen && progress <= 0.01f) {
                        overlay = null
                        drawerSearch = false
                    }
                }
            }
        )

    Box(gestureModifier) {
        WallpaperBackground(
            uri = settings.homeWallpaper,
            accent = accent,
            animate = settings.animations
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress
                    scaleX = 1f - 0.06f * p
                    scaleY = 1f - 0.06f * p
                    translationX = -size.width * 0.10f * p
                    alpha = 1f - 0.12f * p
                }
        ) {
            HomeContent(
                vm = vm,
                settings = settings,
                apps = visibleApps,
                widgets = widgets,
                notificationsCount = notifications.size,
                accent = accent,
                editing = editing,
                onEditDone = { editing = false },
                onAddWidget = { addWidgetMenu = true },
                onOpenDrawer = { openOverlay(Overlay.DRAWER) },
                onOpenDrawerSearch = { openOverlay(Overlay.DRAWER, search = true) },
                onOpenShade = { openOverlay(Overlay.SHADE) },
                onHomeMenu = { homeMenu = true },
                onAppLongPress = { appAction = it }
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .graphicsLayer { alpha = progress * 0.45f }
        )

        val active = overlay
        if (active != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = progress
                        translationX = (1f - p) * size.width
                        val s = 0.94f + 0.06f * p
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        alpha = 0.55f + 0.45f * p
                    }
            ) {
                when (active) {
                    Overlay.DRAWER -> AppDrawerPanel(
                        apps = visibleApps,
                        settings = settings,
                        accent = accent,
                        startSearch = drawerSearch,
                        onLaunch = { launchFromShell(it) },
                        onLongPress = { appAction = it },
                        onClose = { closeOverlay() }
                    )

                    Overlay.SHADE -> NotificationShadePanel(
                        vm = vm,
                        settings = settings,
                        accent = accent,
                        onClose = { closeOverlay() }
                    )
                }
            }
        }

        BackHandler {
            when {
                overlay != null -> closeOverlay()
                editing -> editing = false
                // Never finish the home activity on Back: the system would show
                // the launcher chooser or an empty screen.
            }
        }

        if (homeMenu) {
            HomeMenuDialog(
                accent = if (settings.accent != 0) accent else HyperBlue,
                onDismiss = { homeMenu = false },
                onOpenDrawer = {
                    homeMenu = false
                    openOverlay(Overlay.DRAWER)
                },
                onOpenShade = {
                    homeMenu = false
                    openOverlay(Overlay.SHADE)
                },
                onEditWidgets = {
                    homeMenu = false
                    editing = true
                },
                onAddWidget = {
                    homeMenu = false
                    addWidgetMenu = true
                },
                onAppearance = {
                    homeMenu = false
                    vm.openSettings("appearance")
                },
                onSettings = {
                    homeMenu = false
                    vm.openSettings()
                }
            )
        }

        if (addWidgetMenu) {
            AddWidgetDialog(
                accent = accent,
                onDismiss = { addWidgetMenu = false },
                onBuiltIn = { kind ->
                    vm.addBuiltInWidget(kind, screenWidthDp, screenHeightDp)
                    addWidgetMenu = false
                    editing = true
                },
                onAndroidWidget = { launchWidgetPicker() }
            )
        }

        appAction?.let { app ->
            val pinned = app.packageName in settings.dockApps
            val hidden = app.packageName in settings.hiddenApps
            AppActionDialog(
                app = app,
                accent = accent,
                pinned = pinned,
                hidden = hidden,
                onDismiss = { appAction = null },
                onOpen = {
                    launchFromShell(app)
                    appAction = null
                },
                onToggleDock = {
                    vm.setDock(app.packageName, !pinned)
                    appAction = null
                },
                onToggleHidden = {
                    vm.setHidden(app.packageName, !hidden)
                    appAction = null
                },
                onInfo = {
                    vm.openAppInfo(app.packageName)
                    appAction = null
                },
                onUninstall = {
                    vm.uninstall(app.packageName)
                    appAction = null
                }
            )
        }
    }
}
