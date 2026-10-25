package com.andrespar2.parsound.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.core.content.ContextCompat
import com.andrespar2.parsound.MainActivity
import com.andrespar2.parsound.R
import com.andrespar2.parsound.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParSoundWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ParSoundWidget()
}

class ParSoundWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetStateStore.load(context)
        val artwork = withContext(Dispatchers.IO) {
            WidgetStateStore.artworkBitmap(context, snapshot.artworkPath, 360)
        }
        provideContent {
            GlanceTheme {
                WidgetContent(snapshot, artwork)
            }
        }
    }

    companion object {
        fun updateAll(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { ParSoundWidget().updateAll(context.applicationContext) }
                    .onFailure {
                        android.util.Log.e("ParSoundWidget", "No se pudo actualizar el widget", it)
                    }
            }
        }
    }
}

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot, artwork: Bitmap?) {
    val measured = LocalSize.current
    val width = if (measured.width.value > 0f) measured.width else 240.dp
    val height = if (measured.height.value > 0f) measured.height else 150.dp
    val size = DpSize(width, height)
    val compact = size.width < 190.dp || size.height < 145.dp
    val large = size.width >= 260.dp && size.height >= 220.dp
    val gradient = gradientBitmap(snapshot.background, snapshot.accent)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(gradient))
            .cornerRadius(24.dp)
    ) {
        if (large) {
            LargeLayout(snapshot, artwork, size)
        } else if (compact) {
            CompactLayout(snapshot, artwork, size)
        } else {
            MediumLayout(snapshot, artwork, size)
        }
    }
}

@Composable
private fun CompactLayout(snapshot: WidgetSnapshot, artwork: Bitmap?, size: DpSize) {
    val textWidth = (size.width - 92.dp).coerceAtLeast(34.dp).coerceAtMost(400.dp)
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(
            artwork,
            34,
            GlanceModifier.clickable(actionStartActivity<MainActivity>())
        )
        Spacer(GlanceModifier.width(7.dp))
        Column(
            modifier = GlanceModifier
                .width(textWidth)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Text(
                snapshot.title,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.White)
                )
            )
            if (size.width >= 158.dp) {
                Text(
                    snapshot.artist,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = ColorProvider(Color(0xCCFFFFFF))
                    )
                )
            }
        }
        Spacer(GlanceModifier.width(4.dp))
        ControlButton(if (snapshot.isPlaying) "⏸" else "▶", 76, 30)
    }
}

@Composable
private fun MediumLayout(snapshot: WidgetSnapshot, artwork: Bitmap?, size: DpSize) {
    val textWidth = (size.width - 104.dp).coerceAtLeast(40.dp).coerceAtMost(420.dp)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(
                artwork,
                64,
                GlanceModifier.clickable(actionStartActivity<MainActivity>())
            )
            Spacer(GlanceModifier.width(12.dp))
            Column(
                modifier = GlanceModifier
                    .width(textWidth)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    snapshot.title,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    snapshot.artist,
                    maxLines = 1,
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(0xBFFFFFFF)))
                )
            }
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton("⏮", 92, 34)
            Spacer(GlanceModifier.width(8.dp))
            ControlButton(if (snapshot.isPlaying) "⏸" else "▶", 76, 38)
            Spacer(GlanceModifier.width(8.dp))
            ControlButton("⏭", 93, 34)
        }
    }
}

@Composable
private fun LargeLayout(snapshot: WidgetSnapshot, artwork: Bitmap?, size: DpSize) {
    val textWidth = (size.width - 146.dp).coerceAtLeast(50.dp).coerceAtMost(460.dp)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(
                artwork,
                96,
                GlanceModifier.clickable(actionStartActivity<MainActivity>())
            )
            Spacer(GlanceModifier.width(14.dp))
            Column(
                modifier = GlanceModifier
                    .width(textWidth)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    "PARSOUND",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0x99FFFFFF))
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    snapshot.title,
                    maxLines = 2,
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    snapshot.artist,
                    maxLines = 1,
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(0xBFFFFFFF)))
                )
            }
        }
        Spacer(GlanceModifier.height(14.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton("⏮", 100, 40)
            Spacer(GlanceModifier.width(12.dp))
            ControlButton(if (snapshot.isPlaying) "⏸" else "▶", 80, 44)
            Spacer(GlanceModifier.width(12.dp))
            ControlButton("⏭", 94, 40)
        }
    }
}

@Composable
private fun Cover(artwork: Bitmap?, dimension: Int, modifier: GlanceModifier = GlanceModifier) {
    if (artwork != null) {
        Image(
            provider = ImageProvider(artwork),
            contentDescription = null,
            contentScale = androidx.glance.layout.ContentScale.Crop,
            modifier = modifier
                .size(dimension.dp)
                .cornerRadius((dimension / 6).dp)
        )
    } else {
        Box(
            modifier = modifier
                .size(dimension.dp)
                .cornerRadius((dimension / 6).dp)
                .background(ColorProvider(Color(0x33FFFFFF))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "♪",
                style = TextStyle(
                    fontSize = (dimension / 2).sp,
                    color = ColorProvider(Color(0xCCFFFFFF))
                )
            )
        }
    }
}

@Composable
private fun ControlButton(label: String, action: Int, size: Int) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .cornerRadius((size / 2).dp)
            .background(ColorProvider(Color(0x26FFFFFF)))
            .clickable(
                actionRunCallback<WidgetCommandAction>(
                    actionParametersOf(WidgetCommandAction.actionKey to action.toString())
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize = if (size > 40) 22.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.White)
            )
        )
    }
}

class WidgetCommandAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val raw = parameters[actionKey] ?: return
        val action = raw.toIntOrNull() ?: return
        val command = when (action) {
            76, 69 -> PlaybackService.ACTION_TOGGLE
            92 -> PlaybackService.ACTION_PREVIOUS
            93 -> PlaybackService.ACTION_NEXT
            else -> return
        }
        val intent = Intent(context, PlaybackService::class.java).setAction(command)
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    companion object {
        val actionKey = ActionParameters.Key<String>("widget_action")
    }
}
private fun gradientBitmap(start: Int, accent: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(
        0f, 0f, 96f, 96f,
        intArrayOf(
            (start and 0x00FFFFFF) or (0xF2 shl 24),
            ((accent and 0x00FFFFFF) or (0x55 shl 24)),
            (start and 0x00FFFFFF) or (0xF2 shl 24)
        ),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawRoundRect(0f, 0f, 96f, 96f, 8f, 8f, paint)
    return bitmap
}
