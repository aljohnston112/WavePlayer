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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServiceMain extends Service {

    private static final String TAG = "mainService";
    private static final String FILE_ERROR_LOG = "error";
    private static final String FILE_SAVE = "playlists";
    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";
    public static final Random random = new Random();

    // Settings
    // TODO UPDATE ALL PLAYLISTS WITH MAX_PERCENT
    static double MAX_PERCENT = 0.1;
    static double PERCENT_CHANGE = 0.5;

    final ArrayList<AudioURI> songs = new ArrayList<>();
    final HashMap<Uri, MediaPlayerWURI> songsMap = new HashMap<>();
    final ArrayList<RandomPlaylist> playlists = new ArrayList<>();

    RandomPlaylist masterPlaylist;
    RandomPlaylist currentPlaylist;
    AudioURI currentSong;

    public AudioURI getCurrentSong() {
        return currentSong;
    }

    final LinkedList<Uri> songQueue = new LinkedList<>();
    ListIterator<Uri> songQueueIterator;

    private boolean isPlaying = false;

    public boolean isPlaying() {
        return isPlaying;
    }

    private boolean haveAudioFocus = false;

    // For updating the SeekBar
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    final MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            addToQueueAndPlay(currentPlaylist.getProbFun().fun(random));
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setAction("Complete");
            sendBroadcast(intent);
        }
    };

    private Looper serviceMainLooper;

    private ServiceMainHandler serviceMainHandler;

    private final IBinder serviceMainBinder = new ServiceMainBinder();

    private RemoteViews remoteViewNotificationLayout;

    NotificationCompat.Builder builder;

    Notification notification;

    String CHANNEL_ID = "PinkyPlayer";

    private final class ServiceMainHandler extends Handler {

        public ServiceMainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("ServiceMainStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        serviceMainLooper = thread.getLooper();
        serviceMainHandler = new ServiceMainHandler(serviceMainLooper);
        //setUpCrashSaver();
        //logLastCrash();
        loadSave();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceMainHandler.obtainMessage();
        msg.arg1 = startId;
        serviceMainHandler.sendMessage(msg);
        remoteViewNotificationLayout = new RemoteViews(getPackageName(), R.layout.notification_song_pane);
        Intent notificationIntent = new Intent(this, ServiceMain.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.music_note_black_48dp)
                .setContentTitle("Pinky Player")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteViewNotificationLayout)
                .setContentIntent(pendingIntent);
        if(currentSong != null) {
            remoteViewNotificationLayout.setTextViewText(R.id.textViewNotificationSongPaneSongName, currentSong.title);
        } else{
            remoteViewNotificationLayout.setTextViewText(R.id.textViewNotificationSongPaneSongName, "PinkyPlayer");
        }
        View view = remoteViewNotificationLayout.apply(this, null);
        remoteViewNotificationLayout.reapply(this, view);

        Intent intentNext = new Intent("Next");
        intentNext.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewNotificationLayout.setOnClickPendingIntent(R.id.imageButtonNotificationSongPaneNext, pendingIntentNext);

        Intent intentPlayPause = new Intent("PlayPause");
        intentPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(this, 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewNotificationLayout.setOnClickPendingIntent(R.id.imageButtonNotificationSongPanePlayPause, pendingIntentPlayPause);

        Intent intentPrev = new Intent("Previous");
        intentPrev.addCategory(Intent.CATEGORY_DEFAULT);
        PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(
                this, 0, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewNotificationLayout.setOnClickPendingIntent(R.id.imageButtonNotificationSongPanePrev, pendingIntentPrev);
        /*
        if (currentSong != null) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText((currentSong.title)));
        }
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "PinkyPlayer";
            String description = "Intelligent music player";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        notification = builder.build();
        startForeground(CHANNEL_ID.hashCode(), notification);
        return START_STICKY;
    }

    public void updateNotification(){
        if(currentSong != null) {
            remoteViewNotificationLayout.setTextViewText(R.id.textViewNotificationSongPaneSongName, currentSong.title);
        } else{
            remoteViewNotificationLayout.setTextViewText(R.id.textViewNotificationSongPaneSongName, "PinkyPlayer");
        }



        try {
            Field field = remoteViewNotificationLayout.getClass().getDeclaredField("mActions");
            field.setAccessible(true);
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(remoteViewNotificationLayout);
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);
                int tag = parcel.readInt();
                if (tag != 2) continue;
                // View ID
                parcel.readInt();
                String methodName = parcel.readString();
                if (methodName == null) continue;

                    // Save strings
                else if (methodName.equals("setMinimumHeight")) {
                    parcel.readInt();
                    int height = parcel.readInt();
                }
                parcel.recycle();
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }


        View view = remoteViewNotificationLayout.apply(this, null);
        /*
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        view = layoutInflater.inflate(R.layout.notification_song_pane, (ViewGroup) view.getRootView());
        ImageView imageView = view.findViewById(R.id.imageButtonNotificationSongPaneNext);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable = getResources().getDrawable(R.drawable.skip_next_black_24dp);
        drawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        Bitmap bitmapResized = FragmentSongPane.getResizedBitmap(bitmap, width, height);
        bitmap.recycle();
        // imageView.setImageBitmap(bitmapResized);
        remoteViewNotificationLayout.setBitmap(R.id.imageButtonNotificationSongPaneNext, "setImageBitmap", bitmapResized);
        view = remoteViewNotificationLayout.apply(this, null);

         */
        remoteViewNotificationLayout.reapply(this, view);
        builder.setContent(remoteViewNotificationLayout);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CHANNEL_ID.hashCode(), builder.build());
    }

    public class ServiceMainBinder extends Binder {
        ServiceMain getService() {
            return ServiceMain.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceMainBinder;
    }

    @Override
    public void onDestroy() {
        saveFile();
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    public void destroy(){
        if(isPlaying) {
            pauseOrPlay();
        }
        releaseMediaPlayers();
        stopSelf();
    }



    private void setUpCrashSaver() {
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
                } catch (IOException e) {
                }
            }
        });
    }

    private void logLastCrash() {
        File file = new File(getBaseContext().getFilesDir(), FILE_ERROR_LOG);
        if (file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                StringBuilder stringBuilder = new StringBuilder();
                String sCurrentLine;
                while ((sCurrentLine = bufferedReader.readLine()) != null) {
                    stringBuilder.append(sCurrentLine);
                }
                Log.e(TAG, stringBuilder.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadSave() {
        File file = new File(getBaseContext().getFilesDir(), FILE_SAVE);
        if (file.exists()) {
            try (FileInputStream fileInputStream = getApplicationContext().openFileInput("playlists");
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                MAX_PERCENT = objectInputStream.readDouble();
                PERCENT_CHANGE = objectInputStream.readDouble();
                masterPlaylist = (RandomPlaylist) objectInputStream.readObject();
                int playlistSize = objectInputStream.readInt();
                for (int i = 0; i < playlistSize; i++) {
                    playlists.add((RandomPlaylist) objectInputStream.readObject());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    void getAudioFiles() {
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Images.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC +
                " != ?";
        String[] selectionArgs = new String[]{
                "0"
        };
        String sortOrder = MediaStore.Video.Media.DISPLAY_NAME + " ASC";
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null) {
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
                    Bitmap thumbnail;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        thumbnail = getApplicationContext().getContentResolver().loadThumbnail(
                                uri, new Size(640, 480), null);
                    } else {
                        final int thumbNailWidthAndHeight = 128;
                        thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(data),
                                thumbNailWidthAndHeight, thumbNailWidthAndHeight);
                    }
                    AudioURI audioURI = new AudioURI(uri, thumbnail, displayName, artist, title, id);
                    songs.add(audioURI);
                }
                if (!songs.isEmpty()) {
                    // TODO load master playlist from file
                    masterPlaylist = new RandomPlaylist(songs, MAX_PERCENT, MASTER_PLAYLIST_NAME);
                    currentPlaylist = masterPlaylist;
                    songQueueIterator = songQueue.listIterator();
                }
            }
        } catch (IOException e) {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.thumbnail_error, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    // endregion onCreate

// region onStop

    private void saveFile() {
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


// endregion onStop

    // region ActivityMain UI calls



    // endregion ActivityMain UI calls

    // region Music Controls

    void addToQueueAndPlay(AudioURI audioURI) {
        Log.v(TAG, "addToQueueAndPlay");
        stopAndPreparePrevious();
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(audioURI.getUri());
        if (mediaPlayerWURI == null) {
            mediaPlayerWURI = makeMediaPlayerWURIAndPlay(audioURI);
        } else {
            if (haveAudioFocus) {
                mediaPlayerWURI.shouldStart(true);
                isPlaying = true;
            } else {
                if (requestAudioFocus()) {
                    mediaPlayerWURI.shouldStart(true);
                    isPlaying = true;
                }
            }
        }
        songQueueIterator = null;
        songQueue.add(mediaPlayerWURI.audioURI.getUri());
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(mediaPlayerWURI.audioURI.getUri()));
        songQueueIterator.next();
        currentSong = audioURI;
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

    void playNext() {
        Log.v(TAG, "playNext");
        if(currentSong != null) {
            currentPlaylist.getProbFun().bad(getCurrentSong(), PERCENT_CHANGE);
        }
        stopAndPreparePrevious();
        if (songQueueIterator.hasNext()) {
            MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.next());
            if (mediaPlayerWURI != null) {
                currentSong = mediaPlayerWURI.audioURI;
                if (haveAudioFocus) {
                    mediaPlayerWURI.shouldStart(true);
                    isPlaying = true;
                } else {
                    if (requestAudioFocus()) {
                        mediaPlayerWURI.shouldStart(true);
                        isPlaying = true;
                    }
                }
            }
        } else {
            addToQueueAndPlay(currentPlaylist.getProbFun().fun(random));
        }
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious");
        stopAndPreparePrevious();
        Uri uri = null;
        if (songQueueIterator.hasPrevious()) {
            uri = songQueueIterator.previous();
        }
        if (songQueueIterator.hasPrevious()) {
            MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
            if (mediaPlayerWURI != null) {
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
            songQueueIterator.next();
        } else {
            if (uri != null) {
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(uri);
                if (mediaPlayerWURI != null) {
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
                songQueueIterator.next();
            }
        }
    }

    public void pauseOrPlay() {
        Log.v(TAG, "pauseOrPlay");
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
        if (mediaPlayerWURI != null) {
            if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                mediaPlayerWURI.pause();
                isPlaying = false;
            } else {
                if (haveAudioFocus) {
                    mediaPlayerWURI.shouldStart(true);
                    isPlaying = true;
                } else {
                    if (requestAudioFocus()) {
                        mediaPlayerWURI.shouldStart(true);
                        isPlaying = true;
                    }
                }
            }
        }
    }

    // endregion Music Controls

    public void releaseMediaPlayers() {
        synchronized (this) {
            Iterator<MediaPlayerWURI> collection = songsMap.values().iterator();
            MediaPlayerWURI mediaPlayerWURI;
            while (collection.hasNext()) {
                mediaPlayerWURI = collection.next();
                mediaPlayerWURI.release();
                collection.remove();
            }
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

}
