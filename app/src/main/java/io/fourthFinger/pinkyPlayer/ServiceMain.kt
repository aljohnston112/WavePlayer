package io.fourthFinger.pinkyPlayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.getActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import io.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import io.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.io.PrintWriter
import java.lang.Thread.UncaughtExceptionHandler

class ServiceMain : LifecycleService() {

    private var notificationCompatBuilder: NotificationCompat.Builder? = null
    private var notification: Notification? = null

    private var serviceStarted = false

    private lateinit var mediaSession: MediaSession
    private lateinit var remoteViewCreator: RemoteViewCreator

    private var broadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            synchronized(MEDIA_LOCK) {
                val action: String? = intent.action
                if (action != null) {
                    val mediaSession = (application as ApplicationMain).mediaSession!!
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
                            val notificationManager = getSystemService(
                                NOTIFICATION_SERVICE
                            ) as NotificationManager
                            notificationManager.notify(
                                NOTIFICATION_CHANNEL_ID.hashCode(),
                                notification
                            )
                        }
                    }
                }
            }
        }
    }

    private var defaultExceptionHandler: UncaughtExceptionHandler? =
        UncaughtExceptionHandler { _, throwable ->
        val file = File(
            baseContext.filesDir,
            FILE_ERROR_LOG
        )
        file.delete()
        try {
            PrintWriter(file).use { printWriter ->
                throwable.printStackTrace(printWriter)
                printWriter.flush()
                throwable.printStackTrace()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        throw throwable
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = (application as ApplicationMain).mediaSession!!
        remoteViewCreator = RemoteViewCreator(
            packageName,
            applicationContext,
            mediaSession
        )
        setUpExceptionSaver()
        logLastThrownException()
        setUpBroadCastReceivers()
    }

    private fun setUpExceptionSaver() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
    }

    private fun logLastThrownException() {
        val file = File(
            baseContext.filesDir,
            FILE_ERROR_LOG
        )
        if (file.exists()) {
            try {
                BufferedReader(FileReader(file)).use { bufferedReader ->
                    val stringBuilder: StringBuilder = StringBuilder()
                    var currentLine: String?
                    while (
                        (bufferedReader.readLine().also {
                            currentLine = it
                        }) != null
                    ) {
                        stringBuilder.append(currentLine)
                    }
                    Log.e(
                        TAG_ERROR,
                        stringBuilder.toString()
                    )
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
            registerReceiver(
                broadcastReceiver,
                intentFilter,
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                broadcastReceiver,
                intentFilter
            )
        }
    }

    // endregion onCreate
    // region onStartCommand
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(
            intent,
            flags,
            startId
        )
        if (!serviceStarted) {
            setUpNotificationBuilder()
            updateNotification(
                remoteViewCreator.createRemoteView(applicationContext)
            )
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
            mediaSession.currentAudioUri.observe(this) {
                updateNotification(
                    remoteViewCreator.createRemoteView(
                        applicationContext,
                        it
                    )
                )
            }
            mediaSession.isPlaying.observe(this) {
                updateNotification(
                    remoteViewCreator.createRemoteView(
                        applicationContext,
                        it
                    )
                )
            }
        }
        serviceStarted = true
        return START_NOT_STICKY
    }

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

        val notificationIntent = Intent(
            applicationContext,
            ActivityMain::class.java
        )
        val pendingIntent = getActivity(
            applicationContext,
            0,
            notificationIntent,
            FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
        )
        notificationCompatBuilder?.setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW
            )
            val description = getString(R.string.description)
            channel.description = description
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(remoteViews: RemoteViews) {
        notificationCompatBuilder?.setCustomContentView(remoteViews)
        notification = notificationCompatBuilder?.build()
        val notificationManager = getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.notify(
            NOTIFICATION_CHANNEL_ID.hashCode(),
            notification
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ApplicationMain).cleanUp()
        remoteViewCreator.cleanUp()
        unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
        notification = null
        notificationCompatBuilder = null
        Thread.setDefaultUncaughtExceptionHandler(null)
        defaultExceptionHandler = null
    }

    companion object {
        private val MEDIA_LOCK = Any()
        private const val TAG_ERROR: String = "ServiceMainErrors"
        private const val FILE_ERROR_LOG: String = "error"
        private const val NOTIFICATION_CHANNEL_ID: String = "PinkyPlayer"
    }

}