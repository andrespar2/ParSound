package com.andrespar2.parsound.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.andrespar2.parsound.MainActivity
import com.andrespar2.parsound.R
import com.andrespar2.parsound.player.PlayerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject
    lateinit var manager: PlayerManager

    override fun onCreate() {
        super.onCreate()
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, manager.player)
            .setSessionActivity(sessionActivity)
            .build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.notification_channel_name)
                .build()
        )
        scope.launch {
            manager.restoreSessionOnce()
            manager.updateWidget()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null && action in COMMANDS) {
            scope.launch {
                if (manager.player.mediaItemCount == 0) {
                    manager.restoreSessionOnce()
                }
                when (action) {
                    ACTION_TOGGLE -> {
                        if (manager.player.mediaItemCount > 0) manager.toggle()
                    }
                    ACTION_NEXT -> {
                        if (manager.player.mediaItemCount > 0) {
                            manager.next()
                            if (!manager.player.playWhenReady) manager.play()
                        }
                    }
                    ACTION_PREVIOUS -> {
                        if (manager.player.mediaItemCount > 0) {
                            manager.previous()
                            if (!manager.player.playWhenReady) manager.play()
                        }
                    }
                    ACTION_PLAY -> manager.play()
                    ACTION_PAUSE -> manager.pause()
                }
                manager.updateWidget(true)
            }
            return START_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!manager.player.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_TOGGLE = "com.andrespar2.parsound.action.TOGGLE"
        const val ACTION_NEXT = "com.andrespar2.parsound.action.NEXT"
        const val ACTION_PREVIOUS = "com.andrespar2.parsound.action.PREVIOUS"
        const val ACTION_PLAY = "com.andrespar2.parsound.action.PLAY"
        const val ACTION_PAUSE = "com.andrespar2.parsound.action.PAUSE"

        private val COMMANDS = setOf(ACTION_TOGGLE, ACTION_NEXT, ACTION_PREVIOUS, ACTION_PLAY, ACTION_PAUSE)
    }
}
