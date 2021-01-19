package com.example.waveplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
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
import java.util.LinkedHashMap;
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
    private static final String FILE_SAVE = "playlists";
    private final static String NOTIFICATION_CHANNEL_ID = "PinkyPlayer";
    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";

    private static final Random random = new Random();
    public static final Object lock = new Object();

    // TODO make fragments communicate

    // region settings

    private double maxPercent = 0.1;

    public double getMaxPercent() {
        Log.v(TAG, "getMaxPercent start and end");
        return maxPercent;
    }

    public void setMaxPercent(double maxPercent) {
        Log.v(TAG, "setMaxPercent start");
        this.maxPercent = maxPercent;
        masterPlaylist.setMaxPercent(maxPercent);
        for (RandomPlaylist randomPlaylist : playlists) {
            randomPlaylist.setMaxPercent(maxPercent);
        }
        for (RandomPlaylist randomPlaylist : directoryPlaylists.values()) {
            randomPlaylist.setMaxPercent(maxPercent);
        }
        Log.v(TAG, "setMaxPercent end");
    }

    private double percentChangeUp = 0.1;

    public double getPercentChangeUp() {
        Log.v(TAG, "getPercentChangeUp start and end");
        return percentChangeUp;
    }

    public void setPercentChangeUp(double percentChangeUp) {
        Log.v(TAG, "setPercentChangeUp start");
        this.percentChangeUp = percentChangeUp;
        Log.v(TAG, "setPercentChangeUp end");
    }

    private double percentChangeDown = 0.5;

    public double getPercentChangeDown() {
        Log.v(TAG, "getPercentChangeDown start and end");
        return percentChangeDown;
    }

    public void setPercentChangeDown(double percentChangeDown) {
        Log.v(TAG, "setPercentChangeDown start");
        this.percentChangeDown = percentChangeDown;
        Log.v(TAG, "setPercentChangeDown end");
    }

    // endregion settings

    private final HashMap<Uri, MediaPlayerWUri> uriMediaPlayerWUriHashMap = new HashMap<>();

    // TODO organize usages
    private MediaPlayerWUri getCurrentMediaPlayerWUri() {
        Log.v(TAG, "getCurrentMediaPlayerWUri start");
        if (currentSong != null) {
            Log.v(TAG, "getCurrentMediaPlayerWUri end");
            return uriMediaPlayerWUriHashMap.get(currentSong.getUri());
        }
        Log.v(TAG, "getCurrentMediaPlayerWUri default end");
        return null;
    }

    private MediaPlayerWUri getMediaPlayerWUri(Uri uri) {
        Log.v(TAG, "getMediaPlayerWUri start and end");
        return uriMediaPlayerWUriHashMap.get(uri);
    }

    private final LinkedHashMap<Uri, AudioUri> uriAudioURILinkedHashMap = new LinkedHashMap<>();

    // region playlists

    private RandomPlaylist masterPlaylist;

    public List<AudioUri> getAllSongs() {
        Log.v(TAG, "getAllSongs start and end");
        return masterPlaylist.getAudioUris();
    }

    private final ArrayList<RandomPlaylist> playlists = new ArrayList<>();

    public ArrayList<RandomPlaylist> getPlaylists() {
        Log.v(TAG, "getPlaylists start and end");
        return playlists;
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        Log.v(TAG, "addPlaylist start");
        playlists.add(randomPlaylist);
        Log.v(TAG, "addPlaylist end");
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        Log.v(TAG, "addPlaylist w/ position start");
        playlists.add(position, randomPlaylist);
        Log.v(TAG, "addPlaylist w/ position end");
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        Log.v(TAG, "removePlaylist start");
        directoryPlaylists.remove(randomPlaylist.mediaStoreUriID);
        playlists.remove(randomPlaylist);
        Log.v(TAG, "removePlaylist end");
    }

    public RandomPlaylist getCurrentPlaylist() {
        Log.v(TAG, "getCurrentPlaylist start and end");
        return currentPlaylist;
    }

    private TreeMap<Long, RandomPlaylist> directoryPlaylists =
            new TreeMap<>(new ComparableLongsSerializable());

    static class ComparableLongsSerializable implements Serializable, Comparator<Long> {
        @Override
        public int compare(Long o1, Long o2) {
            Log.v(TAG, "compare start and end");
            return o1.compareTo(o2);
        }
    }

    public boolean containsDirectoryPlaylist(long mediaStoreUriID) {
        Log.v(TAG, "containsDirectoryPlaylist start and end");
        return directoryPlaylists.get(mediaStoreUriID) != null;
    }

    public void addDirectoryPlaylist(long uriID, RandomPlaylist randomPlaylist) {
        Log.v(TAG, "addDirectoryPlaylist start");
        directoryPlaylists.put(uriID, randomPlaylist);
        addPlaylist(randomPlaylist);
        Log.v(TAG, "addDirectoryPlaylist end");
    }

    public RandomPlaylist getDirectoryPlaylist(long mediaStoreUriID) {
        Log.v(TAG, "getDirectoryPlaylist start and end");
        return directoryPlaylists.get(mediaStoreUriID);
    }

    // endregion playlists

    // region userPickedPlaylist

    private RandomPlaylist userPickedPlaylist;

    public RandomPlaylist getUserPickedPlaylist() {
        Log.v(TAG, "getUserPickedPlaylist start and end");
        return userPickedPlaylist;
    }

    public void setUserPickedPlaylistToMasterPlaylist() {
        Log.v(TAG, "setUserPickedPlaylistToMasterPlaylist start");
        userPickedPlaylist = masterPlaylist;
        Log.v(TAG, "setUserPickedPlaylistToMasterPlaylist end");
    }

    public void setUserPickedPlaylist(RandomPlaylist userPickedPlaylist) {
        Log.v(TAG, "setUserPickedPlaylist start");
        this.userPickedPlaylist = userPickedPlaylist;
        Log.v(TAG, "setUserPickedPlaylist end");
    }

    // endregion userPickedPlaylist

    // region userPickedSongs

    private final List<AudioUri> userPickedSongs = new ArrayList<>();

    public List<AudioUri> getUserPickedSongs() {
        Log.v(TAG, "getUserPickedSongs start and end");
        return userPickedSongs;
    }

    public void addUserPickedSong(AudioUri audioURI) {
        Log.v(TAG, "addUserPickedSong start");
        userPickedSongs.add(audioURI);
        Log.v(TAG, "addUserPickedSong end");
    }

    public void removeUserPickedSong(AudioUri audioURI) {
        Log.v(TAG, "removeUserPickedSong start");
        userPickedSongs.remove(audioURI);
        Log.v(TAG, "removeUserPickedSong end");
    }

    public void clearUserPickedSongs() {
        Log.v(TAG, "clearUserPickedSongs start");
        userPickedSongs.clear();
        Log.v(TAG, "clearUserPickedSongs end");
    }

    // endregion userPickedSongs

    // region currentPlaylist

    private RandomPlaylist currentPlaylist;
    private ArrayList<AudioUri> currentPlaylistArray;
    private ListIterator<AudioUri> currentPlaylistIterator;

    public void setCurrentPlaylistToMaster() {
        Log.v(TAG, "setCurrentPlaylistToMaster start");
        setCurrentPlaylist(masterPlaylist);
        Log.v(TAG, "setCurrentPlaylistToMaster end");
    }

    public void setCurrentPlaylist(RandomPlaylist currentPlaylist) {
        Log.v(TAG, "setCurrentPlaylist start");
        this.currentPlaylist = currentPlaylist;
        currentPlaylistArray = currentPlaylist.getAudioUris();
        clearSongQueue();
        Log.v(TAG, "setCurrentPlaylist end");
    }

    public void clearProbabilities() {
        Log.v(TAG, "clearProbabilities start");
        currentPlaylist.clearProbabilities();
        Log.v(TAG, "clearProbabilities end");
    }

    // endregion currentPlaylist

    // region currentSongQueue

    private final LinkedList<Uri> songQueue = new LinkedList<>();
    private ListIterator<Uri> songQueueIterator;

    public boolean songQueueIsEmpty() {
        Log.v(TAG, "songQueueIsEmpty start and end");
        return songQueue.size() == 0;
    }

    private AudioUri currentSong;

    public AudioUri getCurrentSong() {
        Log.v(TAG, "getCurrentSong start and end");
        return currentSong;
    }

    public int getCurrentTime() {
        Log.v(TAG, "getCurrentTime start");
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            Log.v(TAG, "getCurrentTime end");
            return mediaPlayerWURI.getCurrentPosition();
        } else {
            Log.v(TAG, "getCurrentTime default end");
            return -1;
        }
    }

    private boolean isPlaying = false;

    public boolean isPlaying() {
        Log.v(TAG, "isPlaying start and end");
        return isPlaying;
    }

    private boolean songInProgress = false;

    public boolean songInProgress() {
        // Log.v(TAG, "songInProgress start and end");
        return songInProgress;
    }

    // endregion currentSongQueue

    // region playbackLogic

    private boolean shuffling = true;

    public boolean shuffling() {
        Log.v(TAG, "shuffling start and end");
        return shuffling;
    }

    public void shuffling(boolean shuffling) {
        Log.v(TAG, "set shuffling start");
        this.shuffling = shuffling;
        Log.v(TAG, "set shuffling end");
    }

    private boolean looping = false;

    public boolean looping() {
        Log.v(TAG, "looping start and end");
        return looping;
    }

    public void looping(boolean looping) {
        Log.v(TAG, "set looping start");
        this.looping = looping;
        Log.v(TAG, "set looping end");
    }

    private boolean loopingOne = false;

    public boolean loopingOne() {
        Log.v(TAG, "loopingOne start and end");
        return loopingOne;
    }

    public void loopingOne(boolean loopingOne) {
        Log.v(TAG, "set loopingOne start");
        this.loopingOne = loopingOne;
        Log.v(TAG, "set loopingOne end");
    }

    // endregion playbackLogic

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    // For updating the SeekBar
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private RunnableSeekBarUpdater runnableSeekBarUpdater;

    public void updateSeekBarUpdater(SeekBar seekBar, TextView textViewCurrent) {
        Log.v(TAG, "updateSeekBarUpdater start");
        shutDownSeekBarUpdater();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        MediaPlayerWUri mediaPlayerWUri = getCurrentMediaPlayerWUri();
        if(mediaPlayerWUri != null) {
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
        if(runnableSeekBarUpdater != null) {
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

    final MediaPlayer.OnCompletionListener onCompletionListener =
            new MediaPlayerOnCompletionListener(this);

    private RemoteViews remoteViewsNotificationLayout;
    private RemoteViews remoteViewsNotificationLayoutWithoutArt;
    private RemoteViews remoteViewsNotificationLayoutWithArt;
    private NotificationCompat.Builder notificationCompatBuilder;
    private Notification notification;
    private boolean hasArt = false;

    // region ActivityMainUI

    private boolean fragmentSongVisible = false;

    public void fragmentSongVisible(boolean fragmentSongVisible) {
        Log.v(TAG, "fragmentSongVisible start");
        this.fragmentSongVisible = fragmentSongVisible;
        Log.v(TAG, "fragmentSongVisible end");
    }

    public boolean fragmentSongVisible() {
        Log.v(TAG, "fragmentSongVisible start and end");
        return fragmentSongVisible;
    }

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
    private boolean haveAudioFocus = false;
    private boolean saveFileLoaded = false;
    private boolean audioFilesLoaded = false;

    private final IBinder serviceMainBinder = new ServiceMainBinder();

    // region onCreate

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate started");
        super.onCreate();
        if (!saveFileLoaded) {
            Log.v(TAG, "onCreate is loading");
            loadSaveFile();
            setUpBroadCastReceivers();
            // setUpExceptionSaver();
            // logLastThrownException();
            Log.v(TAG, "onCreate done loading");
        } else {
            Log.v(TAG, "onCreate already loaded");
        }
        Log.v(TAG, "onCreate ended");
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

    private void loadSaveFile() {
        Log.v(TAG, "Loading the save file");
        File file = new File(getBaseContext().getFilesDir(), FILE_SAVE);
        if (file.exists()) {
            try (FileInputStream fileInputStream = getApplicationContext().openFileInput(FILE_SAVE);
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                maxPercent = objectInputStream.readDouble();
                percentChangeUp = objectInputStream.readDouble();
                percentChangeDown = objectInputStream.readDouble();
                masterPlaylist = (RandomPlaylist) objectInputStream.readObject();
                int playlistSize = objectInputStream.readInt();
                for (int i = 0; i < playlistSize; i++) {
                    playlists.add((RandomPlaylist) objectInputStream.readObject());
                }
                directoryPlaylists = (TreeMap<Long, RandomPlaylist>) objectInputStream.readObject();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error encountered while loading the save file");
            }
        }
        saveFileLoaded = true;
        Log.v(TAG, "Save file loaded");
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
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, importance);
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
        if(hasArt){
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
        // TODO update song art backgorund
        if (currentSong != null) {
            Bitmap bitmap = ActivityMain.getThumbnail(currentSong, 92, 92, getApplicationContext());
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
        if (currentSong != null) {
            remoteViewsNotificationLayoutWithoutArt.setTextViewText(
                    R.id.textViewNotificationSongPaneSongName, currentSong.title);
            remoteViewsNotificationLayoutWithArt.setTextViewText(
                    R.id.textViewNotificationSongPaneSongNameWArt, currentSong.title);
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
        if (isPlaying) {
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
        if (isPlaying) {
            pauseOrPlay();
        }
        releaseMediaPlayers();
        shutDownSeekBarUpdater();
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        unregisterReceiver(broadcastReceiverNotificationButtonsForServiceMainButtons);
        stopSelf();
        Log.v(TAG, "destroy ended");
    }

    public void releaseMediaPlayers() {
        Log.v(TAG, "Releasing MediaPlayers");
        synchronized (this) {
            Iterator<MediaPlayerWUri> iterator = uriMediaPlayerWUriHashMap.values().iterator();
            MediaPlayerWUri mediaPlayerWURI;
            while (iterator.hasNext()) {
                mediaPlayerWURI = iterator.next();
                mediaPlayerWURI.release();
                iterator.remove();
            }
        }
        Log.v(TAG, "Done releasing MediaPlayers");
    }

    void getAudioFiles() {
        Log.v(TAG, "Getting audio files");
        if (!audioFilesLoaded) {
            String[] projection = new String[]{
                    MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.IS_MUSIC,
                    MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.TITLE, MediaStore.Images.Media.DATA
            };
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != ?";
            String[] selectionArgs = new String[]{"0"};
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            try (Cursor cursor = getApplicationContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    List<Uri> newURIs = getURIs(cursor);
                    if (!uriAudioURILinkedHashMap.isEmpty() && saveFileLoaded) {
                        if (masterPlaylist != null) {
                            addNewSongs();
                            removeMissingSongs(newURIs);
                        } else {
                            masterPlaylist = new RandomPlaylist(
                                    MASTER_PLAYLIST_NAME, new ArrayList<>(
                                    uriAudioURILinkedHashMap.values()),
                                    maxPercent, true, -1);
                        }
                        currentPlaylist = masterPlaylist;
                        songQueueIterator = songQueue.listIterator();
                    }
                }
            }
            audioFilesLoaded = true;
        }
        Log.v(TAG, "Done getting audio files");
    }

    private void removeMissingSongs(List<Uri> newURIs) {
        for (AudioUri audioURI : masterPlaylist.getAudioUris()) {
            if (!newURIs.contains(audioURI.getUri())) {
                masterPlaylist.remove(audioURI);
                uriAudioURILinkedHashMap.remove(audioURI.getUri());
                uriMediaPlayerWUriHashMap.remove(audioURI.getUri());
            }
        }
    }

    private void addNewSongs() {
        for (AudioUri audioURIFromUriMap : uriAudioURILinkedHashMap.values()) {
            if (audioURIFromUriMap != null) {
                if (!masterPlaylist.contains(audioURIFromUriMap)) {
                    masterPlaylist.add(audioURIFromUriMap);
                }
            }
        }
    }

    private List<Uri> getURIs(Cursor cursor) {
        Log.v(TAG, "Getting uris");
        ArrayList<Uri> newURIs = new ArrayList<>();
        int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
        int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(idCol);
            String displayName = cursor.getString(nameCol);
            String title = cursor.getString(titleCol);
            String artist = cursor.getString(artistCol);
            String data = cursor.getString(dataCol);
            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            if (!uriAudioURILinkedHashMap.containsKey(uri)) {
                AudioUri audioURI = new AudioUri(uri, displayName, artist, title, id);
                uriAudioURILinkedHashMap.put(uri, audioURI);
            }
            newURIs.add(uri);
        }
        Log.v(TAG, "Done getting uris");
        return newURIs;
    }

    // region mediaControls

    public void pauseOrPlay() {
        Log.v(TAG, "pauseOrPlay started");
        if (currentSong != null) {
            MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.pause();
                    isPlaying = false;
                } else {
                    if (requestAudioFocus()) {
                        mediaPlayerWURI.shouldPlay(true);
                        isPlaying = true;
                        songInProgress = true;
                    }
                }
            }
        } else if (!isPlaying) {
            playNext();
        }
        updateNotification();
        Log.v(TAG, "pauseOrPlay ended");
    }

    public boolean playNextInQueue(boolean addNew) {
        Log.v(TAG, "playNextInQueue started");
        if (songQueueIterator.hasNext()) {
            playAndMakeIfNeeded(songQueueIterator.next());
        } else if (looping) {
            if (shuffling) {
                songQueueIterator = songQueue.listIterator();
                if (songQueueIterator.hasNext()) {
                    playAndMakeIfNeeded(songQueueIterator.next());
                }
            } else {
                Log.v(TAG, "playNextInQueue end");
                return false;
            }
        } else if (addNew) {
            if (shuffling) {
                addToQueueAndPlay(currentPlaylist.next(random).getUri());
            } else {
                Log.v(TAG, "playNextInQueue end");
                return false;
            }
        }
        Log.v(TAG, "playNextInQueue end");
        return true;
    }

    public boolean playPreviousInQueue() {
        Log.v(TAG, "playPreviousInQueue started");
        if (songQueueIterator.hasPrevious()) {
            songQueueIterator.previous();
            if (songQueueIterator.hasPrevious()) {
                playAndMakeIfNeeded(songQueueIterator.previous());
                songQueueIterator.next();
            } else if (looping) {
                if (shuffling) {
                    songQueueIterator = songQueue.listIterator(songQueue.size());
                    if (songQueueIterator.hasPrevious()) {
                        playAndMakeIfNeeded(songQueueIterator.previous());
                        songQueueIterator.next();
                    } else {
                        Log.v(TAG, "playPreviousInQueue end");
                        return false;
                    }
                }
            } else {
                seekTo(0);
            }
            Log.v(TAG, "playPreviousInQueue end");
            return true;
        }
        Log.v(TAG, "playPreviousInQueue end");
        return false;
    }

    public void playNextInPlaylist() {
        Log.v(TAG, "playNextInPlaylist started");
        if (currentPlaylistIterator.hasNext()) {
            playAndMakeIfNeeded(currentPlaylistIterator.next().getUri());
        } else if (looping) {
            currentPlaylistIterator = currentPlaylistArray.listIterator();
            if (currentPlaylistIterator.hasNext()) {
                playAndMakeIfNeeded(currentPlaylistIterator.next().getUri());
            }
        }
        Log.v(TAG, "playNextInPlaylist end");
    }

    public void playPreviousInPlaylist() {
        Log.v(TAG, "playPreviousInPlaylist started");
        if (currentPlaylistIterator.hasPrevious()) {
            playAndMakeIfNeeded(currentPlaylistIterator.previous().getUri());
        } else if (looping) {
            currentPlaylistIterator = currentPlaylistArray.listIterator(currentPlaylistArray.size());
            if (currentPlaylistIterator.hasPrevious()) {
                playAndMakeIfNeeded(currentPlaylistIterator.previous().getUri());
            }
        }
        Log.v(TAG, "playPreviousInPlaylist end");
    }

    public void playNext() {
        Log.v(TAG, "playNext started");
        if (loopingOne) {
            playLoopingOne();
        } else if (!playNextInQueue(true)) {
            playNextInPlaylist();
        }
        Log.v(TAG, "playNext end");
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious started");
        if (loopingOne) {
            playLoopingOne();
        } else if (!playPreviousInQueue()) {
            playPreviousInPlaylist();
        }
        Log.v(TAG, "playPrevious end");
    }

    void playLoopingOne() {
        Log.v(TAG, "playLoopingOne started");
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            mediaPlayerWURI.seekTo(0);
            mediaPlayerWURI.shouldPlay(true);
            // TODO make a setting?
            //addToQueueAtCurrentIndex(currentSong.getUri());
        } else {
            playAndMakeIfNeeded(currentSong.getUri());
        }
        Log.v(TAG, "playLoopingOne end");
    }

    public void addToQueueAndPlay(Uri uri) {
        Log.v(TAG, "addToQueueAndPlay started");
        playAndMakeIfNeeded(uri);
        songQueueIterator = null;
        songQueue.add(uri);
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(uri));
        songQueueIterator.next();
        Log.v(TAG, "addToQueueAndPlay end");
    }

    public void addToQueue(Uri uri) {
        Log.v(TAG, "addToQueue started");
        int pos = songQueueIterator.nextIndex();
        songQueueIterator = null;
        songQueue.add(uri);
        songQueueIterator = songQueue.listIterator(pos);
        Log.v(TAG, "addToQueue end");
    }

    public void clearSongQueue() {
        Log.v(TAG, "clearSongQueue started");
        songQueueIterator = null;
        songQueue.clear();
        songQueueIterator = songQueue.listIterator();
        Log.v(TAG, "clearSongQueue end");
    }

    void playAndMakeIfNeeded(Uri uri) {
        Log.v(TAG, "playAndMakeIfNeeded w/ Uri started");
        MediaPlayerWUri mediaPlayerWURI = getMediaPlayerWUri(uri);
        if (mediaPlayerWURI != null) {
            playAndMakeIfNeeded(mediaPlayerWURI);
        } else {
            AudioUri audioUri = uriAudioURILinkedHashMap.get(uri);
            if(audioUri != null) {
                makeMediaPlayerWURIAndPlay(audioUri);
            }
        }
        Log.v(TAG, "playAndMakeIfNeeded w/ Uri end");
    }

    private void playAndMakeIfNeeded(MediaPlayerWUri mediaPlayerWURI) {
        Log.v(TAG, "playAndMakeIfNeeded started");
        stopCurrentSong();
        if (requestAudioFocus()) {
            mediaPlayerWURI.shouldPlay(true);
            isPlaying = true;
            songInProgress = true;
        }
        currentSong = mediaPlayerWURI.audioURI;
        updateNotification();
        if (currentPlaylistArray != null) {
            int i = currentPlaylistArray.indexOf(mediaPlayerWURI.audioURI);
            currentPlaylistIterator = currentPlaylistArray.listIterator(i + 1);
        }
        Log.v(TAG, "playAndMakeIfNeeded ended");
    }

    private void stopCurrentSong() {
        Log.v(TAG, "stopCurrentSong started");
        if (currentSong != null) {
            MediaPlayerWUri mediaPlayerWURI = getMediaPlayerWUri(currentSong.getUri());
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.stop();
                    mediaPlayerWURI.prepareAsync();
                }
            } else {
                releaseMediaPlayers();
            }
            songInProgress = false;
            isPlaying = false;
        }
        Log.v(TAG, "stopCurrentSong end");
    }

    public void seekTo(int progress) {
        Log.v(TAG, "seekTo started");
        MediaPlayerWUri mediaPlayerWUri = getCurrentMediaPlayerWUri();
        if (mediaPlayerWUri != null) {
            mediaPlayerWUri.seekTo(progress);
        }
        Log.v(TAG, "seekTo end");
    }

    private void makeMediaPlayerWURIAndPlay(AudioUri audioURI) {
        Log.v(TAG, "makeMediaPlayerWURIAndPlay started");
        MediaPlayerWUri mediaPlayerWURI =
                new CallableCreateMediaPlayerWURI(this, audioURI).call();
        uriMediaPlayerWUriHashMap.put(mediaPlayerWURI.audioURI.getUri(), mediaPlayerWURI);
        playAndMakeIfNeeded(mediaPlayerWURI);
        Log.v(TAG, "makeMediaPlayerWURIAndPlay ended");
    }

    void stopAndPreparePrevious() {
        Log.v(TAG, "stopPreviousAndPrepare started");
        if (songQueueIterator.hasPrevious()) {
            final MediaPlayerWUri mediaPlayerWURI =
                    getMediaPlayerWUri(songQueueIterator.previous());
            songQueueIterator.next();
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.stop();
                    mediaPlayerWURI.prepareAsync();
                }
            } else {
                releaseMediaPlayers();
            }
            songInProgress = false;
            isPlaying = false;
        }
        Log.v(TAG, "stopPreviousAndPrepare ended");
    }

    public class CallableCreateMediaPlayerWURI implements Callable<MediaPlayerWUri> {

        final ServiceMain serviceMain;

        final AudioUri audioURI;

        CallableCreateMediaPlayerWURI(ServiceMain serviceMain, AudioUri audioURI) {
            this.serviceMain = serviceMain;
            this.audioURI = audioURI;
        }

        @Override
        public MediaPlayerWUri call() {
            Log.v(TAG, "makeMediaPlayerWURI being made");
            MediaPlayerWUri mediaPlayerWURI = new MediaPlayerWUri(
                    serviceMain, MediaPlayer.create(
                    getApplicationContext(), audioURI.getUri()), audioURI);
            mediaPlayerWURI.setOnCompletionListener(onCompletionListener);
            Log.v(TAG, "makeMediaPlayerWURI made");
            return mediaPlayerWURI;
        }

    }

    private boolean requestAudioFocus() {
        Log.v(TAG, "Requesting audio focus");
        if (haveAudioFocus) {
            return true;
        }
        // TODO
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener =
                new AudioManager.OnAudioFocusChangeListener() {
                    final Object lock = new Object();
                    boolean mResumeOnFocusGain = false;

                    @Override
                    public void onAudioFocusChange(int i) {
                        switch (i) {
                            case AudioManager.AUDIOFOCUS_GAIN:
                                if (mResumeOnFocusGain) {
                                    synchronized (lock) {
                                        mResumeOnFocusGain = false;
                                    }
                                    if (!isPlaying) {
                                        pauseOrPlay();
                                    }
                                }
                                break;
                            case AudioManager.AUDIOFOCUS_LOSS:
                                synchronized (lock) {
                                    mResumeOnFocusGain = false;
                                }
                                if (isPlaying) {
                                    pauseOrPlay();
                                }
                                break;
                        }
                    }
                };
        int result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                    .build();
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(
                    onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        haveAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        Log.v(TAG, "Done requesting audio focus");
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    // endregion mediaControls

    void saveFile() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "saveFile started");
                File file = new File(getBaseContext().getFilesDir(), FILE_SAVE);
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                try (FileOutputStream fos = getApplicationContext().openFileOutput(FILE_SAVE, Context.MODE_PRIVATE);
                     ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos)) {
                    Log.v(TAG, "Creating save file");
                    objectOutputStream.writeDouble(maxPercent);
                    objectOutputStream.writeDouble(percentChangeUp);
                    objectOutputStream.writeDouble(percentChangeDown);
                    objectOutputStream.writeObject(masterPlaylist);
                    objectOutputStream.writeInt(playlists.size());
                    for (RandomPlaylist randomPlaylist : playlists) {
                        objectOutputStream.writeObject(randomPlaylist);
                    }
                    objectOutputStream.writeObject(directoryPlaylists);
                    objectOutputStream.flush();
                    Log.v(TAG, "Save file created");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.v(TAG, "Problem while trying to save file");
                }
                Log.v(TAG, "saveFile ended");
            }
        });
    }

}