package com.example.waveplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.waveplayer.fragments.fragment_pane_song.FragmentPaneSong;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServiceMain extends Service {

    public static final String TAG = "ServiceMain";
    private static final String FILE_ERROR_LOG = "error";
    private final static String NOTIFICATION_CHANNEL_ID = "PinkyPlayer";

    public static final Object lock = new Object();

    private MediaController mediaController;

    public static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // For updating the SeekBar
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private RunnableSeekBarUpdater runnableSeekBarUpdater;

    public void updateSeekBarUpdater(SeekBar seekBar, TextView textViewCurrent) {
        Log.v(TAG, "updateSeekBarUpdater start");
        shutDownSeekBarUpdater();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        MediaPlayerWUri mediaPlayerWUri =
                mediaController.getMediaPlayerWUri(mediaController.getCurrentSong().id);
        if (mediaPlayerWUri != null) {
            runnableSeekBarUpdater = new RunnableSeekBarUpdater(
                    mediaPlayerWUri,
                    seekBar, textViewCurrent,
                    getResources().getConfiguration().locale);
            scheduledExecutorService.scheduleAtFixedRate(
                    runnableSeekBarUpdater, 0L, 1L, TimeUnit.SECONDS);
        }
        Log.v(TAG, "updateSeekBarUpdater end");
    }

    public void shutDownSeekBarUpdater() {
        Log.v(TAG, "shutDownSeekBarUpdater start");
        if (runnableSeekBarUpdater != null) {
            runnableSeekBarUpdater.shutDown();
            runnableSeekBarUpdater = null;
        }
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
        Log.v(TAG, "shutDownSeekBarUpdater end");
    }

    private final BroadcastReceiverNotificationButtonsForServiceMain
            broadcastReceiverNotificationButtonsForServiceMainButtons =
            new BroadcastReceiverNotificationButtonsForServiceMain(this);

    private RemoteViews remoteViewsNotificationLayout;
    private RemoteViews remoteViewsNotificationLayoutWithoutArt;
    private RemoteViews remoteViewsNotificationLayoutWithArt;
    private NotificationCompat.Builder notificationCompatBuilder;
    private Notification notification;
    private boolean hasArt = false;

    // region ActivityMainUI

    private int songPaneArtHeight = 1;

    public int getSongPaneArtHeight() {
        Log.v(TAG, "getSongPaneArtHeight start and end");
        return songPaneArtHeight;
    }

    public void setSongPaneArtHeight(int songArtHeight) {
        Log.v(TAG, "setSongPaneArtHeight start");
        this.songPaneArtHeight = songArtHeight;
        Log.v(TAG, "setSongPaneArtHeight end");
    }

    private int songPaneArtWidth = 1;

    public int getSongPaneArtWidth() {
        Log.v(TAG, "getSongPaneArtWidth start and end");
        return songPaneArtWidth;
    }

    public void setSongPaneArtWidth(int songArtWidth) {
        Log.v(TAG, "setSongPaneArtWidth start");
        this.songPaneArtWidth = songArtWidth;
        Log.v(TAG, "setSongPaneArtWidth end");
    }

    // endregion ActivityMainUI

    private boolean serviceStarted = false;

    private final IBinder serviceMainBinder = new ServiceMainBinder();

    // region onCreate

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate started");
        super.onCreate();
        Log.v(TAG, "onCreate is loading");
        setUpBroadCastReceivers();
        // setUpExceptionSaver();
        // logLastThrownException();
        Log.v(TAG, "onCreate done loading");
    }

    private void setUpBroadCastReceivers() {
        Log.v(TAG, "Setting up Broadcast receivers for notification buttons");
        setUpBroadcastReceiverNext();
        setUpBroadcastReceiverPrevious();
        setUpBroadcastReceiverPlayPause();
        Log.v(TAG, "Done setting up Broadcast receivers for notification buttons");
    }

    private void setUpBroadcastReceiverNext() {
        IntentFilter filterNext = new IntentFilter();
        filterNext.addAction(getResources().getString(R.string.broadcast_receiver_action_next));
        filterNext.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationButtonsForServiceMainButtons, filterNext);
    }

    private void setUpBroadcastReceiverPrevious() {
        IntentFilter filterPrevious = new IntentFilter();
        filterPrevious.addAction(getResources().getString(R.string.broadcast_receiver_action_previous));
        filterPrevious.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationButtonsForServiceMainButtons, filterPrevious);
    }

    private void setUpBroadcastReceiverPlayPause() {
        IntentFilter filterPlayPause = new IntentFilter();
        filterPlayPause.addAction(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        filterPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationButtonsForServiceMainButtons, filterPlayPause);
    }

    private void setUpExceptionSaver() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread paramThread, @NonNull Throwable paramThrowable) {
                File file = new File(getBaseContext().getFilesDir(), FILE_ERROR_LOG);
                try (PrintWriter pw = new PrintWriter(file)) {
                    paramThrowable.printStackTrace(pw);
                    pw.flush();
                    System.exit(1);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void logLastThrownException() {
        File file = new File(getBaseContext().getFilesDir(), FILE_ERROR_LOG);
        if (file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                StringBuilder stringBuilder = new StringBuilder();
                String sCurrentLine;
                while ((sCurrentLine = bufferedReader.readLine()) != null) {
                    stringBuilder.append(sCurrentLine);
                }
                Log.e(TAG, stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // endregion onCreate

    // region onStartCommand

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand started");
        if (!serviceStarted) {
            Log.v(TAG, "onStartCommand setting up service");
            // TODO remove before release?
            Toast.makeText(getApplicationContext(), "PinkyPlayer starting", Toast.LENGTH_SHORT).show();
            updateNotification();
            notification = notificationCompatBuilder.build();
            startForeground(NOTIFICATION_CHANNEL_ID.hashCode(), notification);
            Log.v(TAG, "onStartCommand done setting up service");
        } else {
            Log.v(TAG, "onStartCommand already set up service");
        }
        serviceStarted = true;
        Log.v(TAG, "onStartCommand ended");
        return START_STICKY;
    }

    public void permissionGranted(){
        mediaController = MediaController.getInstance(this);
    }

    private void setUpNotificationBuilder() {
        Log.v(TAG, "Setting up notification builder");
        notificationCompatBuilder = new NotificationCompat.Builder(
                getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.music_note_black_48dp)
                .setContentTitle(NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        remoteViewsNotificationLayoutWithoutArt =
                new RemoteViews(getPackageName(), R.layout.pane_notification_without_art);
        remoteViewsNotificationLayoutWithArt =
                new RemoteViews(getPackageName(), R.layout.pane_notification_with_art);
        remoteViewsNotificationLayout =
                new RemoteViews(getPackageName(), R.layout.pane_notification);
        notificationCompatBuilder.setCustomContentView(remoteViewsNotificationLayout);

        // TODO try to open songpane
        Intent notificationIntent = new Intent(getApplicationContext(), ActivityMain.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, notificationIntent, 0);
        notificationCompatBuilder.setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, importance);
            String description = "Intelligent music player";
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        Log.v(TAG, "Done setting up notification builder");
    }

    public void updateNotification() {
        String string = "Updating the notification";
        Log.v(TAG, string);
        setUpNotificationBuilder();
        setUpBroadCastsForNotificationButtons();
        notification = notificationCompatBuilder.build();
        updateNotificationSongName();
        updateNotificationPlayButton();
        updateSongArt();
        remoteViewsNotificationLayout.removeAllViews(R.id.pane_notification_linear_layout);
        if (hasArt) {
            remoteViewsNotificationLayout.addView(
                    R.id.pane_notification_linear_layout, remoteViewsNotificationLayoutWithArt);
        } else {
            remoteViewsNotificationLayout.addView(
                    R.id.pane_notification_linear_layout, remoteViewsNotificationLayoutWithoutArt);
        }
        notification.contentView = remoteViewsNotificationLayout;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_CHANNEL_ID.hashCode(), notification);
        Log.v(TAG, "Done updating the notification");
    }

    private void updateSongArt() {
        Log.v(TAG, "updateSongArt for notification start");
        // TODO update song art background
        if (mediaController != null && mediaController.getCurrentSong() != null) {
            Bitmap bitmap = ActivityMain.getThumbnail(
                    mediaController.getCurrentSong(), 92, 92, getApplicationContext());
            if (bitmap != null) {
                FragmentPaneSong.getResizedBitmap(bitmap, songPaneArtWidth, songPaneArtHeight);
                remoteViewsNotificationLayoutWithArt.setImageViewBitmap(
                        R.id.imageViewNotificationSongPaneSongArtWArt, bitmap);
                hasArt = true;
            } else {
                remoteViewsNotificationLayoutWithoutArt.setImageViewResource(
                        R.id.imageViewNotificationSongPaneSongArt, R.drawable.music_note_black_48dp);
                hasArt = false;
            }
        } else {
            remoteViewsNotificationLayoutWithoutArt.setImageViewResource(
                    R.id.imageViewNotificationSongPaneSongArt, R.drawable.music_note_black_48dp);
            hasArt = false;
        }
        Log.v(TAG, "updateSongArt for notification end");
    }

    private void updateNotificationSongName() {
        Log.v(TAG, "updateNotificationSongName start");
        if (mediaController != null && mediaController.getCurrentSong() != null) {
            remoteViewsNotificationLayoutWithoutArt.setTextViewText(
                    R.id.textViewNotificationSongPaneSongName, mediaController.getCurrentSong().title);
            remoteViewsNotificationLayoutWithArt.setTextViewText(
                    R.id.textViewNotificationSongPaneSongNameWArt, mediaController.getCurrentSong().title);
        } else {
            remoteViewsNotificationLayoutWithoutArt.setTextViewText(
                    R.id.textViewNotificationSongPaneSongName, NOTIFICATION_CHANNEL_ID);
            remoteViewsNotificationLayoutWithArt.setTextViewText(
                    R.id.textViewNotificationSongPaneSongNameWArt, NOTIFICATION_CHANNEL_ID);
        }
        Log.v(TAG, "updateNotificationSongName end");
    }

    void updateNotificationPlayButton() {
        Log.v(TAG, "updateNotificationPlayButton start");
        if (mediaController != null && mediaController.isPlaying()) {
            remoteViewsNotificationLayoutWithoutArt.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPause, R.drawable.pause_black_24dp);
            remoteViewsNotificationLayoutWithArt.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPauseWArt, R.drawable.pause_black_24dp);
        } else {
            remoteViewsNotificationLayoutWithoutArt.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPause, R.drawable.play_arrow_black_24dp);
            remoteViewsNotificationLayoutWithArt.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPauseWArt, R.drawable.play_arrow_black_24dp);
        }
        Log.v(TAG, "updateNotificationPlayButton end");
    }

    private void setUpBroadCastsForNotificationButtons() {
        Log.v(TAG, "Setting up broadcasts");
        setUpBroadcastNext();
        setUpBroadcastPlayPause();
        setUpBroadcastPrevious();
        Log.v(TAG, "Done setting up broadcasts");
    }

    private void setUpBroadcastNext() {
        Intent intentNext = new Intent(getResources().getString(R.string.broadcast_receiver_action_next));
        intentNext.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentNext = PendingIntent.getBroadcast(
                getApplicationContext(), 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewsNotificationLayoutWithoutArt.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPaneNext, pendingIntentNext);
        remoteViewsNotificationLayoutWithArt.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPaneNextWArt, pendingIntentNext);
    }

    private void setUpBroadcastPlayPause() {
        Intent intentPlayPause = new Intent(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(
                getApplicationContext(), 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewsNotificationLayoutWithoutArt.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePlayPause, pendingIntentPlayPause);
        remoteViewsNotificationLayoutWithArt.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePlayPauseWArt, pendingIntentPlayPause);
    }

    private void setUpBroadcastPrevious() {
        Intent intentPrev = new Intent(getResources().getString(R.string.broadcast_receiver_action_previous));
        intentPrev.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(
                getApplicationContext(), 0, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewsNotificationLayoutWithoutArt.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePrev, pendingIntentPrev);
        remoteViewsNotificationLayoutWithArt.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePrevWArt, pendingIntentPrev);
    }

    // endregion onStartCommand

    // region onBind

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceMainBinder;
    }

    public class ServiceMainBinder extends Binder {
        ServiceMain getService() {
            return ServiceMain.this;
        }
    }

    // endregion onBind

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy started");
        // TODO remove on release?
        mediaController.releaseMediaPlayers();
        Toast.makeText(this, "PinkyPlayer done", Toast.LENGTH_SHORT).show();
        Log.v(TAG, "onDestroy ended");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.v(TAG, "onTaskRemoved started");
        super.onTaskRemoved(rootIntent);
        taskRemoved();
        Log.v(TAG, "onTaskRemoved ended");
    }

    public void taskRemoved() {
        Log.v(TAG, "destroy started");
        if (mediaController.isPlaying()) {
            mediaController.pauseOrPlay(getApplicationContext());
        }
        mediaController.releaseMediaPlayers();
        shutDownSeekBarUpdater();
        unregisterReceiver(broadcastReceiverNotificationButtonsForServiceMainButtons);
        stopSelf();
        Log.v(TAG, "destroy ended");
    }

    // region mediaControls

    public void playNext() {
        mediaController.playNext(getApplicationContext());
    }

    public void pauseOrPlay() {
        mediaController.pauseOrPlay(getApplicationContext());
    }

    public void playPrevious() {
        mediaController.playPrevious(getApplicationContext());
    }

    // endregion mediaControls

}