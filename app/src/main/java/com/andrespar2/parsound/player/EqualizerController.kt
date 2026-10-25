package com.andrespar2.parsound.player

import android.media.audiofx.Equalizer
import android.util.Log
import com.andrespar2.parsound.data.prefs.ParSoundPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class EqBand(
    val index: Int,
    val frequencyHz: Int,
    val levelMb: Int,
    val minMb: Int,
    val maxMb: Int
)

data class EqState(
    val supported: Boolean = false,
    val attached: Boolean = false,
    val enabled: Boolean = false,
    val presetNames: List<String> = emptyList(),
    val preset: Int = -1,
    val bands: List<EqBand> = emptyList()
)

@Singleton
class EqualizerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: ParSoundPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    private var equalizer: Equalizer? = null
    private var sessionId = 0
    private var configured = false

    init {
        scope.launch {
            val enabled = preferences.eqEnabled.first()
            val preset = preferences.eqPreset.first()
            val bandsRaw = preferences.eqBands.first()
            _state.value = _state.value.copy(
                enabled = enabled,
                preset = preset,
                bands = parseBands(bandsRaw)
            )
            configured = true
            applyAll()
        }
    }

    fun attachSession(id: Int) {
        if (id == 0 || id == sessionId) {
            applyAll()
            return
        }
        sessionId = id
        releaseEqualizer()
        runCatching {
            val effect = Equalizer(0, id)
            equalizer = effect
            val storedLevels = _state.value.bands.map { it.levelMb }
            val count = effect.numberOfBands.toInt()
            val range = effect.bandLevelRange
            val bands = (0 until count).map { index ->
                val band = index.toShort()
                val freq = effect.getCenterFreq(band) / 1000
                EqBand(
                    index = index,
                    frequencyHz = freq,
                    levelMb = storedLevels.getOrNull(index)
                        ?: effect.getBandLevel(band).toInt(),
                    minMb = range[0].toInt(),
                    maxMb = range[1].toInt()
                )
            }
            val presets = (0 until effect.numberOfPresets.toInt()).map {
                runCatching { effect.getPresetName(it.toShort()) }.getOrDefault("Preset $it")
            }
            _state.value = _state.value.copy(
                supported = true,
                attached = true,
                presetNames = presets,
                bands = bands
            )
            applyAll()
        }.onFailure {
            Log.w("ParSoundEq", "Ecualizador no disponible", it)
            _state.value = _state.value.copy(supported = false, attached = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        runCatching { equalizer?.enabled = enabled }
        scope.launch { preferences.setEqEnabled(enabled) }
    }

    fun setPreset(index: Int) {
        _state.value = _state.value.copy(preset = index)
        equalizer?.let { effect ->
            if (index in 0 until effect.numberOfPresets) {
                runCatching { effect.usePreset(index.toShort()) }
                refreshBandLevels()
            }
        }
        scope.launch { preferences.setEqPreset(index) }
    }

    fun setBandLevel(index: Int, levelMb: Int) {
        val effect = equalizer ?: return
        val band = _state.value.bands.firstOrNull { it.index == index } ?: return
        val clamped = levelMb.coerceIn(band.minMb, band.maxMb)
        runCatching { effect.setBandLevel(index.toShort(), clamped.toShort()) }
        val updated = _state.value.bands.map {
            if (it.index == index) it.copy(levelMb = clamped) else it
        }
        _state.value = _state.value.copy(bands = updated, preset = -1)
        scope.launch {
            preferences.setEqBands(updated.joinToString(",") { it.levelMb.toString() })
            preferences.setEqPreset(-1)
        }
    }

    private fun applyAll() {
        val effect = equalizer ?: return
        if (!configured) return
        val current = _state.value
        runCatching {
            effect.enabled = current.enabled
            if (current.enabled) {
                if (current.preset >= 0) {
                    effect.usePreset(current.preset.toShort())
                    refreshBandLevels()
                } else {
                    current.bands.forEach { band ->
                        effect.setBandLevel(band.index.toShort(), band.levelMb.toShort())
                    }
                }
            }
        }
    }

    private fun refreshBandLevels() {
        val effect = equalizer ?: return
        val bands = _state.value.bands.map { band ->
            band.copy(levelMb = runCatching { effect.getBandLevel(band.index.toShort()).toInt() }
                .getOrDefault(band.levelMb))
        }
        _state.value = _state.value.copy(bands = bands)
    }

    private fun parseBands(raw: String): List<EqBand> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',').mapIndexedNotNull { index, value ->
            val level = value.toIntOrNull() ?: return@mapIndexedNotNull null
            EqBand(index, 0, level, -1500, 1500)
        }
    }

    private fun releaseEqualizer() {
        runCatching { equalizer?.enabled = false }
        runCatching { equalizer?.release() }
        equalizer = null
    }
}
