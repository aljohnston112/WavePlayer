package io.fourth_finger.pinky_player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.lifecycle.Observer
import io.fourth_finger.playlist_data_source.AudioUri
import io.fourth_finger.pinky_player.random_playlist.MediaSession

class RemoteViewCreator(
    private val packageName: String,
    context: Context,
    private var mediaSession: MediaSession?
) {

    private var isPlaying = false
    private var songName = ""
    private var notificationHasArt = false
    private var songArt: Bitmap? = null

    private var songObserver: Observer<AudioUri?>? = Observer { value ->
        if(value == null){
            return@Observer
        }
        songName = value.title
        songArt = value.getBitmap(context)
        createRemoteView(context)
    }

    private var isPlayingObserver: Observer<Boolean>? = Observer { isPlaying ->
        this.isPlaying = isPlaying
        createRemoteView(context)
    }

    init {
        songObserver?.let {
            mediaSession?.currentAudioUri?.observeForever(it)
        }
        isPlayingObserver?.let {
            mediaSession?.isPlaying?.observeForever(it)
        }
    }

    fun createRemoteView(
        context: Context,
        it: AudioUri?
    ): RemoteViews {
        if(it != null) {
            songName = it.title
            songArt?.recycle()
            songArt = it.getBitmap(context)
        }
        return createRemoteView(context)
    }

    fun createRemoteView(
        context: Context,
        isPlaying: Boolean
    ): RemoteViews {
        this@RemoteViewCreator.isPlaying = isPlaying
        createRemoteView(context)
        return createRemoteView(context)
    }

    fun createRemoteView(context: Context): RemoteViews {
        val remoteViewsWithoutArt = RemoteViews(
            packageName,
            R.layout.pane_notification_without_art
        )
        val remoteViewsWithArt = RemoteViews(
            packageName,
            R.layout.pane_notification_with_art
        )
        val remoteViewsNotification = RemoteViews(
            packageName,
            R.layout.pane_notification
        )
        setUpBroadCastsForNotificationButtons(
            context,
            remoteViewsWithArt,
            remoteViewsWithoutArt
        )
        updatePlayButton(
            remoteViewsWithArt,
            remoteViewsWithoutArt
        )
        updateNotificationSongName(
            remoteViewsWithArt,
            remoteViewsWithoutArt
        )
        updateSongArt(
            remoteViewsWithArt,
            remoteViewsWithoutArt
        )
        return updateNotification(
            remoteViewsWithArt,
            remoteViewsWithoutArt,
            remoteViewsNotification
        )
    }

    private fun setUpBroadCastsForNotificationButtons(
        context: Context,
        remoteViewsWithArt: RemoteViews,
        remoteViewsWithoutArt: RemoteViews
    ) {
        setUpBroadcastNext(
            context,
            remoteViewsWithArt,
            remoteViewsWithoutArt
        )
        setUpBroadcastPlayPause(
            context,
            remoteViewsWithArt,
            remoteViewsWithoutArt
        )
        setUpBroadcastPrevious(
            context,
            remoteViewsWithArt,
            remoteViewsWithoutArt
        )
    }

    private fun setUpBroadcastNext(
        context: Context,
        remoteViewsWithArt: RemoteViews,
        remoteViewsWithoutArt: RemoteViews
    ) {
        val intentNext = Intent(context.resources.getString(R.string.action_next))
        intentNext.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentNext =
            PendingIntent.getBroadcast(
                context,
                0,
                intentNext,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        remoteViewsWithoutArt.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPaneNext,
            pendingIntentNext
        )
        remoteViewsWithArt.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPaneNextWithArt,
            pendingIntentNext
        )
    }

    private fun setUpBroadcastPlayPause(
        context: Context,
        remoteViewsWithArt: RemoteViews,
        remoteViewsWithoutArt: RemoteViews
    ) {
        val intentPlayPause = Intent(context.resources.getString(R.string.action_play_pause))
        intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentPlayPause = PendingIntent.getBroadcast(
            context,
            0,
            intentPlayPause,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViewsWithoutArt.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPanePlayPause,
            pendingIntentPlayPause
        )
        remoteViewsWithArt.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPanePlayPauseWithArt,
            pendingIntentPlayPause
        )
    }

    private fun setUpBroadcastPrevious(
        context: Context,
        remoteViewsWithArt: RemoteViews,
        remoteViewsWithoutArt: RemoteViews
    ) {
        val intentPrev = Intent(context.resources.getString(R.string.action_previous))
        intentPrev.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentPrev = PendingIntent.getBroadcast(
            context,
            0,
            intentPrev,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViewsWithoutArt.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPanePrev,
            pendingIntentPrev
        )
        remoteViewsWithArt.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPanePrevWithArt,
            pendingIntentPrev
        )
    }

    private fun updateSongArt(
        remoteViewsWithArt: RemoteViews,
        remoteViewsWithoutArt: RemoteViews,
    ) {

        notificationHasArt = if (songArt != null) {
            remoteViewsWithArt.setImageViewBitmap(
                R.id.imageViewNotificationSongPaneSongArtWithArt,
                songArt
            )
            true
        } else {
            remoteViewsWithoutArt.setImageViewResource(
                R.id.imageViewNotificationSongPaneSongArt,
                R.drawable.music_note_black_48dp
            )
            false
        }
    }

    private fun updateNotificationSongName(
        remoteViewsWithArt: RemoteViews,
        remoteViewsWithoutArt: RemoteViews,
    ) {
        remoteViewsWithArt.setTextViewText(
            R.id.textViewNotificationSongPaneSongNameWithArt,
            songName
        )
        remoteViewsWithoutArt.setTextViewText(
            R.id.textViewNotificationSongPaneSongName,
            songName
        )
    }

    private fun updatePlayButton(
        remoteViewsWithArt: RemoteViews,
        remoteViewsWithoutArt: RemoteViews,
    ) {
        if (isPlaying) {
            remoteViewsWithArt.setImageViewResource(
                R.id.imageButtonNotificationSongPanePlayPauseWithArt,
                R.drawable.pause_black_24dp
            )
            remoteViewsWithoutArt.setImageViewResource(
                R.id.imageButtonNotificationSongPanePlayPause,
                R.drawable.pause_black_24dp
            )
        } else {
            remoteViewsWithArt.setImageViewResource(
                R.id.imageButtonNotificationSongPanePlayPauseWithArt,
                R.drawable.play_arrow_black_24dp
            )
            remoteViewsWithoutArt.setImageViewResource(
                R.id.imageButtonNotificationSongPanePlayPause,
                R.drawable.play_arrow_black_24dp
            )
        }
    }

    private fun updateNotification(
        remoteViewsWithArt: RemoteViews,
        remoteViewsWithoutArt: RemoteViews,
        remoteViewsNotification: RemoteViews
    ): RemoteViews {
        remoteViewsNotification.removeAllViews(
            R.id.pane_notification_linear_layout
        )
        if (notificationHasArt) {
            remoteViewsNotification.addView(
                R.id.pane_notification_linear_layout,
                remoteViewsWithArt
            )
        } else {
            remoteViewsNotification.addView(
                R.id.pane_notification_linear_layout,
                remoteViewsWithoutArt
            )
        }
        return remoteViewsNotification

    }

    fun cleanUp() {
        songObserver?.let {
            mediaSession?.currentAudioUri?.removeObserver(it)
        }
        songObserver = null
        isPlayingObserver?.let {
            mediaSession?.isPlaying?.removeObserver(it)
        }
        isPlayingObserver = null
        mediaSession = null
    }

}