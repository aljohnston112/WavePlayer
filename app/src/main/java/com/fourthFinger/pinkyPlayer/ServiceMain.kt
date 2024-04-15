package com.fourthFinger.pinkyPlayer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.getActivity
import android.app.PendingIntent.getBroadcast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import com.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.io.PrintWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ServiceMain : LifecycleService() {

    private var remoteViewsNotificationLayout: RemoteViews? = null
    private var remoteViewsNotificationLayoutWithoutArt: RemoteViews? = null
    private var remoteViewsNotificationLayoutWithArt: RemoteViews? = null

    private var notificationHasArt = false

    private var notificationCompatBuilder: NotificationCompat.Builder? = null
    private var notification: Notification? = null

    private var serviceStarted = false
    private val serviceMainBinder: IBinder = ServiceMainBinder()

    private lateinit var mediaPlayerManager: MediaPlayerManager

    // region lifecycle
    // region onCreate
    override fun onCreate() {
        super.onCreate()
        mediaPlayerManager = (application as ApplicationMain).mediaPlayerManager
        setUpExceptionSaver()
        logLastThrownException()
        setUpBroadCastReceivers()
    }

    private fun setUpExceptionSaver() {
        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable ->
            val file = File(baseContext.filesDir, FILE_ERROR_LOG)
            file.delete()
            try {
                PrintWriter(file).use { pw ->
                    paramThrowable.printStackTrace(pw)
                    pw.flush()
                    paramThrowable.printStackTrace()
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            throw paramThrowable
        }
    }

    private fun logLastThrownException() {
        val file = File(baseContext.filesDir, FILE_ERROR_LOG)
        if (file.exists()) {
            try {
                BufferedReader(FileReader(file)).use { bufferedReader ->
                    val stringBuilder: StringBuilder = StringBuilder()
                    var sCurrentLine: String?
                    while ((bufferedReader.readLine().also { sCurrentLine = it }) != null) {
                        stringBuilder.append(sCurrentLine)
                    }
                    Log.e(TAG_ERROR, stringBuilder.toString())
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setUpBroadCastReceivers() {
        val intentFilter = IntentFilter()
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(resources.getString(R.string.action_next))
        intentFilter.addAction(resources.getString(R.string.action_previous))
        intentFilter.addAction(resources.getString(R.string.action_play_pause))
        intentFilter.addAction(resources.getString(R.string.action_new_song))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, intentFilter)
        }
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            synchronized(LOCK) {
                val action: String? = intent.action
                if (action != null) {
                    val mediaSession = (application as ApplicationMain).mediaSession
                    when (action) {
                        resources.getString(
                            R.string.action_next
                        ) -> {
                            mediaSession.playNext(applicationContext)
                        }
                        resources.getString(
                            R.string.action_play_pause
                        ) -> {
                            mediaSession.pauseOrPlay(applicationContext)
                        }
                        resources.getString(
                            R.string.action_previous
                        ) -> {
                            mediaSession.playPrevious(applicationContext)
                        }
                        resources.getString(
                            R.string.action_new_song
                        ) -> {
                            //updateNotification()
                            val notificationManager: NotificationManager =
                                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(NOTIFICATION_CHANNEL_ID.hashCode(), notification)
                        }
                    }
                }
            }
        }
    }

    // endregion onCreate
    // region onStartCommand
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!serviceStarted) {
            setUpNotificationBuilder()
            setUpBroadCastsForNotificationButtons()
            updateNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_CHANNEL_ID.hashCode(),
                    notification!!,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(
                    NOTIFICATION_CHANNEL_ID.hashCode(),
                    notification
                )
            }
            mediaPlayerManager.currentAudioUri.observe(this) {
                setUpNotificationBuilder()
                setUpBroadCastsForNotificationButtons()
                updateSongArt(it)
                updateNotificationSongName(it)
                updateNotification()
            }
            mediaPlayerManager.isPlaying.observe(this) {
                setUpNotificationBuilder()
                setUpBroadCastsForNotificationButtons()
                updateNotificationPlayButton(it)
                updateNotification()
            }
        }
        serviceStarted = true
        return START_STICKY
    }

    private fun updateNotification() {
        remoteViewsNotificationLayout?.removeAllViews(R.id.pane_notification_linear_layout)
        if (notificationHasArt) {
            remoteViewsNotificationLayout?.addView(
                R.id.pane_notification_linear_layout,
                remoteViewsNotificationLayoutWithArt
            )
        } else {
            remoteViewsNotificationLayout?.addView(
                R.id.pane_notification_linear_layout,
                remoteViewsNotificationLayoutWithoutArt
            )
        }
        notificationCompatBuilder?.setCustomContentView(remoteViewsNotificationLayout)
        notification = notificationCompatBuilder?.build()
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_CHANNEL_ID.hashCode(), notification)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun setUpNotificationBuilder() {
        notificationCompatBuilder = NotificationCompat.Builder(
            applicationContext,
            NOTIFICATION_CHANNEL_ID
        )
            .setOngoing(true)
            .setSmallIcon(R.drawable.music_note_black_48dp)
            .setContentTitle(NOTIFICATION_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        remoteViewsNotificationLayoutWithoutArt =
            RemoteViews(packageName, R.layout.pane_notification_without_art)
        remoteViewsNotificationLayoutWithArt =
            RemoteViews(packageName, R.layout.pane_notification_with_art)
        remoteViewsNotificationLayout = RemoteViews(packageName, R.layout.pane_notification)

        val notificationIntent = Intent(applicationContext, ActivityMain::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getActivity(
                applicationContext,
                0,
                notificationIntent,
                FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
            )
        } else {
            getActivity(
                applicationContext,
                0,
                notificationIntent,
                FLAG_CANCEL_CURRENT
            )
        }
        notificationCompatBuilder?.setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_ID,
                importance
            )
            val description = getString(R.string.description)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        // Log.v(TAG, "Done setting up notification builder");
    }

    private fun setUpBroadCastsForNotificationButtons() {
        setUpBroadcastNext()
        setUpBroadcastPlayPause()
        setUpBroadcastPrevious()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun setUpBroadcastNext() {
        val intentNext = Intent(resources.getString(R.string.action_next))
        intentNext.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentNext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getBroadcast(
                applicationContext,
                0,
                intentNext,
                FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
            )
        } else {
            getBroadcast(
                applicationContext,
                0,
                intentNext,
                FLAG_CANCEL_CURRENT
            )
        }
        remoteViewsNotificationLayoutWithoutArt?.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPaneNext, pendingIntentNext
        )
        remoteViewsNotificationLayoutWithArt?.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPaneNextWArt, pendingIntentNext
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun setUpBroadcastPlayPause() {
        val intentPlayPause = Intent(resources.getString(R.string.action_play_pause))
        intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentPlayPause = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getBroadcast(
                applicationContext,
                0,
                intentPlayPause,
                FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
            )
        } else {
            getBroadcast(
                applicationContext,
                0,
                intentPlayPause,
                FLAG_CANCEL_CURRENT
            )
        }
        remoteViewsNotificationLayoutWithoutArt?.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPanePlayPause, pendingIntentPlayPause
        )
        remoteViewsNotificationLayoutWithArt?.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPanePlayPauseWArt, pendingIntentPlayPause
        )
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun setUpBroadcastPrevious() {
        val intentPrev = Intent(resources.getString(R.string.action_previous))
        intentPrev.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentPrev = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getBroadcast(
                applicationContext,
                0,
                intentPrev,
                FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
            )
        } else {
            getBroadcast(
                applicationContext,
                0,
                intentPrev,
                FLAG_CANCEL_CURRENT
            )
        }
        remoteViewsNotificationLayoutWithoutArt?.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPanePrev, pendingIntentPrev
        )
        remoteViewsNotificationLayoutWithArt?.setOnClickPendingIntent(
            R.id.imageButtonNotificationSongPanePrevWArt, pendingIntentPrev
        )
    }

    private fun updateSongArt(audioUri: AudioUri) {
        // TODO 92? Seems to get resized for the Notification
        val bitmap = BitmapUtil.getThumbnail(
            audioUri.getUri(),
            92,
            92,
            applicationContext
        )
        notificationHasArt = if (bitmap != null) {
            remoteViewsNotificationLayoutWithArt?.setImageViewBitmap(
                R.id.imageViewNotificationSongPaneSongArtWArt,
                bitmap
            )
            true
        } else {
            remoteViewsNotificationLayoutWithoutArt?.setImageViewResource(
                R.id.imageViewNotificationSongPaneSongArt,
                R.drawable.music_note_black_48dp
            )
            false
        }
    }

    private fun updateNotificationSongName(audioUri: AudioUri) {
            remoteViewsNotificationLayoutWithArt?.setTextViewText(
                R.id.textViewNotificationSongPaneSongNameWArt,
                audioUri.title
            )
            remoteViewsNotificationLayoutWithoutArt?.setTextViewText(
                R.id.textViewNotificationSongPaneSongName,
                audioUri.title
            )
    }

    private fun updateNotificationPlayButton(isPlaying: Boolean) {
        if (isPlaying) {
                remoteViewsNotificationLayoutWithArt?.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPauseWArt, R.drawable.pause_black_24dp
                )
                remoteViewsNotificationLayoutWithoutArt?.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPause, R.drawable.pause_black_24dp
                )
        } else {
                remoteViewsNotificationLayoutWithArt?.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPauseWArt,
                    R.drawable.play_arrow_black_24dp
                )
                remoteViewsNotificationLayoutWithoutArt?.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPause, R.drawable.play_arrow_black_24dp
                )
        }
    }

    // endregion onStartCommand
    // region onBind
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return serviceMainBinder
    }

    inner class ServiceMainBinder : Binder() {
        fun getService(): ServiceMain {
            return this@ServiceMain
        }
    }

    // endregion onBind

    override fun onDestroy() {
        super.onDestroy()
        val mediaSession = (application as ApplicationMain).mediaSession
        mediaPlayerManager.cleanUp(
            applicationContext,
            mediaSession
        )
        unregisterReceiver(broadcastReceiver)
        stopSelf()
    }

    // endregion lifecycle
    // region mediaControls

    companion object {
        private val LOCK: Any = Any()
        val executorServiceFIFO: ExecutorService = Executors.newSingleThreadExecutor()
        val executorServicePool: ExecutorService = Executors.newCachedThreadPool()
        private const val TAG_ERROR: String = "ServiceMainErrors"
        private const val FILE_ERROR_LOG: String = "error"
        private const val NOTIFICATION_CHANNEL_ID: String = "PinkyPlayer"
    }
}