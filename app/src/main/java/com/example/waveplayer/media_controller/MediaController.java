package com.example.waveplayer.media_controller;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.util.Log;

import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

public class MediaController {

    static final String TAG = "MediaController";

    public static MediaController INSTANCE;

    synchronized public static MediaController getInstance(Context context){
        if(INSTANCE == null){
            INSTANCE = new MediaController(context);
        }
        return INSTANCE;
    }

    private static final Random random = new Random();

    private final MediaData mediaData = MediaData.getInstance();

    public final MediaPlayerOnCompletionListener onCompletionListener;

    private AudioUri currentAudioUri;

    public AudioUri getCurrentAudioUri() {
        return currentAudioUri;
    }

    private RandomPlaylist currentPlaylist;

    public void setCurrentPlaylistToMaster() {
        setCurrentPlaylist(mediaData.getMasterPlaylist());
    }

    public RandomPlaylist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setCurrentPlaylist(RandomPlaylist currentPlaylist) {
        this.currentPlaylist = currentPlaylist;
    }

    public void clearProbabilities(Context context) {
        currentPlaylist.clearProbabilities(context);
    }

    public void lowerProbabilities(Context context) {
        currentPlaylist.lowerProbabilities(context, mediaData.getLowerProb());
    }

    private final SongQueue songQueue = new SongQueue();

    public void addToQueue(Long songID) {
        songQueue.addToQueue(songID);
    }

    public boolean songQueueIsEmpty() {
        return songQueue.isEmpty();
    }

    public void clearSongQueue(){
        songQueue.clearSongQueue();
    }

    private boolean songInProgress = false;

    public boolean songInProgress() {
        return songInProgress;
    }

    private boolean haveAudioFocus = false;

    private boolean isPlaying = false;

    public boolean isPlaying() {
        return isPlaying;
    }

    private boolean shuffling = true;

    public boolean shuffling() {
        return shuffling;
    }

    public void shuffling(boolean shuffling) {
        this.shuffling = shuffling;
    }

    private boolean looping = false;

    public boolean looping() {
        return looping;
    }

    public void looping(boolean looping) {
        this.looping = looping;
    }

    private boolean loopingOne = false;

    public boolean loopingOne() {
        return loopingOne;
    }

    public void loopingOne(boolean loopingOne) {
        Log.v(TAG, "loopingOne start");
        this.loopingOne = loopingOne;
        Log.v(TAG, "loopingOne end");
    }

    private MediaController(Context context){
        onCompletionListener = new MediaPlayerOnCompletionListener(
                context, this);
        currentPlaylist = mediaData.getMasterPlaylist();
    }

    private MediaPlayerWUri getCurrentMediaPlayerWUri() {
        if (currentAudioUri != null) {
            return mediaData.getMediaPlayerWUri(currentAudioUri.id);
        }
        return null;
    }

    public int getCurrentTime() {
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            return mediaPlayerWURI.getCurrentPosition();
        } else {
            return -1;
        }
    }

    public void releaseMediaPlayers() {
        mediaData.releaseMediaPlayers();
    }

    public void addToQueueAndPlay(Context context, Long songID) {
        songQueue.addToQueue(songID);
        playNext(context);
    }

    private void makeIfNeededAndPlay(Context context, Long songID) {
        MediaPlayerWUri mediaPlayerWUri = mediaData.getMediaPlayerWUri(songID);
        if (mediaPlayerWUri == null) {
            mediaPlayerWUri =
                    new CallableCreateMediaPlayerWUri(
                            context, this, onCompletionListener, songID).call();
            mediaData.addMediaPlayerWUri(mediaPlayerWUri.id, mediaPlayerWUri);
        }
        stopCurrentSongAndPlay(context, mediaPlayerWUri);
    }

    private void stopCurrentSongAndPlay(Context context, MediaPlayerWUri mediaPlayerWURI) {
        stopCurrentSong();
        if (requestAudioFocus(context)) {
            mediaPlayerWURI.shouldPlay(true);
            isPlaying = true;
            songInProgress = true;
        }
        currentAudioUri = MediaData.getAudioUri(context, mediaPlayerWURI.id);
            currentPlaylist.setIndexTo(mediaPlayerWURI.id);
    }

    public void pauseOrPlay(Context context) {
        if (currentAudioUri != null) {
            MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.pause();
                    isPlaying = false;
                } else {
                    if (requestAudioFocus(context)) {
                        mediaPlayerWURI.shouldPlay(true);
                        isPlaying = true;
                        songInProgress = true;
                    }
                }
            }
        } else if (!isPlaying) {
            playNext(context);
        }
    }

    private void stopCurrentSong() {
        if (currentAudioUri != null) {
            MediaPlayerWUri mediaPlayerWURI = mediaData.getMediaPlayerWUri(currentAudioUri.id);
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.stop();
                    mediaPlayerWURI.prepareAsync();
                }
            } else {
                releaseMediaPlayers();
            }
            songInProgress = false;
            isPlaying = false;
        }
    }

    private void stopPrevious() {
        if (songQueue.hasPrevious()) {
            final MediaPlayerWUri mediaPlayerWURI =
                    mediaData.getMediaPlayerWUri(songQueue.previous());
            songQueue.next();
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.stop();
                    mediaPlayerWURI.prepareAsync();
                }
            } else {
                mediaData.releaseMediaPlayers();
            }
            songInProgress = false;
            isPlaying = false;
        }
    }

    private void playLoopingOne(Context context) {
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            mediaPlayerWURI.seekTo(context, currentAudioUri, 0);
            mediaPlayerWURI.shouldPlay(true);
            // TODO make a setting?
            isPlaying = true;
            songInProgress = true;
            //addToQueueAtCurrentIndex(currentSong.getUri());
        } else {
            makeIfNeededAndPlay(context, currentAudioUri.id);
        }
    }

    public void playNext(Context context) {
        Log.v(TAG, "playNext started");
        stopPrevious();
        if (loopingOne) {
            playLoopingOne(context);
        } else if (!playNextInQueue(context)) {
            playNextInPlaylist(context);
        }
        Log.v(TAG, "playNext ended");
    }

    private boolean playNextInQueue(Context context) {
        if (songQueue.hasNext()) {
            makeIfNeededAndPlay(context, songQueue.next());
            return true;
        } else if (looping) {
            if (shuffling) {
                songQueue.goToFront();
                if (songQueue.hasNext()) {
                    makeIfNeededAndPlay(context, songQueue.next());
                    return true;
                }
            } else {
                return false;
            }
        } else if (shuffling) {
                currentAudioUri = currentPlaylist.next(context, random);
                addToQueueAndPlay(context, currentAudioUri.id);
                return true;
            }
        return false;
    }

    private void playNextInPlaylist(Context context) {
        currentAudioUri = currentPlaylist.next(context, random, looping, shuffling);
        addToQueueAndPlay(context, currentAudioUri.id);
    }

    public void playPrevious(Context context) {
        if (loopingOne) {
            playLoopingOne(context);
        } else if (!playPreviousInQueue(context)) {
            playPreviousInPlaylist(context);
        }
    }

    private boolean playPreviousInQueue(Context context) {
        if (songQueue.hasPrevious()) {
            songQueue.previous();
            if (songQueue.hasPrevious()) {
                makeIfNeededAndPlay(context, songQueue.previous());
                songQueue.next();
                return true;
            } else if (looping) {
                if (shuffling) {
                    songQueue.goToBack();
                    if (songQueue.hasPrevious()) {
                        makeIfNeededAndPlay(context, songQueue.previous());
                        songQueue.next();
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return !looping;
    }

    private void playPreviousInPlaylist(Context context) {
        currentAudioUri = currentPlaylist.previous(context, random, looping, shuffling);
        addToQueueAndPlay(context, currentAudioUri.id);

    }

    public void seekTo(Context context, int progress) {
        MediaPlayerWUri mediaPlayerWUri = getCurrentMediaPlayerWUri();
        if (mediaPlayerWUri != null) {
            mediaPlayerWUri.seekTo(context, currentAudioUri, progress);
        }
        if (!isPlaying()) {
            pauseOrPlay(context);
        }
    }

    private boolean requestAudioFocus(final Context context) {
        if (haveAudioFocus) {
            return true;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener =
                new AudioManager.OnAudioFocusChangeListener() {
                    final Object lock = new Object();
                    boolean mResumeOnFocusGain = false;

                    @Override
                    public void onAudioFocusChange(int i) {
                        switch (i) {
                            case AudioManager.AUDIOFOCUS_GAIN:
                                if (mResumeOnFocusGain) {
                                    synchronized (lock) {
                                        mResumeOnFocusGain = false;
                                    }
                                    // TODO DO NOT PLAY MUSIC PAUSED BEFORE FOCUS LOSS!
                                    if (!isPlaying) {
                                        pauseOrPlay(context);
                                    }
                                }
                                break;
                            case AudioManager.AUDIOFOCUS_LOSS:
                                synchronized (lock) {
                                    mResumeOnFocusGain = false;
                                }
                                if (isPlaying) {
                                    pauseOrPlay(context);
                                }
                                break;
                        }
                    }
                };
        int result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                    .build();
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(
                    onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        haveAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public MediaPlayerWUri getMediaPlayerWUri(Long songID) {
        return mediaData.getMediaPlayerWUri(songID);
    }

    public List<Song> getAllSongs() {
        return mediaData.getAllSongs();
    }

    public double getPercentChangeDown() {
        return mediaData.getPercentChangeDown();
    }

    public void goToFront() {
        songQueue.goToFront();
    }
}