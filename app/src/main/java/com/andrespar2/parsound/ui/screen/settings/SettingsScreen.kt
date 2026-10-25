package com.andrespar2.parsound.ui.screen.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.BuildConfig
import com.andrespar2.parsound.data.prefs.ThemeMode
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.GlassCard
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.components.PillButton
import com.andrespar2.parsound.ui.components.glassModifier
import com.andrespar2.parsound.ui.navigation.Routes
import com.andrespar2.parsound.util.songsLabel
import com.andrespar2.parsound.util.foldersLabel
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.util.formatDate
import com.andrespar2.parsound.util.openUri
import com.andrespar2.parsound.widget.ParSoundWidgetReceiver
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val amoled by viewModel.amoled.collectAsState()
    val dynamicColors by viewModel.dynamicColors.collectAsState()
    val glassAlpha by viewModel.glassAlpha.collectAsState()
    val glow by viewModel.glow.collectAsState()
    val blur by viewModel.blur.collectAsState()
    val animations by viewModel.animations.collectAsState()
    val performance by viewModel.performance.collectAsState()
    val coverBackground by viewModel.coverBackground.collectAsState()
    val crossfade by viewModel.crossfadeMs.collectAsState()
    val resume by viewModel.resumeSession.collectAsState()
    val pauseOnDisconnect by viewModel.pauseOnDisconnect.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val lastScan by viewModel.lastScan.collectAsState()
    val songCount by viewModel.songCount.collectAsState()
    val context = LocalContext.current
    val glass = LocalGlass.current

    var showTheme by remember { mutableStateOf(false) }
    var showCrossfade by remember { mutableStateOf(false) }
    var showWidgets by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }

    if (showTheme) {
        ThemeDialog(
            current = themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                showTheme = false
            },
            onDismiss = { showTheme = false }
        )
    }
    if (showCrossfade) {
        CrossfadeDialog(
            current = crossfade,
            onSet = viewModel::setCrossfade,
            onDismiss = { showCrossfade = false }
        )
    }
    if (showWidgets) {
        WidgetInfoDialog(onDismiss = { showWidgets = false })
    }
    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            containerColor = glass.backgroundEnd,
            title = { Text("Borrar historial", color = glass.textPrimary) },
            text = {
                Text(
                    "Se borrará la lista de canciones escuchadas recientemente. Tus favoritas y listas no se modifican.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistory = false
                }) { Text("Borrar", color = glass.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistory = false }) {
                    Text("Cancelar", color = glass.textSecondary)
                }
            }
        )
    }

    AmbientBackground(showArtwork = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                ParSoundTopBar(
                    title = "Ajustes",
                    subtitle = "ParSound ${BuildConfig.VERSION_NAME}",
                    onBack = { navController.navigateUp() }
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .glassModifier(24.dp, strong = true)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(glass.accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.GraphicEq, null, tint = glass.accent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "ParSound",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary
                        )
                        Text(
                            "Desarrollado por andrespar2",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                        Text(
                            "Versión ${BuildConfig.VERSION_NAME} · Compilación ${BuildConfig.VERSION_CODE}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }

            item { SettingsSectionTitle("Apariencia") }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        icon = Icons.Default.Palette,
                        title = "Tema",
                        subtitle = when (themeMode) {
                            ThemeMode.LIGHT -> "Claro"
                            ThemeMode.DARK -> "Oscuro"
                            else -> "Seguir al sistema"
                        },
                        onClick = { showTheme = true }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.DarkMode,
                        title = "Negro AMOLED",
                        subtitle = "Fondo completamente negro en modo oscuro",
                        checked = amoled,
                        onChecked = viewModel::setAmoled
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Colores adaptativos",
                        subtitle = "La interfaz toma los colores de la portada actual",
                        checked = dynamicColors,
                        onChecked = viewModel::setDynamicColors
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Image,
                        title = "Fondo con portada",
                        subtitle = "Usa la portada desenfocada como fondo",
                        checked = coverBackground,
                        onChecked = viewModel::setCoverBackground
                    )
                    SettingsSliderRow(
                        icon = Icons.Default.BlurOn,
                        title = "Desenfoque",
                        value = blur,
                        valueLabel = "${(blur * 100).roundToInt()} %",
                        onChange = viewModel::setBlur
                    )
                    SettingsSliderRow(
                        icon = Icons.Default.Opacity,
                        title = "Transparencia del cristal",
                        value = glassAlpha,
                        valueLabel = "${(glassAlpha * 100).roundToInt()} %",
                        onChange = viewModel::setGlassAlpha
                    )
                    SettingsSliderRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Reflejos",
                        value = glow,
                        valueLabel = "${(glow * 100).roundToInt()} %",
                        onChange = viewModel::setGlow
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Animation,
                        title = "Animaciones",
                        subtitle = "Transiciones suaves entre canciones y colores",
                        checked = animations,
                        onChecked = viewModel::setAnimations
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.BatterySaver,
                        title = "Ahorro de recursos",
                        subtitle = "Reduce desenfoques y capas de cristal en dispositivos modestos",
                        checked = performance,
                        onChecked = viewModel::setPerformance
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Refresh,
                        title = "Restablecer apariencia",
                        subtitle = "Vuelve a los valores recomendados",
                        onClick = viewModel::resetAppearance
                    )
                }
            }

            item { SettingsSectionTitle("Reproducción") }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        icon = Icons.Default.Speed,
                        title = "Crossfade",
                        subtitle = when {
                            crossfade <= 0 -> "Desactivado"
                            crossfade < 1000 -> "${crossfade} ms"
                            else -> "${crossfade / 1000} segundos"
                        },
                        onClick = { showCrossfade = true }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.History,
                        title = "Reanudar última sesión",
                        subtitle = "Recuerda la canción y la posición al abrir ParSound",
                        checked = resume,
                        onChecked = viewModel::setResumeSession
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Headphones,
                        title = "Pausar al desconectar auriculares",
                        subtitle = "Detiene la reproducción si se desconecta la salida de audio",
                        checked = pauseOnDisconnect,
                        onChecked = viewModel::setPauseOnDisconnect
                    )
                    SettingsNavRow(
                        icon = Icons.Default.Tune,
                        title = "Velocidad, tono y temporizador",
                        subtitle = "Controla la reproducción y el apagado automático",
                        onClick = { navController.navigate(Routes.EFFECTS) }
                    )
                    SettingsNavRow(
                        icon = Icons.Default.GraphicEq,
                        title = "Efectos de audio",
                        subtitle = "Ecualizador del dispositivo y presets",
                        onClick = { navController.navigate(Routes.EFFECTS) }
                    )
                }
            }

            item { SettingsSectionTitle("Biblioteca") }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        icon = Icons.Default.Folder,
                        title = "Carpetas de música",
                        subtitle = foldersLabel(folders.size),
                        onClick = { navController.navigate(Routes.FOLDERS) }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Refresh,
                        title = "Actualizar biblioteca",
                        subtitle = "Último análisis: ${formatDate(lastScan)}",
                        onClick = viewModel::scan
                    )
                    SettingsNavRow(
                        icon = Icons.Default.QueueMusic,
                        title = "Listas de reproducción",
                        subtitle = "Crea y organiza tus listas",
                        onClick = { navController.navigate(Routes.PLAYLISTS) }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.History,
                        title = "Borrar historial reciente",
                        subtitle = songsLabel(songCount) + " en la biblioteca",
                        onClick = { showClearHistory = true }
                    )
                }
            }

            item { SettingsSectionTitle("Widgets") }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        icon = Icons.Default.Widgets,
                        title = "Widgets de ParSound",
                        subtitle = "Compacto, mediano y grande para tu pantalla de inicio",
                        onClick = { showWidgets = true }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Add,
                        title = "Añadir widget a la pantalla de inicio",
                        subtitle = "Pide al launcher que coloque ParSound",
                        onClick = {
                            val manager = AppWidgetManager.getInstance(context)
                            val provider = ComponentName(context, ParSoundWidgetReceiver::class.java)
                            val requested = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                manager.isRequestPinAppWidgetSupported
                            ) {
                                manager.requestPinAppWidget(provider, null, null)
                            } else {
                                false
                            }
                            Toast.makeText(
                                context,
                                if (requested) {
                                    "Confirma en tu launcher para colocar el widget"
                                } else {
                                    "Mantén pulsada la pantalla de inicio y elige Widgets > ParSound"
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }

            item { SettingsSectionTitle("Información") }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        icon = Icons.Default.Code,
                        title = "Repositorio en GitHub",
                        subtitle = "andrespar2/ParSound",
                        onClick = { context.openUri(BuildConfig.GITHUB_REPO) }
                    )
                    SettingsNavRow(
                        icon = Icons.Default.Description,
                        title = "Licencias de terceros",
                        subtitle = "Bibliotecas de código abierto utilizadas",
                        onClick = { navController.navigate(Routes.LICENSES) }
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .glassModifier(26.dp, strong = true)
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(glass.accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Download, null, tint = glass.accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Descargar música gratis",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary
                            )
                            Text(
                                "Proyecto externo: ParDL",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accent
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "ParDL es una herramienta independiente de andrespar2 para descargar audio de forma rápida. ParSound no incluye un descargador ni aloja música: solo reproduce tus archivos locales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    PillButton(
                        "Abrir ParDL en GitHub",
                        onClick = { context.openUri(BuildConfig.PARDL_REPO) },
                        icon = Icons.Default.OpenInNew
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "ParSound",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary
                    )
                    Text(
                        "Desarrollado por andrespar2",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Música local, sin anuncios y sin seguimiento.",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalGlass.current.textSecondary,
        modifier = Modifier.padding(start = 26.dp, top = 18.dp, bottom = 6.dp),
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .glassModifier(22.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val glass = LocalGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = glass.accent, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = glass.textSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val glass = LocalGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = glass.accent, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val glass = LocalGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = glass.accent, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = glass.backgroundStart,
                checkedTrackColor = glass.accent,
                uncheckedThumbColor = glass.textSecondary,
                uncheckedTrackColor = glass.glassStrong
            )
        )
    }
}

@Composable
private fun SettingsSliderRow(
    icon: ImageVector,
    title: String,
    value: Float,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    val glass = LocalGlass.current
    var localValue by remember(value) { mutableStateOf(value) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = glass.accent, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = glass.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onChange(localValue) },
            colors = SliderDefaults.colors(
                thumbColor = glass.accent,
                activeTrackColor = glass.accent,
                inactiveTrackColor = glass.textSecondary.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun ThemeDialog(
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Tema de la aplicación", color = glass.textPrimary) },
        text = {
            Column {
                listOf(
                    ThemeMode.SYSTEM to "Seguir al sistema",
                    ThemeMode.LIGHT to "Claro",
                    ThemeMode.DARK to "Oscuro"
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == value, onClick = { onSelect(value) })
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = glass.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = glass.accent) }
        }
    )
}

@Composable
private fun CrossfadeDialog(
    current: Int,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlass.current
    var value by remember { mutableStateOf(current.toFloat()) }
    val presets = listOf(0, 1000, 2000, 3000, 5000, 8000, 10000, 12000)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Crossfade", color = glass.textPrimary) },
        text = {
            Column {
                Text(
                    "Las canciones se solapan con un fundido de volumen. Durante la transición se usan dos motores de audio para evitar cortes.",
                    color = glass.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (value <= 0f) "Desactivado" else "${(value / 1000f).let { if (it == it.toInt().toFloat()) it.toInt().toString() else String.format("%.1f", it) }} s",
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.accent
                )
                Slider(
                    value = value,
                    onValueChange = {
                        value = (it / 500f).roundToInt() * 500f
                        onSet(value.toInt())
                    },
                    valueRange = 0f..12000f,
                    colors = SliderDefaults.colors(
                        thumbColor = glass.accent,
                        activeTrackColor = glass.accent,
                        inactiveTrackColor = glass.textSecondary.copy(alpha = 0.25f)
                    )
                )
                presets.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { preset ->
                            val selected = value.toInt() == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) glass.accent else glass.glassFill
                                    )
                                    .clickable {
                                        value = preset.toFloat()
                                        onSet(preset)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (preset == 0) "Off" else "${preset / 1000}s",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        if (glass.isDark) Color(0xFF07111C) else Color.White
                                    } else glass.textPrimary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo", color = glass.accent) }
        }
    )
}

@Composable
private fun WidgetInfoDialog(onDismiss: () -> Unit) {
    val glass = LocalGlass.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = glass.backgroundEnd,
        title = { Text("Widgets de ParSound", color = glass.textPrimary) },
        text = {
            Column {
                Text(
                    "Mantén pulsada un espacio libre de la pantalla de inicio, entra en Widgets y busca ParSound. Hay tres distribuciones que se adaptan al tamaño del widget.",
                    color = glass.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Compacto: portada, título y reproducir/pausar.\nMediano: añade artista y anterior/siguiente.\nGrande: portada destacada, información y controles.",
                    color = glass.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "La transparencia depende del launcher. Los widgets de Android no permiten desenfoque dinámico real, así que ParSound usa un degradado translúcido estable con los colores de la portada.",
                    color = glass.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido", color = glass.accent) }
        }
    )
}
