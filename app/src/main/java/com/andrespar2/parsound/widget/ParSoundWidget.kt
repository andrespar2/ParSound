package com.andrespar2.parsound.widget

import android.content.ComponentName
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
import androidx.core.content.ContextCompat
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
import androidx.glance.layout.absolutePadding
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andrespar2.parsound.MainActivity
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

/**
 * Widget pensado como la barra de reproducción de la app: portada, título,
 * artista, controles y una fina línea de progreso, todo sobre el cristal
 * coloreado por la paleta de la portada actual.
 */
@Composable
private fun WidgetContent(snapshot: WidgetSnapshot, artwork: Bitmap?) {
    val measured = LocalSize.current
    val width = measured.width.value.takeIf { it > 0f } ?: 250f
    val height = measured.height.value.takeIf { it > 0f } ?: 70f
    val size = DpSize(width.dp, height.dp)
    val compact = height < 92f || width < 196f
    val large = width >= 290f && height >= 140f
    val gradient = gradientBitmap(snapshot.background, snapshot.accent)

    Box(modifier = GlanceModifier.fillMaxSize()) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp)
                .background(ImageProvider(gradient))
                .cornerRadius(20.dp)
        ) {
            when {
                compact -> CompactBar(snapshot, artwork, size)
                large -> LargeBar(snapshot, artwork, size)
                else -> MediumBar(snapshot, artwork, size)
            }
        }
    }
}

@Composable
private fun CompactBar(snapshot: WidgetSnapshot, artwork: Bitmap?, size: DpSize) {
    val cover = (size.height.value - 18f).toInt()
        .coerceIn(30, 54)
        .coerceAtMost((size.width.value * 0.2f).toInt().coerceAtLeast(28))
    val showArtist = size.width.value >= 205f
    val showNext = size.width.value >= 175f
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(
                artwork,
                cover,
                GlanceModifier.clickable(actionStartActivity<MainActivity>())
            )
            Spacer(GlanceModifier.width(8.dp))
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    snapshot.title,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = if (size.width.value >= 205f) 13.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                if (showArtist) {
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
            if (snapshot.songId != 0L) {
                WidgetControl(
                    if (snapshot.isPlaying) WidgetIcon.Pause else WidgetIcon.Play,
                    WidgetAction.Toggle,
                    if (showNext) 32 else 30,
                    snapshot,
                    filled = true
                )
                if (showNext) {
                    Spacer(GlanceModifier.width(2.dp))
                    WidgetControl(WidgetIcon.Next, WidgetAction.Next, 28, snapshot)
                }
            }
        }
        ProgressLine(snapshot, size.width.value, inset = 10f)
    }
}

@Composable
private fun MediumBar(snapshot: WidgetSnapshot, artwork: Bitmap?, size: DpSize) {
    val cover = (size.height.value - 30f).toInt()
        .coerceIn(40, 76)
        .coerceAtMost((size.width.value * 0.22f).toInt().coerceAtLeast(38))
    val showArtist = size.width.value >= 210f
    val showPrevious = size.width.value >= 330f
    val showNext = size.width.value >= 240f
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(
                artwork,
                cover,
                GlanceModifier.clickable(actionStartActivity<MainActivity>())
            )
            Spacer(GlanceModifier.width(11.dp))
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    snapshot.title,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = if (size.width.value >= 260f) 15.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                if (showArtist) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        snapshot.artist,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(Color(0xBFFFFFFF))
                        )
                    )
                }
                if (size.height.value >= 120f && snapshot.durationMs > 0) {
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        timeLabel(snapshot),
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(Color(0x99FFFFFF))
                        )
                    )
                }
            }
            Spacer(GlanceModifier.width(8.dp))
            if (snapshot.songId != 0L) {
                if (showPrevious) {
                    WidgetControl(WidgetIcon.Previous, WidgetAction.Previous, 32, snapshot)
                    Spacer(GlanceModifier.width(6.dp))
                }
                WidgetControl(
                    if (snapshot.isPlaying) WidgetIcon.Pause else WidgetIcon.Play,
                    WidgetAction.Toggle,
                    40,
                    snapshot,
                    filled = true
                )
                if (showNext) {
                    Spacer(GlanceModifier.width(6.dp))
                    WidgetControl(WidgetIcon.Next, WidgetAction.Next, 32, snapshot)
                }
            }
        }
        ProgressLine(snapshot, size.width.value, inset = 13f)
    }
}

@Composable
private fun LargeBar(snapshot: WidgetSnapshot, artwork: Bitmap?, size: DpSize) {
    val cover = (size.height.value - 84f).toInt()
        .coerceIn(52, 112)
        .coerceAtMost((size.width.value * 0.34f).toInt().coerceAtLeast(48))
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(
                artwork,
                cover,
                GlanceModifier.clickable(actionStartActivity<MainActivity>())
            )
            Spacer(GlanceModifier.width(15.dp))
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    "PARSOUND",
                    maxLines = 1,
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    snapshot.artist,
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xBFFFFFFF))
                    )
                )
                if (snapshot.durationMs > 0) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        timeLabel(snapshot),
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(Color(0x99FFFFFF))
                        )
                    )
                }
            }
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (snapshot.songId != 0L) {
                WidgetControl(WidgetIcon.Previous, WidgetAction.Previous, 38, snapshot)
                Spacer(GlanceModifier.width(12.dp))
                WidgetControl(
                    if (snapshot.isPlaying) WidgetIcon.Pause else WidgetIcon.Play,
                    WidgetAction.Toggle,
                    46,
                    snapshot,
                    filled = true
                )
                Spacer(GlanceModifier.width(12.dp))
                WidgetControl(WidgetIcon.Next, WidgetAction.Next, 38, snapshot)
            }
        }
        ProgressLine(snapshot, size.width.value, inset = 16f)
    }
}

@Composable
private fun ProgressLine(snapshot: WidgetSnapshot, totalWidth: Float, inset: Float) {
    val trackWidth = (totalWidth - inset * 2f - 8f).coerceAtLeast(0f)
    val playedWidth = (trackWidth * snapshot.progress).coerceIn(0f, trackWidth)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .absolutePadding(left = (inset + 4f).dp, top = 0.dp, right = (inset + 4f).dp, bottom = 6.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .width(trackWidth.dp)
                .height(2.5.dp)
                .background(ColorProvider(Color(0x33FFFFFF)))
                .cornerRadius(1.5.dp)
        ) {
            Spacer(
                GlanceModifier
                    .width(playedWidth.dp)
                    .height(2.5.dp)
                    .background(ColorProvider(Color(snapshot.accent)))
                    .cornerRadius(1.5.dp)
            )
        }
    }
}

private fun timeLabel(snapshot: WidgetSnapshot): String {
    fun format(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val minutes = total / 60
        val seconds = total % 60
        return "%d:%02d".format(minutes, seconds)
    }
    return "${format(snapshot.positionMs)} / ${format(snapshot.durationMs)}"
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
                .cornerRadius((dimension / 5).dp)
        )
    } else {
        Box(
            modifier = modifier
                .size(dimension.dp)
                .cornerRadius((dimension / 5).dp)
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

private enum class WidgetAction { Toggle, Previous, Next }

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
    action: WidgetAction,
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
                    actionParametersOf(WidgetCommandAction.actionKey to action.name)
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
        val action = parameters[actionKey]?.let { value ->
            runCatching { WidgetAction.valueOf(value) }.getOrNull()
        } ?: return
        val command = when (action) {
            WidgetAction.Toggle -> PlaybackService.ACTION_TOGGLE
            WidgetAction.Previous -> PlaybackService.ACTION_PREVIOUS
            WidgetAction.Next -> PlaybackService.ACTION_NEXT
        }
        val intent = Intent(context, PlaybackService::class.java).setAction(command)
        val started = runCatching {
            ContextCompat.startForegroundService(context, intent)
        }.isSuccess
        if (!started) {
            // En Android 12+ el arranque en primer plano desde segundo plano puede
            // estar limitado: se controla la sesión activa mediante MediaController.
            runCatching { sendThroughSession(context, action) }
        }
    }

    private fun sendThroughSession(context: Context, action: WidgetAction) {
        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context.applicationContext, token).buildAsync()
        future.addListener({
            runCatching {
                val controller = future.get()
                try {
                    when (action) {
                        WidgetAction.Toggle ->
                            if (controller.isPlaying) controller.pause() else controller.play()
                        WidgetAction.Previous -> controller.seekToPreviousMediaItem()
                        WidgetAction.Next -> controller.seekToNextMediaItem()
                    }
                } finally {
                    controller.release()
                }
            }
        }, ContextCompat.getMainExecutor(context))
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
            (start and 0x00FFFFFF) or (0xE8 shl 24),
            ((accent and 0x00FFFFFF) or (0x5C shl 24)),
            (start and 0x00FFFFFF) or (0xE8 shl 24)
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
