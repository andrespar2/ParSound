package com.andrespar2.parsound.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.ui.components.BottomDestination
import com.andrespar2.parsound.ui.components.GlassBottomBar
import com.andrespar2.parsound.ui.components.MiniPlayer
import com.andrespar2.parsound.ui.screen.collection.CollectionScreen
import com.andrespar2.parsound.ui.screen.effects.EffectsScreen
import com.andrespar2.parsound.ui.screen.folders.FoldersScreen
import com.andrespar2.parsound.ui.screen.home.HomeScreen
import com.andrespar2.parsound.ui.screen.licenses.LicensesScreen
import com.andrespar2.parsound.ui.screen.library.LibraryScreen
import com.andrespar2.parsound.ui.screen.player.PlayerScreen
import com.andrespar2.parsound.ui.screen.playlist.PlaylistDetailScreen
import com.andrespar2.parsound.ui.screen.playlist.PlaylistsScreen
import com.andrespar2.parsound.ui.screen.queue.QueueScreen
import com.andrespar2.parsound.ui.screen.search.SearchScreen
import com.andrespar2.parsound.ui.screen.settings.SettingsScreen

private val TOP_LEVEL_ROUTES = setOf(Routes.HOME, Routes.LIBRARY, Routes.SEARCH, Routes.SETTINGS)

private val BOTTOM_DESTINATIONS = listOf(
    BottomDestination(Routes.HOME, "Inicio", Icons.Default.Home),
    BottomDestination(Routes.LIBRARY, "Biblioteca", Icons.Default.LibraryMusic),
    BottomDestination(Routes.SEARCH, "Buscar", Icons.Default.Search),
    BottomDestination(Routes.SETTINGS, "Ajustes", Icons.Default.Settings)
)

@Composable
fun ParSoundNavHost(playerManager: PlayerManager) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute in TOP_LEVEL_ROUTES
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val position by playerManager.positionMs.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showChrome) {
                Column {
                    val song = currentSong
                    if (song != null) {
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            positionMs = position,
                            onOpen = {
                                if (currentRoute != Routes.PLAYER) navController.navigate(Routes.PLAYER)
                            },
                            onToggle = { playerManager.toggle() },
                            onNext = { playerManager.next() }
                        )
                    }
                    GlassBottomBar(
                        destinations = BOTTOM_DESTINATIONS,
                        selectedRoute = currentRoute ?: Routes.HOME,
                        onSelect = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) { HomeScreen(navController) }
            composable(Routes.LIBRARY) { LibraryScreen(navController) }
            composable(Routes.SEARCH) { SearchScreen(navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
            composable(Routes.PLAYER) { PlayerScreen(navController) }
            composable(Routes.QUEUE) { QueueScreen(navController) }
            composable(Routes.EFFECTS) { EffectsScreen(navController) }
            composable(Routes.LICENSES) { LicensesScreen(navController) }
            composable(Routes.FOLDERS) { FoldersScreen(navController) }
            composable(Routes.PLAYLISTS) { PlaylistsScreen(navController) }

            composable(
                Routes.ALBUM,
                arguments = listOf(navArgument("albumKey") { type = NavType.StringType })
            ) { CollectionScreen(navController) }
            composable(
                Routes.ARTIST,
                arguments = listOf(navArgument("artist") { type = NavType.StringType })
            ) { CollectionScreen(navController) }
            composable(
                Routes.GENRE,
                arguments = listOf(navArgument("genre") { type = NavType.StringType })
            ) { CollectionScreen(navController) }
            composable(
                Routes.FOLDER,
                arguments = listOf(
                    navArgument("folderUri") { type = NavType.StringType },
                    navArgument("folder") { type = NavType.StringType }
                )
            ) { CollectionScreen(navController) }
            composable(
                Routes.PLAYLIST,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { PlaylistDetailScreen(navController) }
        }
    }
}

