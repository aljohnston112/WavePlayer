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
import android.view.View;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServiceMain extends Service {

    private static final String TAG = "ServiceMain";
    private static final String FILE_ERROR_LOG = "error";
    private static final String FILE_SAVE = "playlists";
    private final static String CHANNEL_ID = "PinkyPlayer";
    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";

    public static final Random random = new Random();

    // TODO make fragments communicate
    final ArrayList<AudioURI> userPickedSongs = new ArrayList<>();
    RandomPlaylist userPickedPlaylist;

    // Settings
    static double MAX_PERCENT = 0.1;
    static double PERCENT_CHANGE = 0.5;
    // TODO add percent change up and down

    final HashMap<Uri, MediaPlayerWURI> songsMap = new HashMap<>();
    final public LinkedHashMap<Uri, AudioURI> uriMap = new LinkedHashMap<>();

    final ArrayList<RandomPlaylist> playlists = new ArrayList<>();
    RandomPlaylist masterPlaylist;
    RandomPlaylist currentPlaylist;

    final LinkedList<Uri> songQueue = new LinkedList<>();
    ListIterator<Uri> songQueueIterator;

    AudioURI currentSong;

    public AudioURI getCurrentSong() {
        return currentSong;
    }

    private boolean isPlaying = false;

    public boolean isPlaying() {
        return isPlaying;
    }

    private boolean songInProgress = false;

    public boolean songInProgress() {
        return songInProgress;
    }

    boolean fragmentSongVisible = false;

    boolean shuffling = true;

    boolean looping = false;

    boolean loopingOne = false;

    ArrayList<AudioURI> currentPlaylistArray;
    ListIterator<AudioURI> currentPlaylistIterator;

    // For updating the SeekBar
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    BroadcastReceiverNotificationForServiceMainMediaControls broadcastReceiverNotificationForServiceMainMediaControlsButtons = new BroadcastReceiverNotificationForServiceMainMediaControls(this);

    public final MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            String string = "Media player: " +
                    mediaPlayer.hashCode() +
                    " onCompletion started";
            Log.v(TAG, string);
            if (currentSong != null) {
                currentPlaylist.getProbFun().bad(getCurrentSong(), PERCENT_CHANGE);
                if (loopingOne) {
                    MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
                    if (mediaPlayerWURI != null) {
                        mediaPlayerWURI.seekTo(0);
                        mediaPlayerWURI.shouldStart(true);
                    }
                    saveFile();
                    return;
                }
            }
            if((!shuffling && !currentPlaylistIterator.hasNext() && !looping)) {
                scheduledExecutorService.shutdown();
                if (fragmentSongVisible) {
                    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                }
            }
            stopAndPreparePrevious();
            if (!shuffling) {
                if (currentPlaylistIterator.hasNext()) {
                    addToQueueAndPlay(currentPlaylistIterator.next().getUri());
                } else if (looping) {
                    currentPlaylistIterator = currentPlaylistArray.listIterator();
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
            sendBroadcastOnCompletion();
            string = "Media player: " +
                    mediaPlayer.hashCode() +
                    " onCompletion ended";
            Log.v(TAG, string);
        }

        private void sendBroadcastOnCompletion() {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setAction(getResources().getString(R.string.broadcast_receiver_action_on_completion));
            String string = "onCompletion broadcast started";
            Log.v(TAG, string);
            sendBroadcast(intent);
            string = "onCompletion broadcast ended";
            Log.v(TAG, string);
        }

    };

    private void addToQueueAndPlay(Uri uri) {
        stopAndPreparePrevious();
        play(uri);
        songQueueIterator = null;
        songQueue.add(uri);
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(uri));
        songQueueIterator.next();
    }

    private void play(Uri uri) {
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(uri);
        if (mediaPlayerWURI != null) {
            play(mediaPlayerWURI);
        } else {
            makeMediaPlayerWURIAndPlay(uriMap.get(uri));
        }
    }

    private RemoteViews remoteViewNotificationLayout;
    NotificationCompat.Builder builder;
    Notification notification;

    int songPaneArtHeight;

    private boolean serviceStarted = false;
    private boolean haveAudioFocus = false;
    private boolean loaded = false;
    public boolean audioFilesLoaded = false;

    private final IBinder serviceMainBinder = new ServiceMainBinder();

    // region onCreate

    @Override
    public void onCreate() {
        super.onCreate();
        String string = "onCreate started";
        Log.v(TAG, string);
        if (!loaded) {
            string = "onCreate is loading";
            Log.v(TAG, string);
            setUpBroadCastReceivers();
            // setUpExceptionSaver();
            // logLastThrownException();
            loadSaveFile();
            string = "onCreate done loading";
            Log.v(TAG, string);
        } else {
            string = "onCreate already loaded";
            Log.v(TAG, string);
        }
        string = "onCreate ended";
        Log.v(TAG, string);
    }

    private void setUpBroadCastReceivers() {
        String string = "Setting up Broadcast receivers for notification buttons";
        Log.v(TAG, string);
        IntentFilter filterNext = new IntentFilter();
        filterNext.addAction(getResources().getString(R.string.broadcast_receiver_action_next));
        filterNext.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationForServiceMainMediaControlsButtons, filterNext);

        IntentFilter filterPrevious = new IntentFilter();
        filterPrevious.addAction(getResources().getString(R.string.broadcast_receiver_action_previous));
        filterPrevious.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationForServiceMainMediaControlsButtons, filterPrevious);

        IntentFilter filterPlayPause = new IntentFilter();
        filterPlayPause.addAction(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        filterPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationForServiceMainMediaControlsButtons, filterPlayPause);
        string = "Done setting up Broadcast receivers for notification buttons";
        Log.v(TAG, string);
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
        String string = "Loading the save file";
        Log.v(TAG, string);
        File file = new File(getBaseContext().getFilesDir(), FILE_SAVE);
        if (file.exists()) {
            try (FileInputStream fileInputStream = getApplicationContext().openFileInput(FILE_SAVE);
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                MAX_PERCENT = objectInputStream.readDouble();
                PERCENT_CHANGE = objectInputStream.readDouble();
                masterPlaylist = (RandomPlaylist) objectInputStream.readObject();
                int playlistSize = objectInputStream.readInt();
                for (int i = 0; i < playlistSize; i++) {
                    playlists.add((RandomPlaylist) objectInputStream.readObject());
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                string = "Error encountered while loading the save file";
                Log.v(TAG, string);
            }
        }
        loaded = true;
        string = "Save file loaded";
        Log.v(TAG, string);
    }

    // endregion onCreate

    // region onStartCommand

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String string = "onStartCommand started";
        Log.v(TAG, string);
        if (!serviceStarted) {
            string = "onStartCommand setting up service";
            Log.v(TAG, string);
            Toast.makeText(getApplicationContext(), "PinkyPlayer starting", Toast.LENGTH_SHORT).show();
            setUpNotificationBuilder();
            notification = builder.build();
            startForeground(CHANNEL_ID.hashCode(), notification);
            setUpBroadCasts();
            string = "onStartCommand done setting up service";
            Log.v(TAG, string);
        } else {
            string = "onStartCommandalready set up service";
            Log.v(TAG, string);
        }
        serviceStarted = true;
        string = "onStartCommand ended";
        Log.v(TAG, string);
        return START_STICKY;
    }

    private void setUpNotificationBuilder() {
        String string = "Setting up notification builder";
        Log.v(TAG, string);
        builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.music_note_black_48dp)
                .setContentTitle(CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        remoteViewNotificationLayout = new RemoteViews(getPackageName(), R.layout.notification_song_pane);
        if (currentSong != null) {
            remoteViewNotificationLayout.setTextViewText(R.id.textViewNotificationSongPaneSongName, currentSong.title);
        } else {
            remoteViewNotificationLayout.setTextViewText(R.id.textViewNotificationSongPaneSongName, CHANNEL_ID);
        }
        View view = remoteViewNotificationLayout.apply(getApplicationContext(), null);
        remoteViewNotificationLayout.reapply(getApplicationContext(), view);
        builder.setCustomContentView(remoteViewNotificationLayout);
        Intent notificationIntent = new Intent(getApplicationContext(), ActivityMain.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        builder.setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
            String description = "Intelligent music player";
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        string = "Done setting up notification builder";
        Log.v(TAG, string);
    }

    public void updateNotification() {
        String string = "Updating the notification";
        Log.v(TAG, string);
        if (currentSong != null) {
            remoteViewNotificationLayout.setTextViewText(R.id.textViewNotificationSongPaneSongName, currentSong.title);
        } else {
            remoteViewNotificationLayout.setTextViewText(R.id.textViewNotificationSongPaneSongName, CHANNEL_ID);
        }
        View view = remoteViewNotificationLayout.apply(getApplicationContext(), null);
        remoteViewNotificationLayout.reapply(getApplicationContext(), view);
        builder.setContent(remoteViewNotificationLayout);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CHANNEL_ID.hashCode(), builder.build());
        string = "Done updating the notification";
        Log.v(TAG, string);
    }

    void updateNotificationPlayButton() {
        // TODO
    }

    private void setUpBroadCasts() {
        String string = "Setting up broadcasts";
        Log.v(TAG, string);
        Intent intentNext = new Intent(getResources().getString(R.string.broadcast_receiver_action_next));
        intentNext.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentNext = PendingIntent.getBroadcast(
                getApplicationContext(), 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewNotificationLayout.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPaneNext, pendingIntentNext);

        Intent intentPlayPause = new Intent(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(
                getApplicationContext(), 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewNotificationLayout.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePlayPause, pendingIntentPlayPause);

        Intent intentPrev = new Intent(getResources().getString(R.string.broadcast_receiver_action_previous));
        intentPrev.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(
                getApplicationContext(), 0, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewNotificationLayout.setOnClickPendingIntent(
                R.id.imageButtonNotificationSongPanePrev, pendingIntentPrev);
        string = "Done setting up broadcasts";
        Log.v(TAG, string);
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
        unregisterReceiver(broadcastReceiverNotificationForServiceMainMediaControlsButtons);
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
        stopSelf();
        Log.v(TAG, "destroy ended");
    }

    public void releaseMediaPlayers() {
        String string = "Releasing MediaPlayers";
        Log.v(TAG, string);
        synchronized (this) {
            Iterator<MediaPlayerWURI> iterator = songsMap.values().iterator();
            MediaPlayerWURI mediaPlayerWURI;
            while (iterator.hasNext()) {
                mediaPlayerWURI = iterator.next();
                mediaPlayerWURI.release();
                iterator.remove();
            }
        }
        string = "Done releasing MediaPlayers";
        Log.v(TAG, string);
    }

    void getAudioFiles() {
        String string = "Getting audio files";
        Log.v(TAG, string);
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE, MediaStore.Images.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ?";
        String[] selectionArgs = new String[]{"0"};
        String sortOrder = MediaStore.Video.Media.TITLE + " ASC";
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                List<Uri> newURIs = getURIs(cursor);
                if (!uriMap.isEmpty() && loaded) {
                    if (masterPlaylist != null) {
                        // Add new songs to the masterPlaylist
                        for (AudioURI audioURIFromURIMap : uriMap.values()) {
                            if (audioURIFromURIMap != null) {
                                if (!masterPlaylist.getProbFun().getProbMap().containsKey(audioURIFromURIMap)) {
                                    masterPlaylist.getProbFun().add(audioURIFromURIMap);
                                }
                            }
                        }
                        // Remove missing songs
                        for (AudioURI audioURI : masterPlaylist.getProbFun().getProbMap().keySet()) {
                            if (!newURIs.contains(audioURI.getUri())) {
                                masterPlaylist.getProbFun().remove(audioURI);
                                uriMap.remove(audioURI.getUri());
                                songsMap.remove(audioURI.getUri());
                            }
                        }
                    } else {
                        masterPlaylist = new RandomPlaylist(
                                new ArrayList<>(uriMap.values()), MAX_PERCENT, MASTER_PLAYLIST_NAME);
                    }
                    currentPlaylist = masterPlaylist;
                    songQueueIterator = songQueue.listIterator();
                }
            }
        }
        audioFilesLoaded = true;
        string = "Done getting audio files";
        Log.v(TAG, string);
    }

    private List<Uri> getURIs(Cursor cursor) {
        String string = "Getting uris";
        Log.v(TAG, string);
        ArrayList<Uri> newURIs = new ArrayList<>();
        int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(idCol);
            String displayName = cursor.getString(nameCol);
            String title = cursor.getString(titleCol);
            String artist = cursor.getString(artistCol);
            String data = cursor.getString(dataCol);
            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            if (!uriMap.containsKey(uri)) {
                AudioURI audioURI = new AudioURI(uri, data, displayName, artist, title, id);
                uriMap.put(uri, audioURI);
            }
            newURIs.add(uri);
        }
        string = "Done getting uris";
        Log.v(TAG, string);
        return newURIs;
    }

    // region mediaControls

    public void clearQueue() {
        songQueueIterator = null;
        songQueue.clear();
        songQueueIterator = songQueue.listIterator();
    }

    public void pauseOrPlay() {
        Log.v(TAG, "pauseOrPlay started");
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
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
        Log.v(TAG, "pauseOrPlay ended");
    }

    void playNext() {
        Log.v(TAG, "playNext started");
        if (currentSong != null) {
            currentPlaylist.getProbFun().bad(getCurrentSong(), PERCENT_CHANGE);
            if (loopingOne) {
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
                if (mediaPlayerWURI != null) {
                    mediaPlayerWURI.seekTo(0);
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
                currentPlaylistIterator = currentPlaylistArray.listIterator();
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
                Uri uri = songQueueIterator.next();
                play(uri);
            } else {
                addToQueueAndPlay(currentPlaylist.getProbFun().fun(random));
            }
        }
        saveFile();
        Log.v(TAG, "playNext ended");
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious started");
        if (currentSong != null) {
            if (loopingOne) {
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
                if (mediaPlayerWURI != null) {
                    mediaPlayerWURI.seekTo(0);
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
                } else if (looping) {
                    currentPlaylistIterator = currentPlaylistArray.listIterator(currentPlaylistArray.size() - 1);
                    addToQueueAndPlay(currentPlaylistIterator.next().getUri());
                    return;
                }
            } else {
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
                if (mediaPlayerWURI != null) {
                    mediaPlayerWURI.seekTo(0);
                }
            }
            currentPlaylistIterator.next();
            return;
        }
        Uri uri = null;
        if (songQueueIterator.hasPrevious()) {
            uri = songQueueIterator.previous();
            if (songQueueIterator.hasPrevious()) {
                playPrevousInQueue();
                return;
            } else if (looping) {
                songQueueIterator = songQueue.listIterator(songQueue.size() - 1);
                songQueueIterator.next();
                play(songQueueIterator.previous());
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
        songQueueIterator = null;
        songQueue.add(audioURI.getUri());
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(audioURI.getUri()));
        songQueueIterator.next();
        Log.v(TAG, "addToQueueAndPlay ended");
    }

    void stopAndPreparePrevious() {
        Log.v(TAG, "stopPreviousAndPrepare started");
        if (songQueueIterator.hasPrevious()) {
            final MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
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
            songQueueIterator.next();
        }
        Log.v(TAG, "stopPreviousAndPrepare ended");
    }

    private MediaPlayerWURI makeMediaPlayerWURIAndPlay(AudioURI audioURI) {
        Log.v(TAG, "makeMediaPlayerWURIAndPlay started");
        MediaPlayerWURI mediaPlayerWURI = new CreateMediaPlayerWURIThread(this, audioURI).call();
        songsMap.put(mediaPlayerWURI.audioURI.getUri(), mediaPlayerWURI);
        play(mediaPlayerWURI);
        Log.v(TAG, "makeMediaPlayerWURIAndPlay ended");
        return mediaPlayerWURI;
    }

    public class CreateMediaPlayerWURIThread implements Callable<MediaPlayerWURI> {

        final ServiceMain serviceMain;

        final AudioURI audioURI;

        CreateMediaPlayerWURIThread(ServiceMain serviceMain, AudioURI audioURI) {
            this.serviceMain = serviceMain;
            this.audioURI = audioURI;
        }

        @Override
        public MediaPlayerWURI call() {
            Log.v(TAG, "makeMediaPlayerWURI being made");
            MediaPlayerWURI mediaPlayerWURI = new MediaPlayerWURI(
                    serviceMain, MediaPlayer.create(getApplicationContext(), audioURI.getUri()), audioURI);
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

    private void playNextInQueue() {
        Log.v(TAG, "playNextInQueue started");
        Uri uri = songQueueIterator.next();
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(uri);
        if (mediaPlayerWURI != null) {
            play(mediaPlayerWURI);
        } else {
            makeMediaPlayerWURIAndPlay(uriMap.get(uri));
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
        updateNotification();
        Log.v(TAG, "play ended");
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
                /*
                try (FileOutputStream fos = getApplicationContext().openFileOutput("playlists", Context.MODE_PRIVATE);
                     ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos)) {
                    Log.v(TAG, "Creating save file");
                    objectOutputStream.writeDouble(MAX_PERCENT);
                    objectOutputStream.writeDouble(PERCENT_CHANGE);
                    objectOutputStream.writeObject(masterPlaylist);
                    objectOutputStream.writeInt(playlists.size());
                    for (RandomPlaylist randomPlaylist : playlists) {
                        objectOutputStream.writeObject(randomPlaylist);
                    }
                    objectOutputStream.flush();
                    Log.v(TAG, "Save file created");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.v(TAG, "Problem while trying to save file");
                }

                 */
                Log.v(TAG, "saveFile ended");
            }
        });
    }

}
