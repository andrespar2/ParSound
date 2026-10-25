package com.andrespar2.parsound.ui.screen.effects

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.andrespar2.parsound.player.EqBand
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.GlassCard
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.util.formatDuration
import kotlin.math.roundToInt

@Composable
fun EffectsScreen(
    navController: NavController,
    viewModel: EffectsViewModel = hiltViewModel()
) {
    val glass = LocalGlass.current
    val eqState by viewModel.eqState.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val sleepRemaining by viewModel.sleepRemaining.collectAsState()

    AmbientBackground(showArtwork = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParSoundTopBar(
                title = "Efectos de audio",
                subtitle = "Solo se muestran las funciones compatibles con tu dispositivo",
                onBack = { navController.navigateUp() }
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        strong = true
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, null, tint = glass.accent)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Velocidad de reproducción",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                String.format("%.2f×", speed),
                                color = glass.accent,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Slider(
                            value = speed,
                            onValueChange = viewModel::setSpeed,
                            valueRange = 0.5f..2f,
                            colors = SliderDefaults.colors(
                                thumbColor = glass.accent,
                                activeTrackColor = glass.accent,
                                inactiveTrackColor = glass.textSecondary.copy(alpha = 0.25f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0.5×", style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                            Text("1×", style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                            Text("2×", style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = glass.accent)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Tono",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                String.format("%.2f×", pitch),
                                color = glass.accent,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Slider(
                            value = pitch,
                            onValueChange = viewModel::setPitch,
                            valueRange = 0.5f..2f,
                            colors = SliderDefaults.colors(
                                thumbColor = glass.accent,
                                activeTrackColor = glass.accent,
                                inactiveTrackColor = glass.textSecondary.copy(alpha = 0.25f)
                            )
                        )
                        Text(
                            "El tono se ajusta con el procesador Sonic integrado en Media3, sin cambiar la velocidad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        strong = true
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Equalizer, null, tint = glass.accent)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Ecualizador",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary
                                )
                                Text(
                                    when {
                                        eqState.attached -> "Disponible para esta salida de audio"
                                        else -> "Reproduce una canción para activarlo"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                            Switch(
                                checked = eqState.enabled,
                                onCheckedChange = viewModel::setEnabled,
                                enabled = eqState.attached,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = glass.backgroundStart,
                                    checkedTrackColor = glass.accent,
                                    uncheckedThumbColor = glass.textSecondary,
                                    uncheckedTrackColor = glass.glassStrong
                                )
                            )
                        }

                        if (eqState.attached) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Presets",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.textSecondary
                            )
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    EffectChip(
                                        label = "Personalizado",
                                        selected = eqState.preset < 0,
                                        onClick = { viewModel.setPreset(-1) }
                                    )
                                }
                                items(eqState.presetNames.size) { index ->
                                    EffectChip(
                                        label = eqState.presetNames[index],
                                        selected = eqState.preset == index,
                                        onClick = { viewModel.setPreset(index) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            eqState.bands.forEach { band ->
                                EqBandControl(band) { value ->
                                    viewModel.setBand(band.index, value)
                                }
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Android expone el ecualizador a través del efecto de audio de la sesión de reproducción. En algunos dispositivos o salidas externas puede no estar disponible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        strong = true
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = glass.accent)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Temporizador de apagado",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary
                                )
                                Text(
                                    if (sleepRemaining > 0) "Se pausará en ${formatDuration(sleepRemaining)}"
                                    else "Detiene la reproducción tras el tiempo indicado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                            if (sleepRemaining > 0) {
                                TextButton(onClick = viewModel::cancelSleep) {
                                    Text("Cancelar", color = glass.accent)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val presets = listOf(5, 10, 15, 30, 45, 60, 90, 120)
                        presets.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { minutes ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(glass.glassFill)
                                            .clickable { viewModel.startSleep(minutes) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$minutes min",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = glass.textPrimary
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val glass = LocalGlass.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) glass.accent else glass.glassFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                if (glass.isDark) Color(0xFF07111C) else Color.White
            } else glass.textPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun EqBandControl(band: EqBand, onLevel: (Int) -> Unit) {
    val glass = LocalGlass.current
    val range = (band.maxMb - band.minMb).coerceAtLeast(1)
    var local by remember(band.index, band.levelMb) { mutableStateOf(band.levelMb.toFloat()) }
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (band.frequencyHz >= 1000) "${band.frequencyHz / 1000} kHz" else "${band.frequencyHz} Hz",
                style = MaterialTheme.typography.labelSmall,
                color = glass.textSecondary,
                modifier = Modifier.width(56.dp)
            )
            Slider(
                value = local,
                onValueChange = { local = it },
                onValueChangeFinished = { onLevel(local.roundToInt()) },
                valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = glass.accent,
                    activeTrackColor = glass.accent.copy(alpha = 0.75f),
                    inactiveTrackColor = glass.textSecondary.copy(alpha = 0.2f)
                )
            )
            Text(
                "${(local / 100).roundToInt()} dB",
                style = MaterialTheme.typography.labelSmall,
                color = glass.textSecondary,
                modifier = Modifier.width(48.dp)
            )
        }
    }
}
