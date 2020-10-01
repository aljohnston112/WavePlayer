package com.example.waveplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityMain extends AppCompatActivity {

    private static final String TAG = "mainActivity";
    private static final int REQUEST_PERMISSION = 245083964;

    // Settings
    static double MAX_PERCENT = 0.1;
    static double PERCENT_CHANGE = 0.5;

    static final String MASTER_PLAYLIST_NAME = "xdlkrvnadiyfoghj";

    ArrayList<AudioURI> songs = new ArrayList<>();
    HashMap<Uri, MediaPlayerWURI> songsMap = new HashMap<>();
    ArrayList<RandomPlaylist> playlists = new ArrayList<>();

    // TODO make fragments communicate
    ArrayList<AudioURI> userPickedSongs = new ArrayList<>();
    //--------------------------------

    RandomPlaylist masterPlaylist;
    RandomPlaylist currentPlaylist;
    AudioURI currentSong;
    LinkedList<Uri> songQueue = new LinkedList<>();
    ListIterator<Uri> songQueueIterator;

    // For updating the SeekBar
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            addToQueueAndPlay(currentPlaylist.getProbFun().fun());
            updateUI();
        }
    };

    // region onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createUI();
        getExternalStoragePermissionAndFetchMediaFiles();
        songQueueIterator = songQueue.listIterator();
    }

    private void createUI() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorOnPrimary));
        setSupportActionBar(toolbar);
        centerActionBarTitle();
    }

    private void centerActionBarTitle() {
        ArrayList<View> textViews = new ArrayList<>();
        getWindow().getDecorView().findViewsWithText(textViews, getTitle(), View.FIND_VIEWS_WITH_TEXT);
        if (!textViews.isEmpty()) {
            AppCompatTextView appCompatTextView = null;
            for (View v : textViews) {
                if (v.getParent() instanceof Toolbar) {
                    appCompatTextView = (AppCompatTextView) v;
                    break;
                }
            }
            if (appCompatTextView != null) {
                ViewGroup.LayoutParams params = appCompatTextView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                appCompatTextView.setLayoutParams(params);
                appCompatTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
        }
    }

    private void getExternalStoragePermissionAndFetchMediaFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                getAudioFiles();
            }
        } else {
            getAudioFiles();
        }
    }

    private void getAudioFiles() {
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
                    Bitmap thumbnail = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        thumbnail = getApplicationContext().getContentResolver().loadThumbnail(
                                uri, new Size(640, 480), null);
                    } else {
                        final int THUMBSIZE = 128;
                        thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(data),
                                THUMBSIZE, THUMBSIZE);
                    }
                    AudioURI audioURI = new AudioURI(uri, thumbnail, displayName, artist, title);
                    songs.add(audioURI);
                }
                if (!songs.isEmpty()) {
                    masterPlaylist = new RandomPlaylist(songs, MAX_PERCENT, MASTER_PLAYLIST_NAME);
                    currentPlaylist = masterPlaylist;
                }
            }
        } catch (IOException e) {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.thumbnail_error, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    // endregion onCreate

    // region ActivityMain UI calls
    public void setActionBarTitle(String string) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(string);
    }

    public void setFabImage(Drawable drawable) {
        FloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setImageDrawable(drawable);
    }

    public void showFab(boolean show) {
        FloatingActionButton fab;
        fab = findViewById(R.id.fab);
        if (show) {
            fab.show();
        } else {
            fab.hide();
        }
    }

    public void setFabOnClickListener(View.OnClickListener onClickListener) {
        FloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(null);
        fab.setOnClickListener(onClickListener);
    }
    // endregion ActivityMain UI calls

    // region Music Controls
    void addToQueueAndPlay(AudioURI audioURI) {
        stopPreviousAndPrepare();
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(audioURI.uri);
        if (mediaPlayerWURI == null) {
            mediaPlayerWURI = makeMediaPlayerWURI(audioURI);
        }
        songQueueIterator = null;
        songQueue.add(mediaPlayerWURI.audioURI.uri);
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(mediaPlayerWURI.audioURI.uri));
        songQueueIterator.next();
        mediaPlayerWURI.start();
        currentSong = audioURI;
        updateUI();
    }

    private void stopPreviousAndPrepare() {
        if (songQueueIterator.hasPrevious()) {
            MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.stop();
                    mediaPlayerWURI.prepareAsync();
                    songQueueIterator.next();
                }
            }
        }
    }

    private MediaPlayerWURI makeMediaPlayerWURI(AudioURI audioURI) {
        MediaPlayerWURI mediaPlayerWURI = new CreateMediaPlayerWURIThread(this, audioURI).call();
        songsMap.put(mediaPlayerWURI.audioURI.uri, mediaPlayerWURI);
        while (!mediaPlayerWURI.isPrepared) {
        }
        mediaPlayerWURI.setOnCompletionListener(null);
        mediaPlayerWURI.setOnCompletionListener(onCompletionListener);
        return mediaPlayerWURI;
    }

    void playNext() {
        stopPreviousAndPrepare();
        if (songQueueIterator.hasNext()) {
            MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.next());
            if (mediaPlayerWURI != null) {
                currentSong = mediaPlayerWURI.audioURI;
            }
            while (!mediaPlayerWURI.isPrepared) {
            }
            mediaPlayerWURI.start();
            updateUI();
        } else {
            addToQueueAndPlay(currentPlaylist.getProbFun().fun());
        }
    }

    public void playPrevious() {
        stopPreviousAndPrepare();
        Uri uri = null;
        if (songQueueIterator.hasPrevious()) {
            uri = songQueueIterator.previous();
        }
        if (songQueueIterator.hasPrevious()) {
            MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
            if(mediaPlayerWURI!=null) {
                while (!mediaPlayerWURI.isPrepared) {
                }
                mediaPlayerWURI.start();
                currentSong = mediaPlayerWURI.audioURI;
            }
            songQueueIterator.next();
        } else {
            if(uri != null) {
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(uri);
                if(mediaPlayerWURI != null) {
                    while (!mediaPlayerWURI.isPrepared) {
                    }
                    mediaPlayerWURI.start();
                    currentSong = mediaPlayerWURI.audioURI;
                }
            }
        }
        updateUI();
    }

    public void pauseOrPlay() {
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.uri);
        if (mediaPlayerWURI != null) {
            if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                mediaPlayerWURI.pause();
            } else {
                while (!mediaPlayerWURI.isPrepared) {
                }
                mediaPlayerWURI.start();
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

    public class CreateMediaPlayerWURIThread implements Callable<MediaPlayerWURI> {

        ActivityMain activityMain;

        AudioURI audioURI;

        CreateMediaPlayerWURIThread(ActivityMain activityMain, AudioURI audioURI) {
            this.activityMain = activityMain;
            this.audioURI = audioURI;
        }

        @Override
        public MediaPlayerWURI call() {
            return new MediaPlayerWURI(activityMain, MediaPlayer.create(getApplicationContext(), audioURI.uri), audioURI);
        }

    }

    private void updateUI() {
        TextView textViewSongName = findViewById(R.id.text_view_song_name);
        if (textViewSongName != null) {
            textViewSongName.setText(currentSong.title);
        }
        // TODO move to onCreate when saving data is implemented
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(getApplicationContext(), currentSong.uri);
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int millis = Integer.parseInt(time);
        retriever.release();
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (seekBar != null) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(
                    new MRunnable(songsMap.get(currentSong.uri), seekBar),
                    0L, 1L, TimeUnit.SECONDS);
            seekBar.setMax(millis);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                    if (fromUser) {
                        songsMap.get(currentSong.uri).seekTo(i);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getAudioFiles();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.permission_needed, Toast.LENGTH_LONG);
            toast.show();
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
    }

    class MRunnable implements Runnable {

        MediaPlayerWURI mediaPlayerWURI;

        SeekBar seekBar;

        MRunnable(MediaPlayerWURI mediaPlayerWURI, SeekBar seekBar) {
            this.mediaPlayerWURI = mediaPlayerWURI;
            this.seekBar = seekBar;
        }

        @Override
        public void run() {
            int mCurrentPosition = mediaPlayerWURI.getCurrentPosition();
            seekBar.setProgress(mCurrentPosition);
        }
    }


}