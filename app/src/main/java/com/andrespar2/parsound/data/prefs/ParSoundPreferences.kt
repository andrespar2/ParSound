package com.andrespar2.parsound.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parsound_prefs")

data class FolderConfig(val uri: String, val name: String)

object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

@Singleton
class ParSoundPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val GLASS_BLUR = floatPreferencesKey("glass_blur")
        val GLASS_ALPHA = floatPreferencesKey("glass_alpha")
        val GLOW = floatPreferencesKey("glow")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val PERFORMANCE_MODE = booleanPreferencesKey("performance_mode")
        val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        val RESUME_SESSION = booleanPreferencesKey("resume_session")
        val PAUSE_ON_DISCONNECT = booleanPreferencesKey("pause_on_disconnect")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PITCH = floatPreferencesKey("pitch")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = intPreferencesKey("eq_preset")
        val EQ_BANDS = stringPreferencesKey("eq_bands")
        val FOLDERS = stringPreferencesKey("saf_folders")
        val LAST_SONG_URI = stringPreferencesKey("last_song_uri")
        val LAST_POSITION = longPreferencesKey("last_position")
        val LAST_SCAN = longPreferencesKey("last_scan")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val LAST_VOLUME = intPreferencesKey("last_volume")
        val SHOW_COVER_BG = booleanPreferencesKey("show_cover_bg")
    }

    private val data = context.dataStore.data.catch { e ->
        if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e
    }

    val themeMode: Flow<Int> = data.map { it[Keys.THEME_MODE] ?: ThemeMode.DARK }
    val amoledBlack: Flow<Boolean> = data.map { it[Keys.AMOLED_BLACK] ?: false }
    val dynamicColors: Flow<Boolean> = data.map { it[Keys.DYNAMIC_COLORS] ?: true }
    val shuffleEnabled: Flow<Boolean> = data.map { it[Keys.SHUFFLE_ENABLED] ?: false }
    val repeatMode: Flow<Int> = data.map { it[Keys.REPEAT_MODE] ?: 0 }
    val glassBlur: Flow<Float> = data.map { it[Keys.GLASS_BLUR] ?: 0.65f }
    val glassAlpha: Flow<Float> = data.map { it[Keys.GLASS_ALPHA] ?: 0.55f }
    val glow: Flow<Float> = data.map { it[Keys.GLOW] ?: 0.45f }
    val animations: Flow<Boolean> = data.map { it[Keys.ANIMATIONS] ?: true }
    val performanceMode: Flow<Boolean> = data.map { it[Keys.PERFORMANCE_MODE] ?: false }
    val crossfadeMs: Flow<Int> = data.map { it[Keys.CROSSFADE_MS] ?: 0 }
    val resumeSession: Flow<Boolean> = data.map { it[Keys.RESUME_SESSION] ?: true }
    val pauseOnDisconnect: Flow<Boolean> = data.map { it[Keys.PAUSE_ON_DISCONNECT] ?: true }
    val playbackSpeed: Flow<Float> = data.map { it[Keys.PLAYBACK_SPEED] ?: 1f }
    val pitch: Flow<Float> = data.map { it[Keys.PITCH] ?: 1f }
    val eqEnabled: Flow<Boolean> = data.map { it[Keys.EQ_ENABLED] ?: false }
    val eqPreset: Flow<Int> = data.map { it[Keys.EQ_PRESET] ?: -1 }
    val eqBands: Flow<String> = data.map { it[Keys.EQ_BANDS] ?: "" }
    val lastScan: Flow<Long> = data.map { it[Keys.LAST_SCAN] ?: 0L }
    val lastSongUri: Flow<String> = data.map { it[Keys.LAST_SONG_URI] ?: "" }
    val lastPosition: Flow<Long> = data.map { it[Keys.LAST_POSITION] ?: 0L }
    val showCoverBackground: Flow<Boolean> = data.map { it[Keys.SHOW_COVER_BG] ?: true }

    val folders: Flow<List<FolderConfig>> = data.map { parseFolders(it[Keys.FOLDERS] ?: "[]") }

    suspend fun foldersOnce(): List<FolderConfig> = folders.first()

    suspend fun addFolder(folder: FolderConfig) {
        context.dataStore.edit { prefs ->
            val current = parseFolders(prefs[Keys.FOLDERS] ?: "[]")
            if (current.none { it.uri == folder.uri }) {
                prefs[Keys.FOLDERS] = encodeFolders(current + folder)
            }
        }
    }

    suspend fun removeFolder(uri: String) {
        context.dataStore.edit { prefs ->
            val current = parseFolders(prefs[Keys.FOLDERS] ?: "[]")
            prefs[Keys.FOLDERS] = encodeFolders(current.filterNot { it.uri == uri })
        }
    }

    suspend fun setThemeMode(value: Int) = edit { it[Keys.THEME_MODE] = value }
    suspend fun setAmoledBlack(value: Boolean) = edit { it[Keys.AMOLED_BLACK] = value }
    suspend fun setDynamicColors(value: Boolean) = edit { it[Keys.DYNAMIC_COLORS] = value }
    suspend fun setGlassBlur(value: Float) = edit { it[Keys.GLASS_BLUR] = value }
    suspend fun setGlassAlpha(value: Float) = edit { it[Keys.GLASS_ALPHA] = value }
    suspend fun setGlow(value: Float) = edit { it[Keys.GLOW] = value }
    suspend fun setAnimations(value: Boolean) = edit { it[Keys.ANIMATIONS] = value }
    suspend fun setPerformanceMode(value: Boolean) = edit { it[Keys.PERFORMANCE_MODE] = value }
    suspend fun setCrossfadeMs(value: Int) = edit { it[Keys.CROSSFADE_MS] = value.coerceIn(0, 12_000) }
    suspend fun setResumeSession(value: Boolean) = edit { it[Keys.RESUME_SESSION] = value }
    suspend fun setPauseOnDisconnect(value: Boolean) = edit { it[Keys.PAUSE_ON_DISCONNECT] = value }
    suspend fun setPlaybackSpeed(value: Float) = edit { it[Keys.PLAYBACK_SPEED] = value.coerceIn(0.5f, 2f) }
    suspend fun setPitch(value: Float) = edit { it[Keys.PITCH] = value.coerceIn(0.5f, 2f) }
    suspend fun setEqEnabled(value: Boolean) = edit { it[Keys.EQ_ENABLED] = value }
    suspend fun setEqPreset(value: Int) = edit { it[Keys.EQ_PRESET] = value }
    suspend fun setEqBands(value: String) = edit { it[Keys.EQ_BANDS] = value }
    suspend fun setLastScan(value: Long) = edit { it[Keys.LAST_SCAN] = value }
    suspend fun setLastSongUri(value: String) = edit { it[Keys.LAST_SONG_URI] = value }
    suspend fun setLastPosition(value: Long) = edit { it[Keys.LAST_POSITION] = value }
    suspend fun setShuffle(value: Boolean) = edit { it[Keys.SHUFFLE_ENABLED] = value }
    suspend fun setRepeatMode(value: Int) = edit { it[Keys.REPEAT_MODE] = value }
    suspend fun setShowCoverBackground(value: Boolean) = edit { it[Keys.SHOW_COVER_BG] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun parseFolders(raw: String): List<FolderConfig> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uri = obj.optString("uri")
                if (uri.isNotBlank()) {
                    add(FolderConfig(uri, obj.optString("name", "Carpeta")))
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun encodeFolders(folders: List<FolderConfig>): String {
        val array = JSONArray()
        folders.forEach {
            array.put(JSONObject().put("uri", it.uri).put("name", it.name))
        }
        return array.toString()
    }
}
