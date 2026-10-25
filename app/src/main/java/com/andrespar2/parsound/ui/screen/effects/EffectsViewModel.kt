package com.andrespar2.parsound.ui.screen.effects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.prefs.ParSoundPreferences
import com.andrespar2.parsound.player.EqualizerController
import com.andrespar2.parsound.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EffectsViewModel @Inject constructor(
    private val equalizer: EqualizerController,
    private val playerManager: PlayerManager,
    preferences: ParSoundPreferences
) : ViewModel() {
    val eqState = equalizer.state
    val sleepRemaining = playerManager.sleepRemaining
    val sleepEndsAt = playerManager.sleepEndsAt
    val speed: StateFlow<Float> = preferences.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)
    val pitch: StateFlow<Float> = preferences.pitch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    fun setEnabled(enabled: Boolean) = equalizer.setEnabled(enabled)
    fun setPreset(index: Int) = equalizer.setPreset(index)
    fun setBand(index: Int, level: Int) = equalizer.setBandLevel(index, level)
    fun setSpeed(value: Float) = playerManager.setSpeed(value)
    fun setPitch(value: Float) = playerManager.setPitch(value)
    fun startSleep(minutes: Int) = playerManager.startSleepTimer(minutes)
    fun cancelSleep() = playerManager.cancelSleepTimer()
}
