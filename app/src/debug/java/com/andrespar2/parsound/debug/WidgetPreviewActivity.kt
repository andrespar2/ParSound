package com.andrespar2.parsound.debug

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.andrespar2.parsound.widget.ParSoundWidgetReceiver

/**
 * Herramienta exclusiva de depuración: hospeda el widget real de ParSound para
 * comprobar que las RemoteViews de Glance se inflan y actualizan correctamente.
 */
class WidgetPreviewActivity : Activity() {
    private lateinit var host: AppWidgetHost
    private lateinit var container: FrameLayout
    private lateinit var status: TextView
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        host = AppWidgetHost(this, 4200)
        host.startListening()
        widgetId = host.allocateAppWidgetId()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(12, 12, 12, 12)
        }
        status = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            text = "Widget id=$widgetId"
        }
        container = FrameLayout(this)
        root.addView(status)
        val previewWidth = intent.getIntExtra("preview_width", 360)
        val previewHeight = intent.getIntExtra("preview_height", 190)
        root.addView(
            container,
            LinearLayout.LayoutParams(
                (previewWidth * resources.displayMetrics.density).toInt(),
                (previewHeight * resources.displayMetrics.density).toInt()
            )
        )
        setContentView(root)

        val provider = ComponentName(this, ParSoundWidgetReceiver::class.java)
        val manager = AppWidgetManager.getInstance(this)
        val allowed = manager.bindAppWidgetIdIfAllowed(widgetId, provider)
        Log.i("ParSoundWidgetPreview", "bind allowed=$allowed id=$widgetId")
        status.text = "Widget id=$widgetId bind=$allowed ${previewWidth}x$previewHeight"
        if (!allowed) {
            status.append("\nEjecuta: adb shell appwidget grantbind --package $packageName")
            return
        }
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, previewWidth - 20)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, previewWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, previewHeight - 20)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, previewHeight)
        }
        manager.updateAppWidgetOptions(widgetId, options)
        attach()
    }

    private fun attach() {
        val manager = AppWidgetManager.getInstance(this)
        val info = manager.getAppWidgetInfo(widgetId)
        if (info == null) {
            status.append("\nSin AppWidgetInfo")
            return
        }
        val view = host.createView(this, widgetId, info)
        container.removeAllViews()
        container.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
        status.append("\nProveedor: ${info.provider.flattenToShortString()}")
    }

    override fun onDestroy() {
        runCatching {
            host.deleteAppWidgetId(widgetId)
            host.stopListening()
        }
        super.onDestroy()
    }
}
