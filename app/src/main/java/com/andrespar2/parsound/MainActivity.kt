package com.andrespar2.parsound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.andrespar2.parsound.ui.navigation.ParSoundNavHost
import com.andrespar2.parsound.ui.theme.ParSoundTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParSoundTheme {
                ParSoundNavHost()
            }
        }
    }
}
