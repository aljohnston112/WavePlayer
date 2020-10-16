package com.example.waveplayer;

import android.media.MediaPlayer;
import android.util.Log;

public class MediaPlayerWURI {

    private static final String TAG = "MediaPlayerWURI";

    private final MediaPlayerWURI mediaPlayerWURI = this;

    private MediaPlayer mediaPlayer;

    final AudioURI audioURI;

    final ServiceMain serviceMain;

    volatile boolean isPrepared = true;

    volatile boolean shouldPlay = false;

    private final MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            synchronized (mediaPlayerWURI) {
                serviceMain.releaseMediaPlayers();
                serviceMain.addToQueueAndPlay(serviceMain.currentSong);
                return false;
            }
        }
    };

    MediaPlayerWURI(final ServiceMain serviceMain, MediaPlayer mediaPlayer, AudioURI audioURI){
        this.serviceMain = serviceMain;
        this.mediaPlayer = mediaPlayer;
        this.audioURI = audioURI;
        Log.v(TAG, "Set on prepared");
        mediaPlayer.setOnPreparedListener(null);
        mediaPlayer.setOnErrorListener(null);
        mediaPlayer.setOnPreparedListener(new MOnPreparedListener(this));
        mediaPlayer.setOnErrorListener(onErrorListener);
    }

    synchronized public void shouldStart(boolean shouldPlay){
        if(shouldPlay && isPrepared){
            mediaPlayer.start();
        } else {
            this.shouldPlay = shouldPlay;
        }
    }

    synchronized public void release(){
        isPrepared = false;
        shouldPlay = false;
        mediaPlayer.reset();
        mediaPlayer.release();
    }

    synchronized public void prepareAsync(){
        Log.v(TAG, "Waiting for prepareAsync");
        isPrepared = false;
        mediaPlayer.prepareAsync();
    }

    synchronized public int getCurrentPosition(){
        if(isPrepared && serviceMain.songInProgress()) {
            return mediaPlayer.getCurrentPosition();
        } else{
            return -1;
        }
    }

    synchronized public boolean isPlaying(){
        if(isPrepared) {
            return mediaPlayer.isPlaying();
        } else {
            return false;
        }
    }

    synchronized public void stop(){
        Log.v(TAG, "MediaPlayer stopped");
        isPrepared = false;
        shouldPlay = false;
        mediaPlayer.stop();
    }

    synchronized public void pause(){
        if(isPrepared) {
            mediaPlayer.pause();
        }
        shouldPlay = false;
    }

    synchronized public void seekTo(int millis){
        if (isPrepared){
            mediaPlayer.seekTo(millis);
        }
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    class MOnPreparedListener implements MediaPlayer.OnPreparedListener{

        final MediaPlayerWURI mediaPlayerWURI;

        public  MOnPreparedListener(MediaPlayerWURI mediaPlayerWURI){
            this.mediaPlayerWURI = mediaPlayerWURI;
        }

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            synchronized (mediaPlayerWURI) {
                Log.v(TAG, "MediaPlayer prepared");
                mediaPlayerWURI.isPrepared = true;
                if (shouldPlay) {
                    mediaPlayer.start();
                    shouldPlay = false;
                }
            }
        }
    }

}