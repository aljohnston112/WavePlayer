package com.example.waveplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Log;
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
import androidx.loader.content.CursorLoader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityMain extends AppCompatActivity {

    public static final String TAG = "mainActivity";

    private static final int REQUEST_PERMISSION = 245083964;

    ActionBar actionBar;
    FloatingActionButton fab;

    ArrayList<AudioURI> songs = new ArrayList<>();
    HashMap<Uri, MediaPlayerWURI> songsMap = new HashMap<>();
    ArrayList<RandomPlaylist> playlists = new ArrayList<>();
    ArrayList<AudioURI> songsToHighlight = new ArrayList<>();
    ArrayList<AudioURI> userPickedSongs = new ArrayList<>();
    RandomPlaylist masterPlaylist;
    RandomPlaylist currentPlaylist = masterPlaylist;
    AudioURI currentSong;
    LinkedList<Uri> songQueue = new LinkedList<>();
    ListIterator<Uri> songQueueIterator;

    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    MOnCompletionListener mOnCompletionListener;

    static double MAX_PERCENT = 0.1;
    static double PERCENT_CHANGE = 0.5;

    static final String MASTER_PLAYLIST_NAME = "xdlkrvnadiyfoghj";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorOnPrimary));
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        centerActionBarTitle();
        fab = findViewById(R.id.fab);
        songQueueIterator = songQueue.listIterator();
        mOnCompletionListener = new MOnCompletionListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                try {
                    getAudioFiles();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                getAudioFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void centerActionBarTitle() {
        ArrayList<View> textViews = new ArrayList<>();
        getWindow().getDecorView().findViewsWithText(textViews, getTitle(), View.FIND_VIEWS_WITH_TEXT);
        if (textViews.size() > 0) {
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

    void playNext() {
        synchronized (this) {
            if (songQueueIterator.hasNext()) {
                if (songQueueIterator.hasPrevious()) {
                    MediaPlayerWURI mediaPlayer1 = songsMap.get(songQueueIterator.previous());
                    if (mediaPlayer1.isPrepared && mediaPlayer1.isPlaying()) {
                        mediaPlayer1.stop();
                        mediaPlayer1.prepareAsync();
                    }
                    songQueueIterator.next();
                }
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.next());
                currentSong = mediaPlayerWURI.audioURI;
                while (!mediaPlayerWURI.isPrepared) {
                }
                mediaPlayerWURI.start();
                updateUI();
            } else {
                play(currentPlaylist.getProbFun().fun());
            }
        }
    }

    public void playPrevious() {
        if (songQueueIterator.hasPrevious()) {
            MediaPlayerWURI mediaPlayer = songsMap.get(songQueueIterator.previous());
            if(songQueueIterator.hasPrevious()) {
                mediaPlayer.stop();
                mediaPlayer.prepareAsync();
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
                while(!mediaPlayerWURI.isPrepared){}
                mediaPlayerWURI.start();
                currentSong = mediaPlayerWURI.audioURI;
                songQueueIterator.next();
            } else {
                mediaPlayer.seekTo(0);
                songQueueIterator.next();
            }
        }
        updateUI();
    }

    public void pauseOrPlay() {
        MediaPlayerWURI mediaPlayer = songsMap.get(currentSong.uri);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            while(!mediaPlayer.isPrepared){}
            mediaPlayer.start();
        }
    }


    void play(AudioURI audioURI) {
        if(audioURI.uri.equals(currentSong.uri) && songsMap.get(currentSong.uri) != null &&
                songsMap.get(currentSong.uri).isPlaying()){
            songsMap.get(currentSong.uri).seekTo(0);
        } else {
            if (songQueue.size() > 0) {
                Log.v(TAG, "Stopping previous");
                if (songQueueIterator.hasPrevious()) {
                    MediaPlayerWURI mediaPlayer1 = songsMap.get(songQueueIterator.previous());
                    if (mediaPlayer1 != null) {
                        while (!mediaPlayer1.isPrepared) {
                        }
                        if (mediaPlayer1.isPlaying()) {
                            mediaPlayer1.stop();
                            mediaPlayer1.prepareAsync();
                            songQueueIterator.next();
                        }
                    }
                }
                Log.v(TAG, "Stopped previous");
            }
            MediaPlayerWURI mediaPlayerWURI = songsMap.get(audioURI.uri);
            if (mediaPlayerWURI == null) {
                Log.v(TAG, "Creating MediaPlayer");
                mediaPlayerWURI = new NextMediaPlayer(this, audioURI).call();
                songsMap.put(mediaPlayerWURI.audioURI.uri, mediaPlayerWURI);
                Log.v(TAG, "MediaPlayer created");
            }
            Log.v(TAG, "Waiting for preparation");
            while (!mediaPlayerWURI.isPrepared) {
            }
            Log.v(TAG, "Prepared");
            mediaPlayerWURI.setOnCompletionListener(mOnCompletionListener);
            songQueueIterator = null;
            songQueue.add(mediaPlayerWURI.audioURI.uri);
            songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(mediaPlayerWURI.audioURI.uri));
            MediaPlayerWURI mediaPlayer = songsMap.get(songQueueIterator.next());
            mediaPlayer.start();
            currentSong = mediaPlayerWURI.audioURI;
        }
            updateUI();
    }

    public void releaseMediaPlayers() {
        synchronized (this) {
            Iterator<MediaPlayerWURI> collection = songsMap.values().iterator();
            while(collection.hasNext()) {
                MediaPlayerWURI m = collection.next();
                m.release();
                collection.remove();
            }
        }
    }

    public class NextMediaPlayer implements Callable<MediaPlayerWURI> {

        ActivityMain activityMain;

        AudioURI audioURI;

        NextMediaPlayer(ActivityMain activityMain, AudioURI audioURI) {
            this.activityMain = activityMain;
            this.audioURI = audioURI;
        }

        @Override
        public MediaPlayerWURI call() {
            return new MediaPlayerWURI(activityMain, MediaPlayer.create(getApplicationContext(), audioURI.uri), audioURI);
        }

    }

    public static class MOnCompletionListener implements MediaPlayer.OnCompletionListener {

        ActivityMain activityMain;

        MOnCompletionListener(ActivityMain activityMain) {
            this.activityMain = activityMain;
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            activityMain.scheduledExecutorService.shutdown();
            activityMain.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            AudioURI uri = activityMain.currentPlaylist.getProbFun().fun();
            activityMain.play(uri);
            activityMain.updateUI();
        }
    }

    private void updateUI() {
        SeekBar seekBar = findViewById(R.id.seekBar);
        scheduledExecutorService.shutdown();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(
                new MRunnable(songsMap.get(currentSong.uri), seekBar),
                0L,  1L, TimeUnit.SECONDS);
        TextView textViewSongName = findViewById(R.id.text_view_song_name);
        textViewSongName.setText(currentSong.title);
        int millis = currentSong.duration;
        seekBar.setMax(millis);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if(fromUser){
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
        String endTime = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        TextView textViewCurrent = findViewById(R.id.editTextCurrentTime);
        textViewCurrent.setText("00:00:00");
        TextView textViewEnd = findViewById(R.id.editTextEndTime);
        textViewEnd.setText(endTime);
    }

    private void getAudioFiles() throws IOException {
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
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
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    long id = cursor.getLong(idCol);
                    String track = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String duration = cursor.getString(durationCol);
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    Bitmap thumbnail = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        thumbnail = getApplicationContext().getContentResolver().loadThumbnail(
                                contentUri, new Size(640, 480), null);
                    } else{
                        final int THUMBSIZE = 128;
                        thumbnail= ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(getRealPathFromURI(contentUri)),
                                THUMBSIZE, THUMBSIZE);
                    }
                    AudioURI audioURI = new AudioURI(contentUri, thumbnail, artist, track, Integer.parseInt(duration));
                    songs.add(audioURI);
                }
                if(songs.size() != 0){
                    masterPlaylist = new RandomPlaylist(songs, MAX_PERCENT, MASTER_PLAYLIST_NAME);
                    currentPlaylist = masterPlaylist;
                }
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(
                getBaseContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                getAudioFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast toast = Toast.makeText(getBaseContext(), R.string.permission_needed, Toast.LENGTH_LONG);
            toast.show();
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
    }

    class MRunnable implements Runnable {

        MediaPlayerWURI mediaPlayerWURI;

        SeekBar seekBar;

        MRunnable(MediaPlayerWURI mediaPlayerWURI, SeekBar seekBar){
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