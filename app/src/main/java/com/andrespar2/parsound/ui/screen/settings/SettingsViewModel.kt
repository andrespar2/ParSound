package com.andrespar2.parsound.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.prefs.FolderConfig
import com.andrespar2.parsound.data.prefs.ParSoundPreferences
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.player.PlayerManager
import com.andrespar2.parsound.scanner.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: ParSoundPreferences,
    private val repository: MusicRepository,
    private val playerManager: PlayerManager
) : ViewModel() {
    val themeMode = preferences.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 2)
    val amoled = preferences.amoledBlack.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val dynamicColors = preferences.dynamicColors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val glassAlpha = preferences.glassAlpha.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.55f)
    val glow = preferences.glow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.45f)
    val blur = preferences.glassBlur.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.65f)
    val animations = preferences.animations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val performance = preferences.performanceMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val coverBackground = preferences.showCoverBackground.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val crossfadeMs = preferences.crossfadeMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val resumeSession = preferences.resumeSession.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val pauseOnDisconnect = preferences.pauseOnDisconnect.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val lastScan = repository.lastScan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val folders: StateFlow<List<FolderConfig>> = repository.configuredFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val scanState: StateFlow<ScanState> = repository.scanState
    val songCount = repository.songCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setThemeMode(value: Int) = launch { preferences.setThemeMode(value) }
    fun setAmoled(value: Boolean) = launch { preferences.setAmoledBlack(value) }
    fun setDynamicColors(value: Boolean) = launch { preferences.setDynamicColors(value) }
    fun setGlassAlpha(value: Float) = launch { preferences.setGlassAlpha(value) }
    fun setGlow(value: Float) = launch { preferences.setGlow(value) }
    fun setBlur(value: Float) = launch { preferences.setGlassBlur(value) }
    fun setAnimations(value: Boolean) = launch { preferences.setAnimations(value) }
    fun setPerformance(value: Boolean) = launch { preferences.setPerformanceMode(value) }
    fun setCoverBackground(value: Boolean) = launch { preferences.setShowCoverBackground(value) }
    fun setCrossfade(value: Int) = launch { preferences.setCrossfadeMs(value) }
    fun setResumeSession(value: Boolean) = launch { preferences.setResumeSession(value) }
    fun setPauseOnDisconnect(value: Boolean) = launch {
        preferences.setPauseOnDisconnect(value)
        playerManager.setPauseOnDisconnect(value)
    }

    fun scan() = repository.scanNow()
    fun cancelScan() = repository.cancelScan()

    fun clearHistory() = launch { repository.clearHistory() }

    fun findEmbeddedLyrics(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val found = runCatching { repository.enrichMissingLyrics() }.getOrDefault(0)
            playerManager.refreshCurrentSong()
            onResult(found)
        }
    }

    fun resetAppearance() {
        viewModelScope.launch {
            preferences.setThemeMode(2)
            preferences.setAmoledBlack(false)
            preferences.setDynamicColors(true)
            preferences.setShowCoverBackground(true)
            preferences.setGlassAlpha(0.55f)
            preferences.setGlow(0.45f)
            preferences.setGlassBlur(0.65f)
            preferences.setAnimations(true)
            preferences.setPerformanceMode(false)
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
