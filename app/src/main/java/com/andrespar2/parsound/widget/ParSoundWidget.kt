package com.andrespar2.parsound.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class ParSoundWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ParSoundWidgetContent()
        }
    }

    @Composable
    private fun ParSoundWidgetContent() {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column {
                Text(text = "ParSound", style = TextStyle(color = ColorProvider(Color(0xFFE8EAED))))
                Text(text = "Reproductor local", style = TextStyle(color = ColorProvider(Color(0xFF9AA0A6))))
            }
            Spacer(modifier = GlanceModifier.width(12.dp))
        }
    }
}

class ParSoundWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ParSoundWidget()
}
