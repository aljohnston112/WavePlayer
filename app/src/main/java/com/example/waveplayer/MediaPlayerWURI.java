package com.example.waveplayer;

import android.media.MediaPlayer;
import android.util.Log;

public class MediaPlayerWURI {

    private static final String TAG = "MediaPlayerWURI";

    private final MediaPlayer mediaPlayer;

    final AudioURI audioURI;

    final ActivityMain activityMain;

    boolean isPrepared = true;

    MediaPlayerWURI(ActivityMain activityMain, MediaPlayer mediaPlayer, AudioURI audioURI){
        this.activityMain = activityMain;
        this.mediaPlayer = mediaPlayer;
        this.audioURI = audioURI;
        Log.v(TAG, "Set on prepared");
        mediaPlayer.setOnPreparedListener(null);
        mediaPlayer.setOnErrorListener(null);
        mediaPlayer.setOnPreparedListener(new MOnPreparedListener(this));
        mediaPlayer.setOnErrorListener(new MOnErrorListener());
    }

    public void release(){
        mediaPlayer.release();
    }

    public void prepareAsync(){
        Log.v(TAG, "Waiting for prepareAsync");
        isPrepared = false;
        mediaPlayer.prepareAsync();
    }

    public int getCurrentPosition(){
        return mediaPlayer.getCurrentPosition();
    }

    public void start(){
        mediaPlayer.start();
    }

    public boolean isPlaying(){
        return mediaPlayer.isPlaying();
    }

    public void stop(){
        Log.v(TAG, "MediaPlayer stopped");
        isPrepared = false;
        mediaPlayer.stop();
    }

    public void pause(){
        mediaPlayer.pause();
    }
    public void seekTo(int millis){
        mediaPlayer.seekTo(millis);
    }

    public void setOnCompletionListener(ActivityMain.MOnCompletionListener mOnCompletionListener) {
        mediaPlayer.setOnCompletionListener(mOnCompletionListener);
    }

    class MOnPreparedListener implements MediaPlayer.OnPreparedListener{

        final MediaPlayerWURI mediaPlayerWURI;

        public  MOnPreparedListener(MediaPlayerWURI mediaPlayerWURI){
            this.mediaPlayerWURI = mediaPlayerWURI;
        }

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            Log.v(TAG, "MediaPlayer prepared");
            mediaPlayerWURI.isPrepared = true;
        }
    }

    class MOnErrorListener implements MediaPlayer.OnErrorListener {

        public MOnErrorListener(){
        }

        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                activityMain.releaseMediaPlayers();
                return false;
        }

    }

}