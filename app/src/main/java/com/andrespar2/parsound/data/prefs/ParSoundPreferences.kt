package com.andrespar2.parsound.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parsound_prefs")

@Singleton
class ParSoundPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        val REFLECTION_INTENSITY = floatPreferencesKey("reflection_intensity")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val CUSTOM_CROSSFADE_ENABLED = booleanPreferencesKey("custom_crossfade_enabled")
        val CUSTOM_CROSSFADE_MS = intPreferencesKey("custom_crossfade_ms")
        val USE_SAF_FOLDERS = stringPreferencesKey("saf_folders")
        val LAST_SONG_URI = stringPreferencesKey("last_song_uri")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
    }

    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: true }
    val amoledBlack: Flow<Boolean> = context.dataStore.data.map { it[Keys.AMOLED_BLACK] ?: false }
    val dynamicColors: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLORS] ?: true }
    val blurIntensity: Flow<Float> = context.dataStore.data.map { it[Keys.BLUR_INTENSITY] ?: 0.6f }
    val glassTransparency: Flow<Float> = context.dataStore.data.map { it[Keys.GLASS_TRANSPARENCY] ?: 0.5f }
    val reflectionIntensity: Flow<Float> = context.dataStore.data.map { it[Keys.REFLECTION_INTENSITY] ?: 0.4f }
    val reducedMotion: Flow<Boolean> = context.dataStore.data.map { it[Keys.REDUCED_MOTION] ?: false }
    val crossfadeSeconds: Flow<Int> = context.dataStore.data.map { it[Keys.CROSSFADE_SECONDS] ?: 0 }
    val customCrossfadeEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.CUSTOM_CROSSFADE_ENABLED] ?: false }
    val customCrossfadeMs: Flow<Int> = context.dataStore.data.map { it[Keys.CUSTOM_CROSSFADE_MS] ?: 3000 }
    val repeatMode: Flow<Int> = context.dataStore.data.map { it[Keys.REPEAT_MODE] ?: 0 }
    val shuffleEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHUFFLE_ENABLED] ?: false }

    suspend fun setDarkTheme(value: Boolean) { context.dataStore.edit { it[Keys.DARK_THEME] = value } }
    suspend fun setAmoledBlack(value: Boolean) { context.dataStore.edit { it[Keys.AMOLED_BLACK] = value } }
    suspend fun setDynamicColors(value: Boolean) { context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = value } }
    suspend fun setBlurIntensity(value: Float) { context.dataStore.edit { it[Keys.BLUR_INTENSITY] = value } }
    suspend fun setGlassTransparency(value: Float) { context.dataStore.edit { it[Keys.GLASS_TRANSPARENCY] = value } }
    suspend fun setReflectionIntensity(value: Float) { context.dataStore.edit { it[Keys.REFLECTION_INTENSITY] = value } }
    suspend fun setReducedMotion(value: Boolean) { context.dataStore.edit { it[Keys.REDUCED_MOTION] = value } }
    suspend fun setCrossfadeSeconds(value: Int) { context.dataStore.edit { it[Keys.CROSSFADE_SECONDS] = value } }
    suspend fun setCustomCrossfadeEnabled(value: Boolean) { context.dataStore.edit { it[Keys.CUSTOM_CROSSFADE_ENABLED] = value } }
    suspend fun setCustomCrossfadeMs(value: Int) { context.dataStore.edit { it[Keys.CUSTOM_CROSSFADE_MS] = value } }
    suspend fun setRepeatMode(value: Int) { context.dataStore.edit { it[Keys.REPEAT_MODE] = value } }
    suspend fun setShuffleEnabled(value: Boolean) { context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = value } }
}
