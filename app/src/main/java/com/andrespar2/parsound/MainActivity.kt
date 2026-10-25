package com.andrespar2.parsound

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.andrespar2.parsound.data.prefs.ParSoundPreferences
import com.andrespar2.parsound.data.prefs.ThemeMode
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.ui.navigation.ParSoundNavHost
import com.andrespar2.parsound.ui.theme.ParSoundTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferences: ParSoundPreferences

    @Inject
    lateinit var playerManager: PlayerManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.apply {
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }
        requestNotificationPermission()
        lifecycleScope.launch {
            playerManager.restoreSessionOnce()
        }
        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = ThemeMode.DARK)
            val amoled by preferences.amoledBlack.collectAsState(initial = false)
            val dynamicColors by preferences.dynamicColors.collectAsState(initial = true)
            val glassAlpha by preferences.glassAlpha.collectAsState(initial = 0.55f)
            val glow by preferences.glow.collectAsState(initial = 0.45f)
            val blur by preferences.glassBlur.collectAsState(initial = 0.65f)
            val animations by preferences.animations.collectAsState(initial = true)
            val performance by preferences.performanceMode.collectAsState(initial = false)
            val coverBackground by preferences.showCoverBackground.collectAsState(initial = true)
            val palette by playerManager.palette.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> systemDark
            }
            ParSoundTheme(
                darkTheme = darkTheme,
                amoledBlack = amoled,
                palette = if (dynamicColors) palette else com.andrespar2.parsound.player.PaletteColors.Default,
                glassAlpha = glassAlpha,
                glow = glow,
                blur = blur,
                animations = animations,
                performanceMode = performance,
                coverBackground = coverBackground
            ) {
                val view = LocalView.current
                SideEffect {
                    val window = view.context.findActivity()?.window ?: return@SideEffect
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        val barColor = if (darkTheme) {
                            android.graphics.Color.rgb(10, 12, 18)
                        } else {
                            android.graphics.Color.rgb(244, 246, 250)
                        }
                        window.statusBarColor = barColor
                        window.navigationBarColor = barColor
                    } else {
                        window.statusBarColor = Color.TRANSPARENT
                        window.navigationBarColor = Color.TRANSPARENT
                    }
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(com.andrespar2.parsound.ui.theme.LocalGlass.current.backgroundStart)
                ) {
                    ParSoundNavHost(playerManager)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        playerManager.updateWidget()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
