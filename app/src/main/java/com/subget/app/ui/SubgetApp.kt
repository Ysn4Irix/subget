package com.subget.app.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subget.app.SubgetApplication
import com.subget.app.ui.navigation.Screen
import com.subget.app.ui.screens.downloads.DownloadsScreen
import com.subget.app.ui.screens.downloads.DownloadsViewModel
import com.subget.app.ui.screens.search.SearchScreen
import com.subget.app.ui.screens.search.SearchViewModel
import com.subget.app.ui.screens.settings.SettingsScreen
import com.subget.app.ui.screens.settings.SettingsViewModel

@Composable
fun SubgetApp() {
    val navController = rememberNavController()
    val app = SubgetApplication.instance
    val haptic = LocalHapticFeedback.current

    val startDestination = if (app.settingsRepository.isFirstLaunch() && !app.settingsRepository.hasApiKey()) {
        Screen.Settings.route
    } else {
        Screen.Search.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val getRouteIndex: (String?) -> Int = { route ->
            when (route) {
                Screen.Search.route -> 0
                Screen.Downloads.route -> 1
                Screen.Settings.route -> 2
                else -> 0
            }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                val direction = if (targetIndex >= initialIndex) 1 else -1
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.15f * direction).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                val direction = if (targetIndex >= initialIndex) 1 else -1
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-fullWidth * 0.15f * direction).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                )
            },
            popEnterTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                val direction = if (targetIndex >= initialIndex) 1 else -1
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.15f * direction).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                val direction = if (targetIndex >= initialIndex) 1 else -1
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-fullWidth * 0.15f * direction).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                )
            }
        ) {
            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = SearchViewModel.Factory(
                        subtitleRepository = app.subtitleRepository,
                        settingsRepository = app.settingsRepository,
                        storageManager = app.storageManager,
                        posterRepository = app.posterRepository
                    )
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Downloads.route) {
                val downloadsViewModel: DownloadsViewModel = viewModel(
                    factory = DownloadsViewModel.Factory(
                        storageManager = app.storageManager
                    )
                )
                DownloadsScreen(viewModel = downloadsViewModel)
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        settingsRepository = app.settingsRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.navigate(Screen.Search.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
