package com.example.waveplayer.media_controller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.Song;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceMain extends Service {

    public static final ExecutorService executorServiceFIFO = Executors.newSingleThreadExecutor();
    public static final ExecutorService executorServicePool = Executors.newCachedThreadPool();

    private static final String TAG = "ServiceMain";
    private static final String TAG_ERROR = "ServiceMainErrors";
    private static final String FILE_ERROR_LOG = "error";
    private static final String NOTIFICATION_CHANNEL_ID = "PinkyPlayer";
    private static final Object LOCK = new Object();

    private boolean serviceStarted = false;
    private final IBinder serviceMainBinder = new ServiceMainBinder();

    private boolean loaded = false;
    private MediaController mediaController;
    private BroadcastReceiver broadcastReceiver;

    private boolean notificationHasArt = false;
    private RemoteViews remoteViewsNotificationLayout;
    private RemoteViews remoteViewsNotificationLayoutWithoutArt;
    private RemoteViews remoteViewsNotificationLayoutWithArt;
    private NotificationCompat.Builder notificationCompatBuilder;
    private Notification notification;

    // region onCreate

    @Override
    public void onCreate() {
        // Log.v(TAG, "onCreate started");
        super.onCreate();
        // Log.v(TAG, "onCreate is loading");
        setUpExceptionSaver();
        logLastThrownException();
        setUpBroadCastReceivers();
        // Log.v(TAG, "onCreate done loading");
    }

    private void setUpBroadCastReceivers() {
        // Log.v(TAG, "Setting up Broadcast receivers for notification buttons");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(ServiceMain.TAG, "BroadcastReceiverNotificationForServiceMainMediaControls start");
                synchronized (ServiceMain.LOCK) {
                    String action = intent.getAction();
                    if (action != null) {
                        if (action.equals(getResources().getString(
                                R.string.broadcast_receiver_action_next))) {
                            playNext();
                            SaveFile.saveFile(getApplicationContext());
                        } else if (action.equals(getResources().getString(
                                R.string.broadcast_receiver_action_play_pause))) {
                            pauseOrPlay();
                        } else if (action.equals(getResources().getString(
                                R.string.broadcast_receiver_action_previous))) {
                            playPrevious();
                        } else if (action.equals(getResources().getString(
                                R.string.broadcast_receiver_action_new_song))) {
                            updateNotification();
                        }
                    }
                }
                Log.v(ServiceMain.TAG, "BroadcastReceiverNotificationForServiceMainMediaControls end");
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction(getResources().getString(R.string.broadcast_receiver_action_next));
        intentFilter.addAction(getResources().getString(R.string.broadcast_receiver_action_previous));
        intentFilter.addAction(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        intentFilter.addAction(getResources().getString(R.string.broadcast_receiver_action_new_song));
        registerReceiver(broadcastReceiver, intentFilter);
        // Log.v(TAG, "Done setting up Broadcast receivers for notification buttons");
    }

    private void setUpExceptionSaver() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread paramThread, @NonNull Throwable paramThrowable) {
                File file = new File(getBaseContext().getFilesDir(), FILE_ERROR_LOG);
                file.delete();
                try (PrintWriter pw = new PrintWriter(file)) {
                    paramThrowable.printStackTrace(pw);
                    pw.flush();
                    paramThrowable.printStackTrace();
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
                Log.e(TAG_ERROR, stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // endregion onCreate

    // region onStartCommand

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.v(TAG, "onStartCommand started");
        if (!serviceStarted) {
            // Log.v(TAG, "onStartCommand setting up service");
            // TODO remove before release?
            Toast.makeText(getApplicationContext(), "PinkyPlayer starting", Toast.LENGTH_SHORT).show();
            updateNotification();
            notification = notificationCompatBuilder.build();
            startForeground(NOTIFICATION_CHANNEL_ID.hashCode(), notification);
            // Log.v(TAG, "onStartCommand done setting up service");
        } else {
            // Log.v(TAG, "onStartCommand already set up service");
        }
        serviceStarted = true;
        // Log.v(TAG, "onStartCommand ended");
        return START_STICKY;
    }

    public void updateNotification() {
        String string = "Updating the notification";
        // Log.v(TAG, string);
        // TODO try to reuse RemoteViews
        setUpNotificationBuilder();
        setUpBroadCastsForNotificationButtons();
        notification = notificationCompatBuilder.build();
        updateSongArt();
        updateNotificationSongName();
        updateNotificationPlayButton();
        remoteViewsNotificationLayout.removeAllViews(R.id.pane_notification_linear_layout);
        if (notificationHasArt) {
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
        // Log.v(TAG, "Done updating the notification");
    }

    private void setUpNotificationBuilder() {
        // Log.v(TAG, "Setting up notification builder");
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
        // Log.v(TAG, "Done setting up notification builder");
    }

    private void setUpBroadCastsForNotificationButtons() {
        // Log.v(TAG, "Setting up broadcasts");
        setUpBroadcastNext();
        setUpBroadcastPlayPause();
        setUpBroadcastPrevious();
        // Log.v(TAG, "Done setting up broadcasts");
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

    private void updateSongArt() {
        // Log.v(TAG, "updateSongArt for notification start");
        // TODO update song art background
        if (mediaController != null && mediaController.getCurrentAudioUri() != null) {
            Bitmap bitmap = BitmapLoader.getThumbnail(
                    mediaController.getCurrentUri(), 92, 92, getApplicationContext());
            if (bitmap != null) {
                // TODO why? Try to get height of imageView and make the width match
                // BitmapLoader.getResizedBitmap(bitmap, songPaneArtWidth, songPaneArtHeight);
                remoteViewsNotificationLayoutWithArt.setImageViewBitmap(
                        R.id.imageViewNotificationSongPaneSongArtWArt, bitmap);
                notificationHasArt = true;
            } else {
                remoteViewsNotificationLayoutWithoutArt.setImageViewResource(
                        R.id.imageViewNotificationSongPaneSongArt, R.drawable.music_note_black_48dp);
                notificationHasArt = false;
            }
        } else {
            remoteViewsNotificationLayoutWithoutArt.setImageViewResource(
                    R.id.imageViewNotificationSongPaneSongArt, R.drawable.music_note_black_48dp);
            notificationHasArt = false;
        }
        // Log.v(TAG, "updateSongArt for notification end");
    }

    private void updateNotificationSongName() {
        // Log.v(TAG, "updateNotificationSongName start");
        if (mediaController != null && mediaController.getCurrentAudioUri() != null) {
            if(notificationHasArt){
                remoteViewsNotificationLayoutWithArt.setTextViewText(
                        R.id.textViewNotificationSongPaneSongNameWArt, mediaController.getCurrentAudioUri().title);
            } else {
                remoteViewsNotificationLayoutWithoutArt.setTextViewText(
                        R.id.textViewNotificationSongPaneSongName, mediaController.getCurrentAudioUri().title);
            }
        } else {
            if(notificationHasArt) {
                remoteViewsNotificationLayoutWithArt.setTextViewText(
                        R.id.textViewNotificationSongPaneSongNameWArt, NOTIFICATION_CHANNEL_ID);

            } else {
                remoteViewsNotificationLayoutWithoutArt.setTextViewText(
                        R.id.textViewNotificationSongPaneSongName, NOTIFICATION_CHANNEL_ID);
            }
        }
        // Log.v(TAG, "updateNotificationSongName end");
    }

    void updateNotificationPlayButton() {
        // Log.v(TAG, "updateNotificationPlayButton start");
        if (mediaController != null && mediaController.isPlaying()) {
            if(notificationHasArt) {
                remoteViewsNotificationLayoutWithArt.setImageViewResource(
                        R.id.imageButtonNotificationSongPanePlayPauseWArt, R.drawable.pause_black_24dp);
            } else {
                remoteViewsNotificationLayoutWithoutArt.setImageViewResource(
                        R.id.imageButtonNotificationSongPanePlayPause, R.drawable.pause_black_24dp);
            }
        } else {
                if(notificationHasArt) {
                    remoteViewsNotificationLayoutWithArt.setImageViewResource(
                            R.id.imageButtonNotificationSongPanePlayPauseWArt, R.drawable.play_arrow_black_24dp);
                } else {
                    remoteViewsNotificationLayoutWithoutArt.setImageViewResource(
                            R.id.imageButtonNotificationSongPanePlayPause, R.drawable.play_arrow_black_24dp);
                }
        }
        // Log.v(TAG, "updateNotificationPlayButton end");
    }

    // endregion onStartCommand

    // region onBind

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceMainBinder;
    }

    public class ServiceMainBinder extends Binder {
        public ServiceMain getService() {
            return ServiceMain.this;
        }
    }

    // endregion onBind

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Log.v(TAG, "onTaskRemoved started");
        super.onTaskRemoved(rootIntent);
        taskRemoved();
        // Log.v(TAG, "onTaskRemoved ended");
    }

    public void taskRemoved() {
        // Log.v(TAG, "destroy started");
        if (mediaController.isPlaying()) {
            mediaController.pauseOrPlay(getApplicationContext());
        }
        mediaController.releaseMediaPlayers();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        stopSelf();
        // Log.v(TAG, "destroy ended");
    }

    @Override
    public void onDestroy() {
        // Log.v(TAG, "onDestroy started");
        if (mediaController.isPlaying()) {
            mediaController.pauseOrPlay(getApplicationContext());
        }
        mediaController.releaseMediaPlayers();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        stopSelf();
        // TODO remove on release?
        Toast.makeText(this, "PinkyPlayer done", Toast.LENGTH_SHORT).show();
        // Log.v(TAG, "onDestroy ended");
    }

    // region mediaControls

    public void permissionGranted() {
        ServiceMain.executorServicePool.execute(
                () -> mediaController = MediaController.getInstance(getApplicationContext()));
    }

    public MediaPlayerWUri getCurrentMediaPlayerWUri() {
        return mediaController.getCurrentMediaPlayerWUri();
    }

    public void goToFrontOfQueue() {
        mediaController.goToFrontOfQueue();
    }

    public void playNext() {
        // Log.v(TAG, "playNext start");
        if(loaded) {
            if (mediaController.getCurrentAudioUri() != null) {
                RandomPlaylist randomPlaylist = getCurrentPlaylist();
                if (randomPlaylist != null) {
                    randomPlaylist.bad(
                            getApplicationContext(),
                            MediaController.getInstance(getApplicationContext())
                                    .getSong(mediaController.getCurrentAudioUri().id),
                            mediaController.getPercentChangeDown());
                }
            }
            mediaController.playNext(getApplicationContext());
            sendBroadcastNewSong();
        }
        // Log.v(TAG, "playNext end");
    }

    public boolean loaded(){
        return loaded;
    }
    public void loaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void pauseOrPlay() {
        if(loaded) {
            if (mediaController.getCurrentAudioUri() != null) {
                mediaController.pauseOrPlay(getApplicationContext());
            }
            updateNotification();
        }
    }

    public void playPrevious() {
        if(loaded) {
            mediaController.playPrevious(getApplicationContext());
            sendBroadcastNewSong();
        }
    }

    public void lowerProbabilities() {
        mediaController.lowerProbabilities(getApplicationContext());
    }

    public boolean songQueueIsEmpty() {
        return mediaController.songQueueIsEmpty();
    }

    public void clearProbabilities() {
        mediaController.clearProbabilities(getApplicationContext());
    }

    // endregion mediaControls

    public List<Song> getAllSongs() {
        return mediaController.getAllSongs();
    }

    public int getCurrentTime() {
        // Log.v(TAG, "getCurrentTime start");
        // Log.v(TAG, "getCurrentTime end");
        return mediaController.getCurrentTime();
    }

    public void addToQueue(Long songID) {
        // Log.v(TAG, "addToQueue start");
        mediaController.addToQueue(songID);
        // Log.v(TAG, "addToQueue end");
    }

    public void seekTo(int progress) {
        // Log.v(TAG, "seekTo start");
        mediaController.seekTo(getApplicationContext(), progress);
        // Log.v(TAG, "seekTo end");
    }

    // endregion playbackControls

    public boolean songInProgress() {
        // Log.v(TAG, "songInProgress start");
        // Log.v(TAG, "songInProgress end");
        return (mediaController != null) && mediaController.isSongInProgress();
    }

    public boolean isPlaying() {
        // Log.v(TAG, "isPlaying start");
        // Log.v(TAG, "isPlaying end");
        return mediaController.isPlaying();
    }

    public void setCurrentPlaylistToMaster() {
        // Log.v(TAG, "setCurrentPlaylistToMaster start");
        mediaController.setCurrentPlaylistToMaster();
        // Log.v(TAG, "setCurrentPlaylistToMaster end");
    }

    public void setCurrentPlaylist(RandomPlaylist userPickedPlaylist) {
        // Log.v(TAG, "setCurrentPlaylist start");
        mediaController.setCurrentPlaylist(userPickedPlaylist);
        // Log.v(TAG, "setCurrentPlaylist end");
    }

    public AudioUri getCurrentAudioUri() {
        // Log.v(TAG, "getCurrentSong start");
        // Log.v(TAG, "getCurrentSong end");
        return mediaController.getCurrentAudioUri();
    }

    public RandomPlaylist getCurrentPlaylist() {
        // Log.v(TAG, "getCurrentPlaylist start");
        // Log.v(TAG, "getCurrentPlaylist end");
        return mediaController.getCurrentPlaylist();
    }

    public boolean shuffling() {
        // Log.v(TAG, "shuffling start");
        // Log.v(TAG, "shuffling end");
        return mediaController.isShuffling();
    }

    public void shuffling(boolean shuffling) {
        // Log.v(TAG, "set shuffling start");
        mediaController.setShuffling(shuffling);
        // Log.v(TAG, "set shuffling end");
    }

    public boolean loopingOne() {
        // Log.v(TAG, "loopingOne start");
        // Log.v(TAG, "loopingOne end");
        return mediaController.isLoopingOne();
    }

    public void loopingOne(boolean loopingOne) {
        // Log.v(TAG, "set loopingOne start");
        mediaController.setLoopingOne(loopingOne);
        // Log.v(TAG, "set loopingOne end");
    }

    public boolean looping() {
        // Log.v(TAG, "looping start");
        // Log.v(TAG, "looping end");
        return mediaController.isLooping();
    }

    public void looping(boolean looping) {
        // Log.v(TAG, "set looping start");
        mediaController.setLooping(looping);
        // Log.v(TAG, "set looping end");
    }

    public void clearSongQueue() {
        mediaController.clearSongQueue();
    }

    // region fragmentLoading

    public Song getCurrentSong() {
        if (getCurrentAudioUri() != null) {
            return MediaController.getInstance(getApplicationContext())
                    .getSong(getCurrentAudioUri().id);
        }
        return null;
    }

    public void sendBroadcastNewSong() {
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(getApplicationContext().getResources().getString(
                        R.string.broadcast_receiver_action_new_song));
        getApplicationContext().sendBroadcast(intent);
    }

    public Uri getCurrentUri() {
        return mediaController.getCurrentUri();
    }

    public double getMaxPercent() {
        return mediaController.getMaxPercent();
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        mediaController.addPlaylist(randomPlaylist);
    }

    public List<RandomPlaylist> getPlaylists() {
        return mediaController.getPlaylists();
    }

    public Song getSong(long songID) {
        return mediaController.getSong(songID);
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        mediaController.removePlaylist(randomPlaylist);
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        mediaController.addPlaylist(position, randomPlaylist);
    }

    public double getPercentChangeUp() {
        return mediaController.getPercentChangeUp();
    }

    public double getPercentChangeDown() {
        return mediaController.getPercentChangeDown();
    }

    public void setMaxPercent(double maxPercent) {
        mediaController.setMaxPercent(maxPercent);
    }

    public void setPercentChangeUp(double percentChangeUp) {
        mediaController.setPercentChangeUp(percentChangeUp);
    }

    public void setPercentChangeDown(double percentChangeDown) {
        mediaController.setPercentChangeDown(percentChangeDown);
    }

    public RandomPlaylist getMasterPlaylist() {
        return mediaController.getMasterPlaylist();
    }

    public RandomPlaylist getPlaylist(String playlistName) {
        return mediaController.getPlaylist(playlistName);
    }

    public boolean isSongInProgress() {
        return mediaController.isSongInProgress();
    }

}