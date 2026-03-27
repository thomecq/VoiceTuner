package com.voicetuner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voicetuner.R
import com.voicetuner.model.GamePhase
import com.voicetuner.ui.screens.GamePlayScreen
import com.voicetuner.ui.screens.GameResultScreen
import com.voicetuner.ui.screens.GameSetupScreen
import com.voicetuner.ui.screens.HistoryScreen
import com.voicetuner.ui.screens.MainScreen
import com.voicetuner.ui.screens.SettingsScreen
import com.voicetuner.viewmodel.GameViewModel
import com.voicetuner.viewmodel.PianoViewModel
import com.voicetuner.viewmodel.PitchViewModel

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Keyboard : Screen("keyboard", R.string.keyboard, Icons.Filled.Piano, Icons.Outlined.Piano)
    data object Game : Screen("game", R.string.game, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports)
    data object History : Screen("history", R.string.history, Icons.Filled.History, Icons.Outlined.History)
    data object Settings : Screen("settings", R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

val screens = listOf(Screen.Keyboard, Screen.Game, Screen.History, Screen.Settings)

@Composable
fun AppNavigation(
    pianoViewModel: PianoViewModel,
    pitchViewModel: PitchViewModel,
    gameViewModel: GameViewModel
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = stringResource(screen.titleRes)
                            )
                        },
                        label = {
                            Text(
                                stringResource(screen.titleRes),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = Color(0xFF9896B8),
                            unselectedTextColor = Color(0xFF9896B8)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Keyboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Keyboard.route) {
                MainScreen(pianoViewModel, pitchViewModel)
            }
            composable(Screen.Game.route) {
                val gameState by gameViewModel.gameState.collectAsState()
                when (gameState.phase) {
                    GamePhase.SETUP -> GameSetupScreen(gameViewModel)
                    GamePhase.GAME_OVER -> GameResultScreen(gameViewModel)
                    else -> GamePlayScreen(gameViewModel)
                }
            }
            composable(Screen.History.route) {
                HistoryScreen(pitchViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(pianoViewModel, pitchViewModel)
            }
        }
    }
}
