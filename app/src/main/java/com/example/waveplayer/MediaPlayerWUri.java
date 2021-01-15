package com.example.waveplayer;

import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import static android.media.MediaPlayer.SEEK_CLOSEST;

public class MediaPlayerWUri {

    private static final String TAG = "MediaPlayerWURI";

    private final Object lock = new Object();

    private MediaPlayer mediaPlayer;

    final AudioUri audioURI;

    private final ServiceMain serviceMain;

    private volatile boolean isPrepared = true;

    public boolean isPrepared() {
        synchronized (lock) {
            return isPrepared;
        }
    }

    private volatile boolean shouldPlay = false;

    MediaPlayerWUri(final ServiceMain serviceMain, MediaPlayer mediaPlayer, AudioUri audioURI){
        this.serviceMain = serviceMain;
        this.mediaPlayer = mediaPlayer;
        this.audioURI = audioURI;
        mediaPlayer.setOnPreparedListener(null);
        mediaPlayer.setOnErrorListener(null);
        MOnPreparedListener mOnPreparedListener = new MOnPreparedListener(this);
        mediaPlayer.setOnPreparedListener(mOnPreparedListener);
        MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                synchronized (lock) {
                    serviceMain.releaseMediaPlayers();
                    serviceMain.addToQueueAndPlay(serviceMain.getCurrentSong().getUri());
                    return false;
                }
            }
        };
        mediaPlayer.setOnErrorListener(onErrorListener);
    }

    public void shouldPlay(boolean shouldPlay){
        synchronized (lock) {
            if (shouldPlay && isPrepared) {
                mediaPlayer.start();
            } else {
                this.shouldPlay = shouldPlay;
            }
        }
    }

    public void release(){
        synchronized (lock) {
            isPrepared = false;
            shouldPlay = false;
            mediaPlayer.reset();
            mediaPlayer.release();
        }
    }

    public void prepareAsync(){
        Log.v(TAG, "Waiting for prepareAsync");
        synchronized (lock) {
            isPrepared = false;
            mediaPlayer.prepareAsync();
        }
    }

    public int getCurrentPosition(){
        synchronized (lock) {
            if (isPrepared && serviceMain.songInProgress()) {
                return mediaPlayer.getCurrentPosition();
            } else {
                return -1;
            }
        }
    }

    public boolean isPlaying(){
        synchronized (lock) {
            if (isPrepared) {
                return mediaPlayer.isPlaying();
            } else {
                return false;
            }
        }
    }

    public void stop(){
        Log.v(TAG, "MediaPlayer stopped");
        synchronized (lock) {
            isPrepared = false;
            shouldPlay = false;
            mediaPlayer.stop();
        }
    }

    public void pause(){
        synchronized (lock) {
            if (isPrepared) {
                mediaPlayer.pause();
            }
            shouldPlay = false;
        }
    }

    public void seekTo(int millis){
        synchronized (lock) {
            if (isPrepared) {
                String[] s = audioURI.displayName.split("\\.");
                if(s.length > 0) {
                    if(s[s.length - 1].toLowerCase().equals("mkv")){
                        mediaPlayer.reset();
                        mediaPlayer.release();
                        mediaPlayer = null;
                        mediaPlayer = MediaPlayer.create(
                                serviceMain.getApplicationContext(), audioURI.getUri());
                        mediaPlayer.setOnPreparedListener(null);
                        mediaPlayer.setOnErrorListener(null);
                        MOnPreparedListener mOnPreparedListener = new MOnPreparedListener(this);
                        mediaPlayer.setOnPreparedListener(mOnPreparedListener);
                        MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
                            @Override
                            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                                synchronized (lock) {
                                    serviceMain.releaseMediaPlayers();
                                    serviceMain.addToQueueAndPlay(serviceMain.getCurrentSong().getUri());
                                    return false;
                                }
                            }
                        };
                        mediaPlayer.setOnErrorListener(onErrorListener);
                        setOnCompletionListener(serviceMain.onCompletionListener);
                        this.shouldPlay = true;
                        return;
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer.seekTo(millis, SEEK_CLOSEST);
                } else {
                    mediaPlayer.seekTo(millis);
                }
            }
        }
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    class MOnPreparedListener implements MediaPlayer.OnPreparedListener{

        private final MediaPlayerWUri mediaPlayerWURI;

        public MOnPreparedListener(MediaPlayerWUri mediaPlayerWURI){
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