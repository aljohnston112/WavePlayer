package com.example.pinky_player.media_controller

import android.app.*
import android.content.*
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.pinky_player.R
import com.example.pinky_player.activity_main.ActivityMain
import com.example.pinky_player.random_playlist.SongQueue
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.exitProcess

class ServiceMain : LifecycleService() {

    private var notificationHasArt = false
    private var remoteViewsNotificationLayout: RemoteViews? = null
    private var remoteViewsNotificationLayoutWithoutArt: RemoteViews? = null
    private var remoteViewsNotificationLayoutWithArt: RemoteViews? = null
    private var notificationCompatBuilder: NotificationCompat.Builder? = null
    private var notification: Notification? = null
    private var serviceStarted = false
    private val serviceMainBinder: IBinder = ServiceMainBinder()
    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            synchronized(LOCK) {
                val action: String? = intent.action
                if (action != null) {
                    if ((action == resources.getString(
                                    R.string.broadcast_receiver_action_next))) {
                        mediaController.playNext()
                        SaveFile.saveFile(applicationContext)
                    } else if ((action == resources.getString(
                                    R.string.broadcast_receiver_action_play_pause))) {
                        mediaController.pauseOrPlay()
                    } else if ((action == resources.getString(
                                    R.string.broadcast_receiver_action_previous))) {
                        mediaController.playPrevious()
                    } else if ((action == resources.getString(
                                    R.string.broadcast_receiver_action_new_song))) {
                        updateNotification()
                    }
                }
            }
        }
    }
    private var loaded = false
    private lateinit var mediaController: MediaController
    private lateinit var mediaData: MediaData
    private val songQueue = SongQueue.getInstance()

    // region lifecycle
    // region onCreate
    override fun onCreate() {
        super.onCreate()
        mediaController = MediaController.getInstance(applicationContext)
        mediaData = MediaData.getInstance(applicationContext)
        setUpExceptionSaver()
        logLastThrownException()
        setUpBroadCastReceivers()
    }

    private fun setUpBroadCastReceivers() {
        val intentFilter = IntentFilter()
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(resources.getString(R.string.broadcast_receiver_action_next))
        intentFilter.addAction(resources.getString(R.string.broadcast_receiver_action_previous))
        intentFilter.addAction(resources.getString(R.string.broadcast_receiver_action_play_pause))
        intentFilter.addAction(resources.getString(R.string.broadcast_receiver_action_new_song))
        registerReceiver(broadcastReceiver, intentFilter)
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
                    exitProcess(1)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
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

    // endregion onCreate
    // region onStartCommand
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!serviceStarted) {
            // TODO remove before release?
            Toast.makeText(applicationContext, "PinkyPlayer starting", Toast.LENGTH_SHORT).show()
            updateNotification()
            notification = notificationCompatBuilder?.build()
            startForeground(NOTIFICATION_CHANNEL_ID.hashCode(), notification)
            mediaController.isPlaying.observe(this){
                updateNotification()
            }
        }
        serviceStarted = true
        return START_STICKY
    }

    fun updateNotification() {
        // TODO try to reuse RemoteViews
        setUpNotificationBuilder()
        setUpBroadCastsForNotificationButtons()
        updateSongArt()
        updateNotificationSongName()
        updateNotificationPlayButton()
        remoteViewsNotificationLayout?.removeAllViews(R.id.pane_notification_linear_layout)
        if (notificationHasArt) {
            remoteViewsNotificationLayout?.addView(
                    R.id.pane_notification_linear_layout, remoteViewsNotificationLayoutWithArt)
        } else {
            remoteViewsNotificationLayout?.addView(
                    R.id.pane_notification_linear_layout, remoteViewsNotificationLayoutWithoutArt)
        }
        notificationCompatBuilder?.setCustomContentView(remoteViewsNotificationLayout)
        notification = notificationCompatBuilder?.build()
        val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_CHANNEL_ID.hashCode(), notification)
    }

    private fun setUpNotificationBuilder() {
        notificationCompatBuilder = NotificationCompat.Builder(
                applicationContext, NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.music_note_black_48dp)
                .setContentTitle(NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        remoteViewsNotificationLayoutWithoutArt = RemoteViews(packageName, R.layout.pane_notification_without_art)
        remoteViewsNotificationLayoutWithArt = RemoteViews(packageName, R.layout.pane_notification_with_art)
        remoteViewsNotificationLayout = RemoteViews(packageName, R.layout.pane_notification)
        notificationCompatBuilder?.setCustomContentView(remoteViewsNotificationLayout)

        // TODO try to open songpane
        val notificationIntent = Intent(applicationContext, ActivityMain::class.java)
        val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, notificationIntent, 0)
        notificationCompatBuilder?.setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, importance)
            val description = "Intelligent music player"
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        // Log.v(TAG, "Done setting up notification builder");
    }

    private fun setUpBroadCastsForNotificationButtons() {
        // Log.v(TAG, "Setting up broadcasts");
        setUpBroadcastNext()
        setUpBroadcastPlayPause()
        setUpBroadcastPrevious()
        // Log.v(TAG, "Done setting up broadcasts");
    }

    private fun setUpBroadcastNext() {
        val intentNext = Intent(resources.getString(R.string.broadcast_receiver_action_next))
        intentNext.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentNext = PendingIntent.getBroadcast(
                applicationContext, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteViewsNotificationLayoutWithoutArt?.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPaneNext, pendingIntentNext)
        remoteViewsNotificationLayoutWithArt?.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPaneNextWArt, pendingIntentNext)
    }

    private fun setUpBroadcastPlayPause() {
        val intentPlayPause: Intent = Intent(resources.getString(R.string.broadcast_receiver_action_play_pause))
        intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentPlayPause = PendingIntent.getBroadcast(
                applicationContext, 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteViewsNotificationLayoutWithoutArt?.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePlayPause, pendingIntentPlayPause)
        remoteViewsNotificationLayoutWithArt?.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePlayPauseWArt, pendingIntentPlayPause)
    }

    private fun setUpBroadcastPrevious() {
        val intentPrev = Intent(resources.getString(R.string.broadcast_receiver_action_previous))
        intentPrev.addCategory(Intent.CATEGORY_DEFAULT)
        val pendingIntentPrev = PendingIntent.getBroadcast(
                applicationContext, 0, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT)
        remoteViewsNotificationLayoutWithoutArt?.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePrev, pendingIntentPrev)
        remoteViewsNotificationLayoutWithArt?.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePrevWArt, pendingIntentPrev)
    }

    private fun updateSongArt() {
        // TODO update song art background
        if (mediaController.currentAudioUri.value != null) {
            val bitmap = mediaController.getCurrentUri()?.let {
                BitmapLoader.getThumbnail(
                        it, 92, 92, applicationContext)
            }
            if (bitmap != null) {
                // TODO why? Try to get height of imageView and make the width match
                // BitmapLoader.getResizedBitmap(bitmap, songPaneArtWidth, songPaneArtHeight);
                remoteViewsNotificationLayoutWithArt?.setImageViewBitmap(
                        R.id.imageViewNotificationSongPaneSongArtWArt, bitmap)
                notificationHasArt = true
            } else {
                remoteViewsNotificationLayoutWithoutArt?.setImageViewResource(
                        R.id.imageViewNotificationSongPaneSongArt, R.drawable.music_note_black_48dp)
                notificationHasArt = false
            }
        } else {
            remoteViewsNotificationLayoutWithoutArt?.setImageViewResource(
                    R.id.imageViewNotificationSongPaneSongArt, R.drawable.music_note_black_48dp)
            notificationHasArt = false
        }
    }

    private fun updateNotificationSongName() {
        if (mediaController.currentAudioUri.value != null) {
            if (notificationHasArt) {
                mediaController.currentAudioUri.value?.title?.let{
                    remoteViewsNotificationLayoutWithArt?.setTextViewText(
                            R.id.textViewNotificationSongPaneSongNameWArt, it)
                }
            } else {
                mediaController.currentAudioUri.value?.title?.let{
                    remoteViewsNotificationLayoutWithoutArt?.setTextViewText(
                            R.id.textViewNotificationSongPaneSongName, it)
                }
            }
        } else {
            if (notificationHasArt) {
                remoteViewsNotificationLayoutWithArt?.setTextViewText(
                        R.id.textViewNotificationSongPaneSongNameWArt, NOTIFICATION_CHANNEL_ID)
            } else {
                remoteViewsNotificationLayoutWithoutArt?.setTextViewText(
                        R.id.textViewNotificationSongPaneSongName, NOTIFICATION_CHANNEL_ID)
            }
        }
    }

    private fun updateNotificationPlayButton() {
        if (mediaController.isPlaying.value == true) {
            if (notificationHasArt) {
                remoteViewsNotificationLayoutWithArt?.setImageViewResource(
                        R.id.imageButtonNotificationSongPanePlayPauseWArt, R.drawable.pause_black_24dp)
            } else {
                remoteViewsNotificationLayoutWithoutArt?.setImageViewResource(
                        R.id.imageButtonNotificationSongPanePlayPause, R.drawable.pause_black_24dp)
            }
        } else {
            if (notificationHasArt) {
                remoteViewsNotificationLayoutWithArt?.setImageViewResource(
                        R.id.imageButtonNotificationSongPanePlayPauseWArt, R.drawable.play_arrow_black_24dp)
            } else {
                remoteViewsNotificationLayoutWithoutArt?.setImageViewResource(
                        R.id.imageButtonNotificationSongPanePlayPause, R.drawable.play_arrow_black_24dp)
            }
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
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        taskRemoved()
    }

    private fun taskRemoved() {
        if (mediaController.isPlaying.value == true) {
            mediaController.pauseOrPlay()
        }
        mediaController.releaseMediaPlayers()
        unregisterReceiver(broadcastReceiver)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaController.isPlaying.value == true) {
            mediaController.pauseOrPlay()
        }
        mediaController.releaseMediaPlayers()
        unregisterReceiver(broadcastReceiver)
        stopSelf()
        // TODO remove on release?
        Toast.makeText(this, "PinkyPlayer done", Toast.LENGTH_SHORT).show()
    }

    // endregion lifecycle
    // region mediaControls

    fun loaded(): Boolean {
        return loaded
    }

    fun loaded(loaded: Boolean) {
        this.loaded = loaded
    }

    companion object {
        val executorServiceFIFO: ExecutorService = Executors.newSingleThreadExecutor()
        val executorServicePool: ExecutorService = Executors.newCachedThreadPool()
        private val TAG: String = "ServiceMain"
        private val TAG_ERROR: String = "ServiceMainErrors"
        private val FILE_ERROR_LOG: String = "error"
        private val LOCK: Any = Any()
        private val NOTIFICATION_CHANNEL_ID: String = "PinkyPlayer"
    }
}