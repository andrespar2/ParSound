package com.andrespar2.parsound.ui.screen.licenses

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andrespar2.parsound.ui.components.AmbientBackground
import com.andrespar2.parsound.ui.components.GlassCard
import com.andrespar2.parsound.ui.components.ParSoundTopBar
import com.andrespar2.parsound.ui.theme.LocalGlass
import com.andrespar2.parsound.util.openUri

private data class LicenseItem(
    val name: String,
    val license: String,
    val url: String
)

@Composable
fun LicensesScreen(navController: NavController) {
    val glass = LocalGlass.current
    val context = LocalContext.current
    val items = listOf(        LicenseItem("AndroidX / Jetpack", "Apache License 2.0", "https://github.com/androidx"),
        LicenseItem("Jetpack Compose", "Apache License 2.0", "https://developer.android.com/jetpack/compose"),
        LicenseItem("Material Components for Android", "Apache License 2.0", "https://github.com/material-components/material-components-android"),
        LicenseItem("Kotlin y kotlinx.coroutines", "Apache License 2.0", "https://github.com/Kotlin"),
        LicenseItem("Android Media3 / ExoPlayer", "Apache License 2.0", "https://github.com/androidx/media"),
        LicenseItem("Hilt y Dagger", "Apache License 2.0", "https://github.com/google/dagger"),
        LicenseItem("Room", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
        LicenseItem("DataStore", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/datastore"),
        LicenseItem("Navigation Compose", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/navigation"),
        LicenseItem("Coil", "Apache License 2.0", "https://github.com/coil-kt/coil"),
        LicenseItem("Glance AppWidget", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/glance"),
        LicenseItem("AndroidX Palette", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/palette"),
        LicenseItem("DocumentFile", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/documentfile")
    )

    AmbientBackground(showArtwork = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            ParSoundTopBar(
                title = "Licencias",
                subtitle = "Código abierto que hace posible ParSound",
                onBack = { navController.navigateUp() }
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        strong = true
                    ) {
                        Text(
                            "ParSound",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Copyright © 2026 andrespar2. Distribuido bajo licencia MIT. El código fuente está disponible en GitHub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
                items(items) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .clickable { context.openUri(item.url) }
                    ) {
                        GlassCard(padding = 14.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = glass.textPrimary
                                    )
                                    Text(
                                        item.license,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary
                                    )
                                }
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Abrir enlace",
                                    tint = glass.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Las licencias completas de cada biblioteca están disponibles en sus repositorios oficiales. Los iconos de Material pertenecen a Google y se usan bajo Apache 2.0.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}
