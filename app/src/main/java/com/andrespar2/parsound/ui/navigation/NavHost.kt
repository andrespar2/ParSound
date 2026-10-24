package com.andrespar2.parsound.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andrespar2.parsound.ui.screen.home.HomeScreen
import com.andrespar2.parsound.ui.screen.library.LibraryScreen
import com.andrespar2.parsound.ui.screen.player.FullPlayerScreen
import com.andrespar2.parsound.ui.screen.settings.SettingsScreen
import com.andrespar2.parsound.ui.screen.favorites.FavoritesScreen
import com.andrespar2.parsound.ui.screen.search.SearchScreen
import com.andrespar2.parsound.ui.screen.queue.QueueScreen

@Composable
fun ParSoundNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.LIBRARY) { LibraryScreen(navController) }
        composable(Routes.PLAYER) { FullPlayerScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.FAVORITES) { FavoritesScreen(navController) }
        composable(Routes.SEARCH) { SearchScreen(navController) }
        composable(Routes.QUEUE) { QueueScreen(navController) }
    }
}
