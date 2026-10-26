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
    val wide = size.width >= 240.dp
    val textWidth = (size.width - if (wide) 164.dp else 98.dp)
        .coerceAtLeast(34.dp)
        .coerceAtMost(400.dp)
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(
            artwork,
            38,
            GlanceModifier.clickable(actionStartActivity<MainActivity>())
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(
            modifier = GlanceModifier
                .width(textWidth)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Text(
                snapshot.title,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 12.sp,
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
        if (wide) {
            WidgetControl(WidgetIcon.Previous, 92, 29, snapshot)
            Spacer(GlanceModifier.width(4.dp))
            WidgetControl(
                if (snapshot.isPlaying) WidgetIcon.Pause else WidgetIcon.Play,
                76,
                34,
                snapshot,
                filled = true
            )
            Spacer(GlanceModifier.width(4.dp))
            WidgetControl(WidgetIcon.Next, 93, 29, snapshot)
        } else {
            WidgetControl(
                if (snapshot.isPlaying) WidgetIcon.Pause else WidgetIcon.Play,
                76,
                34,
                snapshot,
                filled = true
            )
        }
    }
}

@Composable
private fun MediumLayout(snapshot: WidgetSnapshot, artwork: Bitmap?, size: DpSize) {
    val textWidth = (size.width - 92.dp).coerceAtLeast(48.dp).coerceAtMost(360.dp)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(
                artwork,
                60,
                GlanceModifier.clickable(actionStartActivity<MainActivity>())
            )
            Spacer(GlanceModifier.width(11.dp))
            Column(
                modifier = GlanceModifier
                    .width(textWidth)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    "PARSOUND",
                    style = TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0x99FFFFFF))
                    )
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    snapshot.title,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                Spacer(GlanceModifier.height(1.dp))
                Text(
                    snapshot.artist,
                    maxLines = 1,
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xBFFFFFFF)))
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetControl(WidgetIcon.Previous, 92, 34, snapshot)
            Spacer(GlanceModifier.width(10.dp))
            WidgetControl(
                if (snapshot.isPlaying) WidgetIcon.Pause else WidgetIcon.Play,
                76,
                40,
                snapshot,
                filled = true
            )
            Spacer(GlanceModifier.width(10.dp))
            WidgetControl(WidgetIcon.Next, 93, 34, snapshot)
        }
    }
}

@Composable
private fun LargeLayout(snapshot: WidgetSnapshot, artwork: Bitmap?, size: DpSize) {
    val textWidth = (size.width - 144.dp).coerceAtLeast(60.dp).coerceAtMost(420.dp)
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
                94,
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
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xAAFFFFFF))
                    )
                )
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    snapshot.title,
                    maxLines = 2,
                    style = TextStyle(
                        fontSize = 17.sp,
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
        Spacer(GlanceModifier.height(12.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetControl(WidgetIcon.Previous, 92, 40, snapshot)
            Spacer(GlanceModifier.width(14.dp))
            WidgetControl(
                if (snapshot.isPlaying) WidgetIcon.Pause else WidgetIcon.Play,
                76,
                46,
                snapshot,
                filled = true
            )
            Spacer(GlanceModifier.width(14.dp))
            WidgetControl(WidgetIcon.Next, 93, 40, snapshot)
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

private enum class WidgetIcon { Play, Pause, Previous, Next }

private val widgetIconCache = HashMap<String, Bitmap>()

private fun widgetIcon(kind: WidgetIcon, color: Int): Bitmap {
    val key = "${kind.name}-$color"
    widgetIconCache[key]?.let { return it }
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    when (kind) {
        WidgetIcon.Play -> {
            val path = android.graphics.Path().apply {
                moveTo(30f, 20f)
                lineTo(74f, 48f)
                lineTo(30f, 76f)
                close()
            }
            canvas.drawPath(path, paint)
        }
        WidgetIcon.Pause -> {
            canvas.drawRoundRect(28f, 22f, 42f, 74f, 5f, 5f, paint)
            canvas.drawRoundRect(54f, 22f, 68f, 74f, 5f, 5f, paint)
        }
        WidgetIcon.Previous -> {
            canvas.drawRoundRect(24f, 22f, 33f, 74f, 4f, 4f, paint)
            val path = android.graphics.Path().apply {
                moveTo(72f, 22f)
                lineTo(37f, 48f)
                lineTo(72f, 74f)
                close()
            }
            canvas.drawPath(path, paint)
        }
        WidgetIcon.Next -> {
            val path = android.graphics.Path().apply {
                moveTo(24f, 22f)
                lineTo(59f, 48f)
                lineTo(24f, 74f)
                close()
            }
            canvas.drawPath(path, paint)
            canvas.drawRoundRect(63f, 22f, 72f, 74f, 4f, 4f, paint)
        }
    }
    widgetIconCache[key] = bitmap
    return bitmap
}

@Composable
private fun WidgetControl(
    icon: WidgetIcon,
    action: Int,
    size: Int,
    snapshot: WidgetSnapshot,
    filled: Boolean = false
) {
    val accent = snapshot.accent
    val accentLuma = (
        ((accent shr 16) and 0xFF) * 0.299f +
            ((accent shr 8) and 0xFF) * 0.587f +
            (accent and 0xFF) * 0.114f
        )
    val iconColor = if (filled && accentLuma > 150f) 0xFF07111C.toInt() else 0xFFFFFFFF.toInt()
    val background = if (filled) accent else 0x2BFFFFFF
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .cornerRadius((size / 2).dp)
            .background(ColorProvider(Color(background)))
            .clickable(
                actionRunCallback<WidgetCommandAction>(
                    actionParametersOf(WidgetCommandAction.actionKey to action.toString())
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(widgetIcon(icon, iconColor)),
            contentDescription = when (icon) {
                WidgetIcon.Play -> "Reproducir"
                WidgetIcon.Pause -> "Pausar"
                WidgetIcon.Previous -> "Anterior"
                WidgetIcon.Next -> "Siguiente"
            },
            contentScale = androidx.glance.layout.ContentScale.Fit,
            modifier = GlanceModifier.size((size * 0.48f).dp)
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
    val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val rounded = android.graphics.Path().apply {
        addRoundRect(
            android.graphics.RectF(0f, 0f, 192f, 192f),
            18f, 18f,
            android.graphics.Path.Direction.CW
        )
    }
    canvas.save()
    canvas.clipPath(rounded)
    val base = Paint(Paint.ANTI_ALIAS_FLAG)
    base.shader = LinearGradient(
        0f, 0f, 192f, 192f,
        intArrayOf(
            (start and 0x00FFFFFF) or (0xF7 shl 24),
            ((accent and 0x00FFFFFF) or (0x66 shl 24)),
            (start and 0x00FFFFFF) or (0xF7 shl 24)
        ),
        floatArrayOf(0f, 0.50f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, 192f, 192f, base)

    val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    glow.shader = android.graphics.RadialGradient(
        28f, 18f, 150f,
        intArrayOf(0x24FFFFFF, 0x00FFFFFF),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, 192f, 192f, glow)

    val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    shade.shader = LinearGradient(
        0f, 120f, 0f, 192f,
        intArrayOf(0x00000000, 0x22000000),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, 192f, 192f, shade)

    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0x30FFFFFF
    }
    canvas.drawRoundRect(0.75f, 0.75f, 191.25f, 191.25f, 18f, 18f, border)
    canvas.restore()
    return bitmap
}
