package com.example.waveplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServiceMain extends Service {

    private static final String TAG = "mainService";
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

    final HashMap<Uri, MediaPlayerWURI> songsMap = new HashMap<>();
    final public HashMap<Uri, AudioURI> uriMap = new HashMap<>();

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


    // For updating the SeekBar
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public final MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            if (!songQueueIterator.hasNext()) {
                addToQueueAndPlay(currentPlaylist.getProbFun().fun(random));
            } else {
                stopAndPreparePrevious();
                playNextInQueue();
            }
            updateNotification();
            sendBroadcastOnCompletion();
        }

        private void sendBroadcastOnCompletion() {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setAction(getResources().getString(R.string.broadcast_receiver_action_on_completion));
            sendBroadcast(intent);
        }

    };

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
        if (!loaded) {
           // setUpExceptionSaver();
           // logLastThrownException();
            loadSaveFile();
        }
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
            }
        }
        loaded = true;
    }

    // endregion onCreate

    // region onStartCommand

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!serviceStarted) {
            Toast.makeText(getApplicationContext(), "PinkyPlayer starting", Toast.LENGTH_SHORT).show();
            setUpNotificationBuilder();
            setUpBroadCasts();
            notification = builder.build();
            startForeground(CHANNEL_ID.hashCode(), notification);
        }
        serviceStarted = true;
        return START_STICKY;
    }

    private void setUpNotificationBuilder() {
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
    }

    public void updateNotification() {
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
    }

    private void setUpBroadCasts() {
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
        Toast.makeText(this, "PinkyPlayer done", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        destroy();
    }

    public void destroy() {
        if (isPlaying) {
            pauseOrPlay();
        }
        releaseMediaPlayers();
        stopSelf();
    }

    public void releaseMediaPlayers() {
        synchronized (this) {
            Iterator<MediaPlayerWURI> iterator = songsMap.values().iterator();
            MediaPlayerWURI mediaPlayerWURI;
            while (iterator.hasNext()) {
                mediaPlayerWURI = iterator.next();
                mediaPlayerWURI.release();
                iterator.remove();
            }
        }
    }

    void getAudioFiles() {
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE, MediaStore.Images.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ?";
        String[] selectionArgs = new String[]{"0"};
        String sortOrder = MediaStore.Video.Media.DISPLAY_NAME + " ASC";
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                List<Uri> newURIs = getURIs(cursor);
                if (!uriMap.isEmpty() && loaded) {
                    if (masterPlaylist != null) {
                        // Add new songs to the masterPlaylist
                        AudioURI audioURIFromURIMap;
                        for (int i = 0; i < uriMap.size(); i++) {
                            audioURIFromURIMap = uriMap.get(i);
                            if(audioURIFromURIMap != null) {
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
                                (List<AudioURI>) uriMap.values(), MAX_PERCENT, MASTER_PLAYLIST_NAME);
                    }
                    currentPlaylist = masterPlaylist;
                    songQueueIterator = songQueue.listIterator();
                }
            }
        }
        audioFilesLoaded = true;
    }

    private List<Uri> getURIs(Cursor cursor) {
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
            if(!uriMap.containsKey(uri)) {
                AudioURI audioURI = new AudioURI(uri, data, displayName, artist, title, id);
                uriMap.put(uri, audioURI);
            }
            newURIs.add(uri);
        }
        return newURIs;
    }

    // region mediaControls

    public void pauseOrPlay() {
        Log.v(TAG, "pauseOrPlay");
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
        if (mediaPlayerWURI != null) {
            if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                mediaPlayerWURI.pause();
                isPlaying = false;
            } else {
                play(mediaPlayerWURI);
            }
        }
    }

    void playNext() {
        Log.v(TAG, "playNext");
        if (currentSong != null) {
            currentPlaylist.getProbFun().bad(getCurrentSong(), PERCENT_CHANGE);
        }
        stopAndPreparePrevious();
        if (songQueueIterator.hasNext()) {
            playNextInQueue();
        } else {
            addToQueueAndPlay(currentPlaylist.getProbFun().fun(random));
        }
        saveFile();
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious");
        stopAndPreparePrevious();
        Uri uri = null;
        if (songQueueIterator.hasPrevious()) {
            uri = songQueueIterator.previous();
        }
        if (songQueueIterator.hasPrevious()) {
            playPrevousInQueue();
        } else {
            if (uri != null) {
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(uri);
                if (mediaPlayerWURI != null) {
                    play(mediaPlayerWURI);
                }
                songQueueIterator.next();
            }
        }
    }

    void addToQueueAndPlay(AudioURI audioURI) {
        Log.v(TAG, "addToQueueAndPlay");
        stopAndPreparePrevious();
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(audioURI.getUri());
        if (mediaPlayerWURI == null) {
            mediaPlayerWURI = makeMediaPlayerWURIAndPlay(audioURI);
        } else {
            play(mediaPlayerWURI);
        }
        songQueueIterator = null;
        songQueue.add(mediaPlayerWURI.audioURI.getUri());
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(mediaPlayerWURI.audioURI.getUri()));
        songQueueIterator.next();
    }

    private void stopAndPreparePrevious() {
        Log.v(TAG, "stopPreviousAndPrepare");
        if (songQueueIterator.hasPrevious()) {
            final MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.stop();
                    mediaPlayerWURI.prepareAsync();
                    isPlaying = false;
                }
            }
            songQueueIterator.next();
        }
    }

    private MediaPlayerWURI makeMediaPlayerWURIAndPlay(AudioURI audioURI) {
        Log.v(TAG, "makeMediaPlayerWURI");
        MediaPlayerWURI mediaPlayerWURI = new CreateMediaPlayerWURIThread(this, audioURI).call();
        songsMap.put(mediaPlayerWURI.audioURI.getUri(), mediaPlayerWURI);
        play(mediaPlayerWURI);
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
            MediaPlayerWURI mediaPlayerWURI = new MediaPlayerWURI(
                    serviceMain, MediaPlayer.create(getApplicationContext(), audioURI.getUri()), audioURI);
            mediaPlayerWURI.setOnCompletionListener(null);
            mediaPlayerWURI.setOnCompletionListener(onCompletionListener);
            return mediaPlayerWURI;
        }

    }

    private boolean requestAudioFocus() {
        if (haveAudioFocus) {
            return true;
        }
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int i) {

            }
        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            haveAudioFocus = true;
        } else {
            haveAudioFocus = false;
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void playNextInQueue() {
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.next());
        if (mediaPlayerWURI != null) {
            play(mediaPlayerWURI);
        }
    }

    private void playPrevousInQueue(){
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
        if (mediaPlayerWURI != null) {
            play(mediaPlayerWURI);
        }
        songQueueIterator.next();
    }

    private void play(MediaPlayerWURI mediaPlayerWURI) {
        if (haveAudioFocus) {
            mediaPlayerWURI.shouldStart(true);
            isPlaying = true;
        } else {
            if (requestAudioFocus()) {
                mediaPlayerWURI.shouldStart(true);
                isPlaying = true;
            }
        }
        currentSong = mediaPlayerWURI.audioURI;
    }

    // endregion mediaControls

    void saveFile() {
        File file = new File(getBaseContext().getFilesDir(), FILE_SAVE);
        //noinspection ResultOfMethodCallIgnored
        file.delete();
        try (FileOutputStream fos = getApplicationContext().openFileOutput("playlists", Context.MODE_PRIVATE);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos)) {
            objectOutputStream.writeDouble(MAX_PERCENT);
            objectOutputStream.writeDouble(PERCENT_CHANGE);
            objectOutputStream.writeObject(masterPlaylist);
            objectOutputStream.writeInt(playlists.size());
            for (RandomPlaylist randomPlaylist : playlists) {
                objectOutputStream.writeObject(randomPlaylist);
            }
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
