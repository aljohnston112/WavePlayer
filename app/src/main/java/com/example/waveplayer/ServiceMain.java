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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;
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

public class ServiceMain extends Service {

    public static final String TAG = "ServiceMain";
    private static final String FILE_ERROR_LOG = "error";
    private static final String FILE_SAVE = "playlists";
    private final static String NOTIFICATION_CHANNEL_ID = "PinkyPlayer";
    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";

    public static final Random random = new Random();

    // TODO make fragments communicate
    RandomPlaylist userPickedPlaylist;
    final ArrayList<AudioURI> userPickedSongs = new ArrayList<>();

    // Settings
    static double MAX_PERCENT = 0.1;
    static double PERCENT_CHANGE_UP = 0.1;
    static double PERCENT_CHANGE_DOWN = 0.5;

    final HashMap<Uri, MediaPlayerWURI> uriMediaPlayerWURIHashMap = new HashMap<>();
    final public LinkedHashMap<Uri, AudioURI> uriAudioURILinkedHashMap = new LinkedHashMap<>();

    RandomPlaylist masterPlaylist;
    final ArrayList<RandomPlaylist> playlists = new ArrayList<>();
    public TreeMap<Long, RandomPlaylist> directoryPlaylists = new TreeMap<>(new ComparableLongsSerializable());
    static class ComparableLongsSerializable implements Serializable, Comparator<Long> {
        @Override
        public int compare(Long o1, Long o2) {
            return o1.compareTo(o2);
        }
    }
    RandomPlaylist currentPlaylist;

    final LinkedList<Uri> songQueue = new LinkedList<>();
    ListIterator<Uri> songQueueIterator;
    ArrayList<AudioURI> currentPlaylistArray;
    ListIterator<AudioURI> currentPlaylistIterator;

    AudioURI currentSong;
    public AudioURI getCurrentSong() {
        return currentSong;
    }

    public boolean isPlaying = false;
    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean songInProgress = false;
    public boolean songInProgress() {
        return songInProgress;
    }

    boolean fragmentSongVisible = false;

    boolean shuffling = true;
    boolean looping = false;
    boolean loopingOne = false;

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    // For updating the SeekBar
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    BroadcastReceiverNotificationButtonsForServiceMain
            broadcastReceiverNotificationButtonsForServiceMainButtons =
            new BroadcastReceiverNotificationButtonsForServiceMain(this);

    public final MediaPlayer.OnCompletionListener onCompletionListener =
            new MediaPlayerOnCompletionListener(this);

    private RemoteViews remoteViewsNotificationLayout;
    private NotificationCompat.Builder notificationCompatBuilder;
    private Notification notification;

    int songPaneArtHeight;

    private boolean serviceStarted = false;
    private boolean haveAudioFocus = false;
    private boolean saveFileLoaded = false;
    public boolean audioFilesLoaded = false;

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
                MAX_PERCENT = objectInputStream.readDouble();
                PERCENT_CHANGE_UP = objectInputStream.readDouble();
                PERCENT_CHANGE_DOWN = objectInputStream.readDouble();
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
            Toast.makeText(getApplicationContext(), "PinkyPlayer starting", Toast.LENGTH_SHORT).show();
            setUpNotificationBuilder();
            setUpBroadCastsForNotificationButtons();
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
                .setSmallIcon(R.drawable.music_note_black_48dp)
                .setContentTitle(NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        remoteViewsNotificationLayout = new RemoteViews(getPackageName(), R.layout.pane_notification);
        if (currentSong != null) {
            remoteViewsNotificationLayout.setTextViewText(
                    R.id.textViewNotificationSongPaneSongName, currentSong.title);
        } else {
            remoteViewsNotificationLayout.setTextViewText(
                    R.id.textViewNotificationSongPaneSongName, NOTIFICATION_CHANNEL_ID);
        }
        notificationCompatBuilder.setCustomContentView(remoteViewsNotificationLayout);
        Intent notificationIntent = new Intent(getApplicationContext(), ActivityMain.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
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
        updateNotificationSongName();
        updateNotificationPlayButton();
        // TODO update song art
        notification.contentView = remoteViewsNotificationLayout;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_CHANNEL_ID.hashCode(), notification);
        Log.v(TAG, "Done updating the notification");
    }

    private void updateNotificationSongName() {
        if (currentSong != null) {
            remoteViewsNotificationLayout.setTextViewText(
                    R.id.textViewNotificationSongPaneSongName, currentSong.title);
        } else {
            remoteViewsNotificationLayout.setTextViewText(
                    R.id.textViewNotificationSongPaneSongName, NOTIFICATION_CHANNEL_ID);
        }
    }

    void updateNotificationPlayButton() {
        if (isPlaying) {
            remoteViewsNotificationLayout.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPause, R.drawable.pause_black_24dp);
        } else {
            remoteViewsNotificationLayout.setImageViewResource(
                    R.id.imageButtonNotificationSongPanePlayPause, R.drawable.play_arrow_black_24dp);
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
        remoteViewsNotificationLayout.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPaneNext, pendingIntentNext);
    }

    private void setUpBroadcastPlayPause() {
        Intent intentPlayPause = new Intent(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(
                getApplicationContext(), 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewsNotificationLayout.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePlayPause, pendingIntentPlayPause);
    }

    private void setUpBroadcastPrevious() {
        Intent intentPrev = new Intent(getResources().getString(R.string.broadcast_receiver_action_previous));
        intentPrev.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(
                getApplicationContext(), 0, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewsNotificationLayout.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePrev, pendingIntentPrev);
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
        Toast.makeText(this, "PinkyPlayer done", Toast.LENGTH_SHORT).show();
        Log.v(TAG, "onDestroy ended");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.v(TAG, "onTaskRemoved started");
        super.onTaskRemoved(rootIntent);
        destroy();
        Log.v(TAG, "onTaskRemoved ended");
    }

    public void destroy() {
        Log.v(TAG, "destroy started");
        if (isPlaying) {
            pauseOrPlay();
        }
        releaseMediaPlayers();
        unregisterReceiver(broadcastReceiverNotificationButtonsForServiceMainButtons);
        stopSelf();
        Log.v(TAG, "destroy ended");
    }

    public void releaseMediaPlayers() {
        Log.v(TAG, "Releasing MediaPlayers");
        synchronized (this) {
            Iterator<MediaPlayerWURI> iterator = uriMediaPlayerWURIHashMap.values().iterator();
            MediaPlayerWURI mediaPlayerWURI;
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
                                    new ArrayList<>(uriAudioURILinkedHashMap.values()),
                                    MAX_PERCENT, MASTER_PLAYLIST_NAME,
                                    true, -1);
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
        for (AudioURI audioURI : masterPlaylist.getProbFun().getProbMap().keySet()) {
            if (!newURIs.contains(audioURI.getUri())) {
                masterPlaylist.getProbFun().remove(audioURI);
                uriAudioURILinkedHashMap.remove(audioURI.getUri());
                uriMediaPlayerWURIHashMap.remove(audioURI.getUri());
            }
        }
    }

    private void addNewSongs() {
        for (AudioURI audioURIFromURIMap : uriAudioURILinkedHashMap.values()) {
            if (audioURIFromURIMap != null) {
                if (!masterPlaylist.getProbFun().getProbMap().containsKey(audioURIFromURIMap)) {
                    masterPlaylist.getProbFun().add(audioURIFromURIMap);
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
                AudioURI audioURI = new AudioURI(uri, data, displayName, artist, title, id);
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
            MediaPlayerWURI mediaPlayerWURI = uriMediaPlayerWURIHashMap.get(currentSong.getUri());
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.pause();
                    isPlaying = false;
                } else {
                    if (haveAudioFocus) {
                        mediaPlayerWURI.shouldStart(true);
                        isPlaying = true;
                        songInProgress = true;
                    } else {
                        if (requestAudioFocus()) {
                            mediaPlayerWURI.shouldStart(true);
                            isPlaying = true;
                            songInProgress = true;
                        }
                    }
                }
            }
        } else {
            playNext();
        }
        Log.v(TAG, "pauseOrPlay ended");
    }

    void playNext() {
        Log.v(TAG, "playNext started");
        if (currentSong != null) {
            currentPlaylist.getProbFun().bad(getCurrentSong(), PERCENT_CHANGE_DOWN);
            if (loopingOne) {
                MediaPlayerWURI mediaPlayerWURI = uriMediaPlayerWURIHashMap.get(currentSong.getUri());
                if (mediaPlayerWURI != null) {
                    mediaPlayerWURI.seekTo(0);
                    addToQueueAtCurrentIndex(currentSong.getUri());
                } else {
                    play(currentSong.getUri());
                }
                saveFile();
                return;
            }
        }
        stopAndPreparePrevious();
        if (!shuffling) {
            if (currentPlaylistIterator.hasNext()) {
                addToQueueAndPlay(currentPlaylistIterator.next().getUri());
            } else if (looping) {
                currentPlaylistIterator = currentPlaylistArray.listIterator(0);
                addToQueueAndPlay(currentPlaylistIterator.next().getUri());
            } else {
                isPlaying = false;
                songInProgress = false;
            }
        } else {
            if (songQueueIterator.hasNext()) {
                playNextInQueue();
            } else if (looping) {
                songQueueIterator = songQueue.listIterator(0);
                play(songQueueIterator.next());
            } else {
                addToQueueAndPlay(currentPlaylist.getProbFun().fun(random));
            }
        }
        Log.v(TAG, "playNext ended");
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious started");
        if (currentSong != null) {
            if (loopingOne) {
                MediaPlayerWURI mediaPlayerWURI = uriMediaPlayerWURIHashMap.get(currentSong.getUri());
                if (mediaPlayerWURI != null) {
                    mediaPlayerWURI.seekTo(0);
                } else {
                    play(currentSong.getUri());
                }
                return;
            }
        }
        stopAndPreparePrevious();
        if (!shuffling) {
            if (currentPlaylistIterator.hasPrevious()) {
                currentPlaylistIterator.previous();
                if (currentPlaylistIterator.hasPrevious()) {
                    addToQueueAndPlay(currentPlaylistIterator.previous().getUri());
                    currentPlaylistIterator.next();
                    currentPlaylistIterator.next();
                } else if (looping) {
                    currentPlaylistIterator = currentPlaylistArray.listIterator(
                            currentPlaylistArray.size() - 1);
                    addToQueueAndPlay(currentPlaylistIterator.next().getUri());
                    return;
                }
            } else {
                MediaPlayerWURI mediaPlayerWURI = uriMediaPlayerWURIHashMap.get(currentSong.getUri());
                if (mediaPlayerWURI != null) {
                    mediaPlayerWURI.seekTo(0);
                }
            }
            return;
        }
        Uri uri = null;
        if (songQueueIterator.hasPrevious()) {
            uri = songQueueIterator.previous();
            if (songQueueIterator.hasPrevious()) {
                playPrevousInQueue();
                return;
            } else if (looping) {
                songQueueIterator = songQueue.listIterator(songQueue.size() - 2);
                play(songQueueIterator.next());
                songQueueIterator.next();
            } else if (uri != null) {
                play(uri);
                songQueueIterator.next();
            }
        }
        Log.v(TAG, "playPrevious ended");
    }

    void addToQueueAndPlay(AudioURI audioURI) {
        Log.v(TAG, "addToQueueAndPlay started");
        stopAndPreparePrevious();
        play(audioURI.getUri());
        Log.v(TAG, "addToQueueAndPlay ended");
    }

    public void addToQueueAndPlay(Uri uri) {
        stopAndPreparePrevious();
        play(uri);
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

    void addToQueueAtCurrentIndex(Uri uri) {
        songQueueIterator.add(uri);
    }

    public void clearSongQueue() {
        songQueueIterator = null;
        songQueue.clear();
        songQueueIterator = songQueue.listIterator();
    }

    private void play(MediaPlayerWURI mediaPlayerWURI) {
        Log.v(TAG, "play started");
        if (haveAudioFocus) {
            mediaPlayerWURI.shouldStart(true);
            isPlaying = true;
            songInProgress = true;
        } else {
            if (requestAudioFocus()) {
                mediaPlayerWURI.shouldStart(true);
                isPlaying = true;
                songInProgress = true;
            }
        }
        currentSong = mediaPlayerWURI.audioURI;
        if(currentPlaylistArray != null) {
            int i = currentPlaylistArray.indexOf(mediaPlayerWURI.audioURI);
            currentPlaylistIterator = currentPlaylistArray.listIterator(i + 1);
        }
        updateNotification();
        Log.v(TAG, "play ended");
    }

    void play(Uri uri) {
        MediaPlayerWURI mediaPlayerWURI = uriMediaPlayerWURIHashMap.get(uri);
        if (mediaPlayerWURI != null) {
            play(mediaPlayerWURI);
        } else {
            makeMediaPlayerWURIAndPlay(uriAudioURILinkedHashMap.get(uri));
        }
    }

    void playNextInQueue() {
        Log.v(TAG, "playNextInQueue started");
        Uri uri = songQueueIterator.next();
        MediaPlayerWURI mediaPlayerWURI = uriMediaPlayerWURIHashMap.get(uri);
        if (mediaPlayerWURI != null) {
            play(mediaPlayerWURI);
        } else {
            makeMediaPlayerWURIAndPlay(uriAudioURILinkedHashMap.get(uri));
        }
        Log.v(TAG, "playNextInQueue ended");
    }

    private void playPrevousInQueue() {
        Log.v(TAG, "playPreviousInQueue started");
        Uri uri = songQueueIterator.previous();
        play(uri);
        songQueueIterator.next();
        Log.v(TAG, "playPreviousInQueue ended");
    }

    private MediaPlayerWURI makeMediaPlayerWURIAndPlay(AudioURI audioURI) {
        Log.v(TAG, "makeMediaPlayerWURIAndPlay started");
        MediaPlayerWURI mediaPlayerWURI =
                new CallableCreateMediaPlayerWURI(this, audioURI).call();
        uriMediaPlayerWURIHashMap.put(mediaPlayerWURI.audioURI.getUri(), mediaPlayerWURI);
        play(mediaPlayerWURI);
        Log.v(TAG, "makeMediaPlayerWURIAndPlay ended");
        return mediaPlayerWURI;
    }

    void stopAndPreparePrevious() {
        Log.v(TAG, "stopPreviousAndPrepare started");
        if (songQueueIterator.hasPrevious()) {
            final MediaPlayerWURI mediaPlayerWURI =
                    uriMediaPlayerWURIHashMap.get(songQueueIterator.previous());
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

    public class CallableCreateMediaPlayerWURI implements Callable<MediaPlayerWURI> {

        final ServiceMain serviceMain;

        final AudioURI audioURI;

        CallableCreateMediaPlayerWURI(ServiceMain serviceMain, AudioURI audioURI) {
            this.serviceMain = serviceMain;
            this.audioURI = audioURI;
        }

        @Override
        public MediaPlayerWURI call() {
            Log.v(TAG, "makeMediaPlayerWURI being made");
            MediaPlayerWURI mediaPlayerWURI = new MediaPlayerWURI(
                    serviceMain, MediaPlayer.create(
                            getApplicationContext(), audioURI.getUri()), audioURI);
            mediaPlayerWURI.setOnCompletionListener(null);
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
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int i) {

            }
        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
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
                    objectOutputStream.writeDouble(MAX_PERCENT);
                    objectOutputStream.writeDouble(PERCENT_CHANGE_UP);
                    objectOutputStream.writeDouble(PERCENT_CHANGE_DOWN);
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