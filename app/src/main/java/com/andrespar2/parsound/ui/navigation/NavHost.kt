package com.andrespar2.parsound.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andrespar2.parsound.ui.screen.home.HomeScreen
import com.andrespar2.parsound.ui.screen.library.LibraryScreen
import com.andrespar2.parsound.ui.screen.player.FullPlayerScreen
import com.andrespar2.parsound.ui.screen.settings.SettingsScreen

@Composable
fun ParSoundNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("library") { LibraryScreen(navController) }
        composable("player") { FullPlayerScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}
