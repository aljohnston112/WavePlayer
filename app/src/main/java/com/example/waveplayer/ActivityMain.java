package com.example.waveplayer;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ActivityMain extends AppCompatActivity {

    ActionBar actionBar;
    FloatingActionButton fab;

    ArrayList<AudioURI> songs = new ArrayList<>();
    HashMap<Uri, MediaPlayerWURI> songsMap = new HashMap<>();
    ArrayList<RandomPlaylist> playlists = new ArrayList<>();
    ArrayList<AudioURI> songsToHighlight = new ArrayList<>();
    ArrayList<AudioURI> userPickedSongs = new ArrayList<>();
    RandomPlaylist masterPlaylist;
    RandomPlaylist userPickedPlaylist = masterPlaylist;
    AudioURI currentSong;
    LinkedList<MediaPlayer> songQueue = new LinkedList<>();
    ListIterator<MediaPlayer> songQueueIterator;

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    ArrayList<Future<MediaPlayerWURI>> futureMediaPlayers = new ArrayList<>();

    MOnCompletionListener mOnCompletionListener;

    static final double MAX_PERCENT = 0.1;
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
        if (songQueueIterator.hasNext()) {
            songQueueIterator.next().start();
        }
    }

    void play(AudioURI audioURI) {
        if (songQueue.size() > 0) {
            if(songQueueIterator.hasPrevious()) {
                MediaPlayer mediaPlayer1 = songQueueIterator.previous();
                if (mediaPlayer1.isPlaying()) {
                    mediaPlayer1.stop();
                    mediaPlayer1.prepareAsync();
                }
            }
        }
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(audioURI);
        try {
            if (mediaPlayerWURI == null && futureMediaPlayers.size() < FragmentSongs.nVisibleItems) {
                Iterator iterator = futureMediaPlayers.iterator();
                Future<MediaPlayerWURI> next;
                while (iterator.hasNext()) {
                    next = (Future<MediaPlayerWURI>) iterator.next();
                    mediaPlayerWURI = next.get();
                    songsMap.put(mediaPlayerWURI.audioURI.uri, mediaPlayerWURI);
                }
                if (!songsMap.containsKey(audioURI)) {
                    mediaPlayerWURI = new NextMediaPlayer(audioURI).call();
                    songsMap.put(mediaPlayerWURI.audioURI.uri, mediaPlayerWURI);
                }
            } else {
                mediaPlayerWURI = new NextMediaPlayer(audioURI).call();
                songsMap.put(mediaPlayerWURI.audioURI.uri, mediaPlayerWURI);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayerWURI = songsMap.get(audioURI.uri);
        mediaPlayerWURI.mediaPlayer.setOnCompletionListener(mOnCompletionListener);
        songQueueIterator.add(mediaPlayerWURI.mediaPlayer);
        songQueueIterator = songQueue.listIterator(songQueue.indexOf(mediaPlayerWURI.mediaPlayer));
        MediaPlayer mediaPlayer = songQueueIterator.next();
        mediaPlayer.start();
        currentSong = mediaPlayerWURI.audioURI;
    }

    public class NextMediaPlayer implements Callable<MediaPlayerWURI> {

        AudioURI audioURI;

        NextMediaPlayer(AudioURI audioURI) {
            this.audioURI = audioURI;
        }

        @Override
        public MediaPlayerWURI call() throws Exception {
            return new MediaPlayerWURI(MediaPlayer.create(getApplicationContext(), audioURI.uri), audioURI);
        }

    }

    public static class MOnCompletionListener implements MediaPlayer.OnCompletionListener {

        ActivityMain activityMain;

        MOnCompletionListener(ActivityMain activityMain) {
            this.activityMain = activityMain;
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            AudioURI uri = activityMain.userPickedPlaylist.getProbFun().fun();
            activityMain.play(uri);
            TextView textViewSongName = activityMain.findViewById(R.id.text_view_song_name);
            textViewSongName.setText(activityMain.currentSong.title);
            int millis = activityMain.currentSong.duration;
            SeekBar seekBar = activityMain.findViewById(R.id.seekBar);
            seekBar.setMax(millis);
            String endTime = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            TextView textViewCurrent = activityMain.findViewById(R.id.editTextCurrentTime);
            textViewCurrent.setText("00:00:00");
            TextView textViewEnd = activityMain.findViewById(R.id.editTextEndTime);
            textViewEnd.setText(endTime);
        }
    }

}