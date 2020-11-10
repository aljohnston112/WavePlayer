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
        return maxPercent;
    }

    public void setMaxPercent(double maxPercent) {
        this.maxPercent = maxPercent;
        masterPlaylist.setMaxPercent(maxPercent);
        for (RandomPlaylist randomPlaylist : playlists) {
            randomPlaylist.setMaxPercent(maxPercent);
        }
        for (RandomPlaylist randomPlaylist : directoryPlaylists.values()) {
            randomPlaylist.setMaxPercent(maxPercent);
        }
    }

    private double percentChangeUp = 0.1;

    public double getPercentChangeUp() {
        return percentChangeUp;
    }

    public void setPercentChangeUp(double percentChangeUp) {
        this.percentChangeUp = percentChangeUp;
    }

    private double percentChangeDown = 0.5;

    public double getPercentChangeDown() {
        return percentChangeDown;
    }

    public void setPercentChangeDown(double percentChangeDown) {
        this.percentChangeDown = percentChangeDown;
    }

    // endregion settings

    private final HashMap<Uri, MediaPlayerWUri> uriMediaPlayerWUriHashMap = new HashMap<>();

    // TODO organize usages
    private MediaPlayerWUri getCurrentMediaPlayerWUri() {
        if (currentSong != null) {
            return uriMediaPlayerWUriHashMap.get(currentSong.getUri());
        }
        return null;
    }

    private MediaPlayerWUri getMediaPlayerWUri(Uri uri) {
        return uriMediaPlayerWUriHashMap.get(uri);
    }

    private final LinkedHashMap<Uri, AudioUri> uriAudioURILinkedHashMap = new LinkedHashMap<>();

    // region playlists

    private RandomPlaylist masterPlaylist;

    public List<AudioUri> getAllSongs() {
        return masterPlaylist.getAudioUris();
    }

    private final ArrayList<RandomPlaylist> playlists = new ArrayList<>();

    public ArrayList<RandomPlaylist> getPlaylists() {
        return playlists;
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        playlists.add(randomPlaylist);
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        playlists.add(position, randomPlaylist);
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        directoryPlaylists.remove(randomPlaylist.mediaStoreUriID);
        playlists.remove(randomPlaylist);
    }

    public RandomPlaylist getCurrentPlaylist() {
        return currentPlaylist;
    }

    private TreeMap<Long, RandomPlaylist> directoryPlaylists =
            new TreeMap<>(new ComparableLongsSerializable());

    static class ComparableLongsSerializable implements Serializable, Comparator<Long> {
        @Override
        public int compare(Long o1, Long o2) {
            return o1.compareTo(o2);
        }
    }

    public boolean containsDirectoryPlaylist(long mediaStoreUriID) {
        return directoryPlaylists.get(mediaStoreUriID) != null;
    }

    public void addDirectoryPlaylist(long uriID, RandomPlaylist randomPlaylist) {
        directoryPlaylists.put(uriID, randomPlaylist);
        addPlaylist(randomPlaylist);
    }

    public RandomPlaylist getDirectoryPlaylist(long mediaStoreUriID) {
        return directoryPlaylists.get(mediaStoreUriID);
    }

    // endregion playlists

    // region userPickedPlaylist

    private RandomPlaylist userPickedPlaylist;

    public RandomPlaylist getUserPickedPlaylist() {
        return userPickedPlaylist;
    }

    public void setUserPickedPlaylistToMasterPlaylist() {
        userPickedPlaylist = masterPlaylist;
    }

    public void setUserPickedPlaylist(RandomPlaylist userPickedPlaylist) {
        this.userPickedPlaylist = userPickedPlaylist;
    }

    // endregion userPickedPlaylist

    // region userPickedSongs

    private final List<AudioUri> userPickedSongs = new ArrayList<>();

    public List<AudioUri> getUserPickedSongs() {
        return userPickedSongs;
    }

    public void addUserPickedSong(AudioUri audioURI) {
        userPickedSongs.add(audioURI);
    }

    public void removeUserPickedSong(AudioUri audioURI) {
        userPickedSongs.remove(audioURI);
    }

    public void clearUserPickedSongs() {
        userPickedSongs.clear();
    }

    // endregion userPickedSongs

    // region currentPlaylist

    private RandomPlaylist currentPlaylist;
    private ArrayList<AudioUri> currentPlaylistArray;
    private ListIterator<AudioUri> currentPlaylistIterator;

    public void setCurrentPlaylistToMaster() {
        setCurrentPlaylist(masterPlaylist);
    }

    public void setCurrentPlaylist(RandomPlaylist currentPlaylist) {
        this.currentPlaylist = currentPlaylist;
        currentPlaylistArray = currentPlaylist.getAudioUris();
        clearSongQueue();
    }

    public void clearProbabilities() {
        currentPlaylist.clearProbabilities();
    }

    // endregion currentPlaylist

    // region currentSongQueue

    private final LinkedList<Uri> songQueue = new LinkedList<>();
    private ListIterator<Uri> songQueueIterator;

    public boolean songQueueIsEmpty() {
        return songQueue.size() == 0;
    }

    private AudioUri currentSong;

    public AudioUri getCurrentSong() {
        return currentSong;
    }

    public int getCurrentTime() {
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            return mediaPlayerWURI.getCurrentPosition();
        } else {
            return -1;
        }
    }

    private boolean isPlaying = false;

    public boolean isPlaying() {
        return isPlaying;
    }

    private boolean songInProgress = false;

    public boolean songInProgress() {
        return songInProgress;
    }

    // endregion currentSongQueue

    // region playbackLogic

    private boolean shuffling = true;

    public boolean shuffling() {
        return shuffling;
    }

    public void shuffling(boolean shuffling) {
        this.shuffling = shuffling;
    }

    private boolean looping = false;

    public boolean looping() {
        return looping;
    }

    public void looping(boolean looping) {
        this.looping = looping;
    }

    private boolean loopingOne = false;

    public boolean loopingOne() {
        return loopingOne;
    }

    public void loopingOne(boolean loopingOne) {
        this.loopingOne = loopingOne;
    }

    // endregion playbackLogic

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    // For updating the SeekBar
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public void updateSeekBarUpdater(SeekBar seekBar, TextView textViewCurrent, int maxMillis) {
        shutDownSeekBarUpdater();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(
                new RunnableSeekBarUpdater(
                        getCurrentMediaPlayerWUri(),
                        seekBar, textViewCurrent, maxMillis,
                        getResources().getConfiguration().locale),
                0L, 1L, TimeUnit.SECONDS);
    }

    public void shutDownSeekBarUpdater() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
    }

    private final BroadcastReceiverNotificationButtonsForServiceMain
            broadcastReceiverNotificationButtonsForServiceMainButtons =
            new BroadcastReceiverNotificationButtonsForServiceMain(this);

    private final MediaPlayer.OnCompletionListener onCompletionListener =
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
        this.fragmentSongVisible = fragmentSongVisible;
    }

    public boolean fragmentSongVisible() {
        return fragmentSongVisible;
    }

    private int songPaneArtHeight = 1;

    public int getSongPaneArtHeight() {
        return songPaneArtHeight;
    }

    public void setSongPaneArtHeight(int songArtHeight) {
        this.songPaneArtHeight = songArtHeight;
    }

    private int songPaneArtWidth = 1;

    public int getSongPaneArtWidth() {
        return songPaneArtWidth;
    }

    public void setSongPaneArtWidth(int songArtWidth) {
        this.songPaneArtWidth = songArtWidth;
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
        super.onCreate();
        Log.v(TAG, "onCreate started");
        if (!saveFileLoaded) {
            Log.v(TAG, "onCreate is loading");
            setUpBroadCastReceivers();
            // setUpExceptionSaver();
            // logLastThrownException();
            loadSaveFile();
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
            setUpNotificationBuilder();
            setUpBroadCastsForNotificationButtons();
            notification = notificationCompatBuilder.build();
            updateNotification();
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
                .setSmallIcon(R.drawable.music_note_black_48dp)
                .setContentTitle(NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        remoteViewsNotificationLayout =
                new RemoteViews(getPackageName(), R.layout.pane_notification);
        remoteViewsNotificationLayoutWithoutArt =
                new RemoteViews(getPackageName(), R.layout.pane_notification_without_art);
        remoteViewsNotificationLayoutWithArt =
                new RemoteViews(getPackageName(), R.layout.pane_notification_with_art);
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
        // TODO update song art backgorund
        if (currentSong != null) {
            Bitmap bitmap = AudioUri.getThumbnail(currentSong, 50, 50, getApplicationContext());
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
    }

    private void updateNotificationSongName() {
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
    }

    void updateNotificationPlayButton() {
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
                AudioUri audioURI = new AudioUri(uri, data, displayName, artist, title, id);
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
                if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.pause();
                    isPlaying = false;
                } else {
                    if (requestAudioFocus()) {
                        mediaPlayerWURI.shouldStart(true);
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
        if (songQueueIterator.hasNext()) {
            playAndMakeIfNeeded(songQueueIterator.next());
        } else if (looping) {
            if (shuffling) {
                songQueueIterator = songQueue.listIterator();
                if (songQueueIterator.hasNext()) {
                    playAndMakeIfNeeded(songQueueIterator.next());
                }
            } else {
                return false;
            }
        } else if (addNew) {
            if (shuffling) {
                addToQueueAndPlay(currentPlaylist.next(random).getUri());
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean playPreviousInQueue() {
        if (songQueueIterator.hasPrevious()) {
            Uri uri = songQueueIterator.previous();
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
                        return false;
                    }
                }
            } else {
                seekTo(0);
            }
            return true;
        }
        return false;
    }

    public void playNextInPlaylist() {
        if (currentPlaylistIterator.hasNext()) {
            playAndMakeIfNeeded(currentPlaylistIterator.next().getUri());
        } else if (looping) {
            currentPlaylistIterator = currentPlaylistArray.listIterator();
            if (currentPlaylistIterator.hasNext()) {
                playAndMakeIfNeeded(currentPlaylistIterator.next().getUri());
            }
        }
    }

    public void playPreviousInPlaylist() {
        if (currentPlaylistIterator.hasPrevious()) {
            playAndMakeIfNeeded(currentPlaylistIterator.previous().getUri());
        } else if (looping) {
            currentPlaylistIterator = currentPlaylistArray.listIterator(currentPlaylistArray.size());
            if (currentPlaylistIterator.hasPrevious()) {
                playAndMakeIfNeeded(currentPlaylistIterator.previous().getUri());
            }
        }
    }

    public void playNext() {
        if (loopingOne) {
            playLoopingOne();
        } else if (!playNextInQueue(true)) {
            playNextInPlaylist();
        }
    }

    public void playPrevious() {
        if (loopingOne) {
            playLoopingOne();
        } else if (!playPreviousInQueue()) {
            playPreviousInPlaylist();
        }
    }

    void playLoopingOne() {
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            mediaPlayerWURI.seekTo(0);
            mediaPlayerWURI.shouldStart(true);
            // TODO make a setting?
            //addToQueueAtCurrentIndex(currentSong.getUri());
        } else {
            playAndMakeIfNeeded(currentSong.getUri());
        }
    }

    public void addToQueueAndPlay(Uri uri) {
        playAndMakeIfNeeded(uri);
        songQueueIterator = null;
        songQueue.add(uri);
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(uri));
        songQueueIterator.next();
    }

    public void addToQueue(Uri uri) {
        int pos = songQueueIterator.nextIndex();
        songQueueIterator = null;
        songQueue.add(uri);
        songQueueIterator = songQueue.listIterator(pos);
    }

    public void clearSongQueue() {
        songQueueIterator = null;
        songQueue.clear();
        songQueueIterator = songQueue.listIterator();
    }

    void playAndMakeIfNeeded(Uri uri) {
        MediaPlayerWUri mediaPlayerWURI = getMediaPlayerWUri(uri);
        if (mediaPlayerWURI != null) {
            playAndMakeIfNeeded(mediaPlayerWURI);
        } else {
            makeMediaPlayerWURIAndPlay(uriAudioURILinkedHashMap.get(uri));
        }
    }

    private void playAndMakeIfNeeded(MediaPlayerWUri mediaPlayerWURI) {
        Log.v(TAG, "play started");
        stopCurrentSong();
        if (requestAudioFocus()) {
            mediaPlayerWURI.shouldStart(true);
            isPlaying = true;
            songInProgress = true;
        }
        currentSong = mediaPlayerWURI.audioURI;
        updateNotification();
        if (currentPlaylistArray != null) {
            int i = currentPlaylistArray.indexOf(mediaPlayerWURI.audioURI);
            currentPlaylistIterator = currentPlaylistArray.listIterator(i + 1);
        }
        Log.v(TAG, "play ended");
    }

    private void stopCurrentSong() {
        if (currentSong != null) {
            MediaPlayerWUri mediaPlayerWURI = getMediaPlayerWUri(currentSong.getUri());
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.stop();
                    mediaPlayerWURI.prepareAsync();
                }
            } else {
                releaseMediaPlayers();
            }
            songInProgress = false;
            isPlaying = false;
        }
    }

    public void seekTo(int progress) {
        MediaPlayerWUri mediaPlayerWUri = getCurrentMediaPlayerWUri();
        if (mediaPlayerWUri != null) {
            mediaPlayerWUri.seekTo(progress);
        }
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
                if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
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