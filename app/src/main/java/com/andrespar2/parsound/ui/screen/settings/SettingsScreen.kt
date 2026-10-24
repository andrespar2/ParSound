package com.andrespar2.parsound.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Apariencia",
                modifier = Modifier.padding(16.dp)
            )
            ListItem(
                headlineContent = { Text("Tema oscuro") },
                supportingContent = { Text("Predeterminado activo") }
            )
            ListItem(
                headlineContent = { Text("Modo AMOLED completamente negro") },
                supportingContent = { Text("Disponible en ajustes avanzados") }
            )
            ListItem(
                headlineContent = { Text("Colores adaptativos de portada") },
                supportingContent = { Text("Liquid Glass adaptable") }
            )
            HorizontalDivider()
            Text(
                text = "Reproducción",
                modifier = Modifier.padding(16.dp)
            )
            ListItem(
                headlineContent = { Text("Crossfade configurable") },
                supportingContent = { Text("Desactivado / 1s / 2s / 3s / 5s / 8s / 10s / 12s") }
            )
            ListItem(
                headlineContent = { Text("Temporizador de apagado") },
                supportingContent = { Text("Disponible próximamente") }
            )
            HorizontalDivider()
            Text(
                text = "Descargas",
                modifier = Modifier.padding(16.dp)
            )
            ListItem(
                headlineContent = { Text("Descargar música gratis") },
                supportingContent = { Text("Abrir repositorio ParDL en GitHub (proyecto externo)") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/andrespar2/ParDL"))
                    context.startActivity(intent)
                }
            )
            HorizontalDivider()
            Text(
                text = "Información",
                modifier = Modifier.padding(16.dp)
            )
            ListItem(
                headlineContent = { Text("ParSound") },
                supportingContent = { Text("Desarrollado por andrespar2") }
            )
            ListItem(
                headlineContent = { Text("Versión") },
                supportingContent = { Text("1.0.0") }
            )
            ListItem(
                headlineContent = { Text("Repositorio ParSound") },
                supportingContent = { Text("https://github.com/andrespar2/ParSound") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/andrespar2/ParSound"))
                    context.startActivity(intent)
                }
            )
        }
    }
}
