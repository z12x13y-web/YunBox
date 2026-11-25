@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.github.avbox.osc.ui.page

import com.github.avbox.osc.api.AppBootstrap
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.avbox.osc.R
import com.github.avbox.osc.server.ControlManager
import com.github.avbox.osc.ui.LivePlayActivity
import com.github.avbox.osc.ui.components.AVBoxAlertDialog
import com.github.avbox.osc.ui.components.LocalTopBarGlassPauseRecording
import com.github.avbox.osc.ui.components.LocalSheetDismissThen
import com.github.avbox.osc.ui.components.LocalSheetHost
import com.github.avbox.osc.ui.components.SheetHost
import com.github.avbox.osc.ui.components.SheetHostState
import com.github.avbox.osc.ui.currentWindowWidthClass
import com.github.avbox.osc.ui.page.ConfigManageScreen
import com.github.avbox.osc.ui.navbar.FloatingNavBar
import com.github.avbox.osc.ui.navbar.GlassTabItem
import com.github.avbox.osc.ui.navbar.NavAxis
import com.github.avbox.osc.ui.navbar.NavMetrics
import com.github.avbox.osc.ui.settings.UiSettingsState
import com.github.avbox.osc.ui.theme.LiquidGlassConfig
import com.github.avbox.osc.ui.theme.LiquidGlassState
import com.github.avbox.osc.ui.SearchActivity
import com.github.avbox.osc.util.ActivityStack
import com.github.avbox.osc.boot.BootGuard
import com.github.avbox.osc.util.HomeSettings
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch

private const val NavSearchButtonSizeDp = 56
private const val NavSearchGapDp = 8

private enum class AppTab(@StringRes val labelRes: Int, @DrawableRes val icon: Int) {
    MEDIA(R.string.tab_media, R.drawable.ic_tab_home),
    RESOURCE(R.string.tab_resource, R.drawable.ic_tab_resource),
    SETTINGS(R.string.tab_settings, R.drawable.ic_tab_mine),
}

@Composable
fun MainScreen() {
    LaunchedEffect(Unit) { AppBootstrap.start() }
    val boot by AppBootstrap.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        MainContent()
        if (boot is AppBootstrap.Boot.Error) {
            BootErrorDialog((boot as AppBootstrap.Boot.Error).msg)
        }
    }
}

@Composable
private fun BootErrorDialog(msg: String) {
    AVBoxAlertDialog(
        onDismissRequest = {},
        dismissible = false,
        title = { Text(stringResource(R.string.config_load_failed)) },
        text = { Text(msg) },
        confirmButton = {
            val dismissThen = LocalSheetDismissThen.current
            TextButton(onClick = { dismissThen { AppBootstrap.retry() } }) {
                Text(stringResource(R.string.common_retry))
            }
        },
        dismissButton = {
            val dismissThen = LocalSheetDismissThen.current
            TextButton(onClick = { dismissThen { AppBootstrap.continueOffline() } }) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun MainContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { AppTab.entries.size })
    val homeViewModel: HomeViewModel = viewModel()

    LaunchedEffect(Unit) {
        if (homeViewModel.defaultLiveLaunched) return@LaunchedEffect
        AppBootstrap.state.collect { boot ->
            if (boot is AppBootstrap.Boot.Ready && !homeViewModel.defaultLiveLaunched) {
                homeViewModel.markDefaultLiveLaunched()
                val disabled = BootGuard.takeSafeDisabledNotice()
                if (disabled.isNotEmpty()) {
                    Toast.makeText(context, context.getString(R.string.toast_source_auto_disabled), Toast.LENGTH_LONG).show()
                }
                if (UiSettingsState.config.defaultLoadLive) {
                    context.startActivity(Intent(context, LivePlayActivity::class.java))
                }
            }
        }
    }

    LaunchedEffect(pagerState) {
        withFrameNanos { }
        withFrameNanos { }
        pagerState.scrollBy(1f)
        pagerState.scrollBy(-1f)
    }

    BackHandler {
        if (homeViewModel.onBackPressedOnce()) {
            ActivityStack.getInstance().finishAllActivity()
            ControlManager.get().stopServer()
            (context as? Activity)?.finishAffinity()
        } else {
            Toast.makeText(context, context.getString(R.string.toast_press_again_to_exit), Toast.LENGTH_SHORT).show()
        }
    }

    val sheetHost = remember { SheetHostState() }
    val navConfig = UiSettingsState.config
    val navAnimationEnabled = !navConfig.navAnimationDisabled

    val selectTab: (Int) -> Unit = { index ->
        scope.launch {
            if (navAnimationEnabled) {
                pagerState.animateScrollToPage(index)
            } else {
                pagerState.scrollToPage(index)
            }
        }
    }
    val liquidGlassConfig = LiquidGlassState.config
    val liquidGlassEnabled = liquidGlassConfig.navbarEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val liquidBackdropBgColor = MaterialTheme.colorScheme.surfaceContainer
    val liquidBackdropOnDraw: ContentDrawScope.() -> Unit =
        remember(liquidBackdropBgColor) {
            { drawRect(liquidBackdropBgColor); drawContent() }
        }
    val liquidBackdrop = rememberLayerBackdrop(onDraw = liquidBackdropOnDraw)
    val pauseGlassRecording: () -> Boolean = remember(pagerState, sheetHost) {
        { pagerState.isScrollInProgress || sheetHost.request != null }
    }
    val pauseTopBarRecording: () -> Boolean = remember(pagerState) {
        { pagerState.isScrollInProgress }
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val navAxis = NavMetrics.axisFor(currentWindowWidthClass())
    val railMode = navAxis == NavAxis.Vertical
    val surfaceNavVisible = !liquidGlassEnabled
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val navBandExtent = NavMetrics.BAND_EXTENT_DP.dp
    val liquidBackdropBounds: (Size) -> Rect? = remember(density, navBandExtent, navAxis) {
        { size ->
            val extentPx = with(density) { navBandExtent.toPx() }
            if (navAxis == NavAxis.Horizontal) {
                Rect(0f, size.height - extentPx, size.width, size.height)
            } else {
                Rect(0f, 0f, extentPx, size.height)
            }
        }
    }
    val navReserve = NavMetrics.reserveDp(liquidGlassEnabled, navAxis).dp
    val pageContentPadding: PaddingValues = when {
        railMode -> PaddingValues(
            start = navReserve + navBarsPadding.calculateStartPadding(layoutDirection),
            bottom = navBarsPadding.calculateBottomPadding(),
        )

        liquidGlassEnabled -> PaddingValues(
            bottom = navBarsPadding.calculateBottomPadding() + navReserve,
        )

        else -> PaddingValues(0.dp)
    }
    val tabLabels = AppTab.entries.map { stringResource(it.labelRes) }
    val glassTabs = remember(tabLabels) {
        AppTab.entries.mapIndexed { index, tab -> GlassTabItem(tab.icon, tabLabels[index]) }
    }
    val searchActionLabel = stringResource(R.string.common_search)
    val openSearch: () -> Unit = remember(context) {
        { context.startActivity(Intent(context, SearchActivity::class.java)) }
    }
    CompositionLocalProvider(
        LocalSheetHost provides sheetHost,
        LocalTopBarGlassPauseRecording provides pauseTopBarRecording,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (surfaceNavVisible && !railMode) {
                        ShortNavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            AppTab.entries.forEachIndexed { index, tab ->
                                val selected = pagerState.targetPage == index
                                ShortNavigationBarItem(
                                    selected = selected,
                                    onClick = { selectTab(index) },
                                    icon = {
                                        Icon(
                                            painterResource(tab.icon),
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    },
                                    label = if (selected) {
                                        { Text(stringResource(tab.labelRes)) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (liquidGlassEnabled) {
                                Modifier.layerBackdrop(liquidBackdrop, liquidBackdropBounds, pauseGlassRecording)
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = navAnimationEnabled,
                        beyondViewportPageCount = 3,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (liquidGlassEnabled) Modifier else Modifier.padding(innerPadding)),
                    ) { page ->
                        val pageLifecycleOwner = rememberLifecycleOwner(
                            maxLifecycle = if (page == pagerState.targetPage) {
                                Lifecycle.State.RESUMED
                            } else {
                                Lifecycle.State.STARTED
                            }
                        )
                        CompositionLocalProvider(LocalLifecycleOwner provides pageLifecycleOwner) {
                            when (AppTab.entries[page]) {
                                AppTab.MEDIA -> HomePage(homeViewModel, pageContentPadding)
                                AppTab.RESOURCE -> ConfigManageScreen(onNavigateBack = {})
                                AppTab.SETTINGS -> SettingsPage(contentPadding = pageContentPadding)
                            }
                        }
                    }
                }
            }
            if (liquidGlassEnabled) {
                val homeHorizontal = HomeSettings.layoutFlow.collectAsStateWithLifecycle().value ==
                    HomeSettings.HomeLayout.Horizontal
                if (navAxis == NavAxis.Horizontal && !homeHorizontal) {
                    val scrimColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(navBandExtent)
                            .align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(0f to Color.Transparent, 1f to scrimColor)),
                    )
                }
                Box(
                    modifier = if (navAxis == NavAxis.Horizontal) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = NavMetrics.MARGIN_DP.dp)
                    } else {
                        Modifier
                            .align(Alignment.CenterStart)
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(vertical = 16.dp)
                            .padding(start = NavMetrics.MARGIN_DP.dp)
                    },
                ) {
                    if (navAxis == NavAxis.Horizontal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FloatingNavBar(
                                modifier = Modifier.weight(1f),
                                backdrop = liquidBackdrop,
                                axis = navAxis,
                                selectedTabIndex = { pagerState.targetPage },
                                onTabSelected = selectTab,
                                tabs = glassTabs,
                                config = liquidGlassConfig,
                                interactive = { true },
                            )
                            Spacer(modifier = Modifier.width(NavSearchGapDp.dp))
                            NavSearchButton(
                                backdrop = liquidBackdrop,
                                config = liquidGlassConfig,
                                label = searchActionLabel,
                                onSearchClick = openSearch,
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            FloatingNavBar(
                                backdrop = liquidBackdrop,
                                axis = navAxis,
                                selectedTabIndex = { pagerState.targetPage },
                                onTabSelected = selectTab,
                                tabs = glassTabs,
                                config = liquidGlassConfig,
                                interactive = { true },
                            )
                            NavSearchButton(
                                backdrop = liquidBackdrop,
                                config = liquidGlassConfig,
                                label = searchActionLabel,
                                onSearchClick = openSearch,
                            )
                        }
                    }
                }
            }
            if (surfaceNavVisible && railMode) {
                NavigationRail(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    AppTab.entries.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = pagerState.currentPage == index,
                            onClick = { selectTab(index) },
                            icon = {
                                Icon(
                                    painterResource(tab.icon),
                                    contentDescription = stringResource(tab.labelRes),
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
            SheetHost(sheetHost)
        }
    }
}

@Composable
private fun NavSearchButton(
    backdrop: Backdrop,
    config: LiquidGlassConfig,
    label: String,
    onSearchClick: () -> Unit,
) {
    val isBlurEnabled = config.navbarEnabled
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
        alpha = if (isBlurEnabled) 0.4f * config.containerAlphaScale else 1f,
    )
    Box(
        modifier = Modifier
            .size(NavSearchButtonSizeDp.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    if (isBlurEnabled) {
                        colorControls(
                            brightness = config.contentBrightness,
                            contrast = config.contentContrast,
                            saturation = config.contentSaturation,
                        )
                        blur(config.blurDp.dp.toPx())
                        lens(
                            config.distortionDp.dp.toPx(),
                            config.distortionDp.dp.toPx(),
                            chromaticAberration = config.dispersion,
                        )
                    }
                },
                highlight = {
                    Highlight.Default.copy(alpha = if (isBlurEnabled) 1f else 0f)
                },
                shadow = {
                    Shadow.Default.copy(
                        color = Color.Black.copy(if (isLightTheme) 0.1f else 0.2f),
                    )
                },
                onDrawSurface = { drawRect(containerColor) },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSearchClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_tab_search),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}
