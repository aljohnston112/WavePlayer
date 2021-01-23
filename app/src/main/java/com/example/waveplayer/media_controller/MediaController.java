package com.example.waveplayer.media_controller;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.service_main.ServiceMain;

import java.util.Random;

public class MediaController {

    public static MediaController INSTANCE;

    synchronized public static MediaController getInstance(ServiceMain serviceMain){
        if(INSTANCE == null){
            INSTANCE = new MediaController(serviceMain);
        }
        return INSTANCE;
    }

    private static final Random random = new Random();

    private final ServiceMain serviceMain;

    private final MediaData mediaData;

    public final MediaPlayer.OnCompletionListener onCompletionListener;

    private AudioUri currentSong;

    public AudioUri getCurrentSong() {
        return currentSong;
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
        songQueue.clearSongQueue();
    }

    public void clearProbabilities(Context context) {
        currentPlaylist.clearProbabilities(context);
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
        this.loopingOne = loopingOne;
    }

    private MediaController(ServiceMain serviceMain){
        this.serviceMain = serviceMain;
        mediaData = MediaData.getInstance(serviceMain.getApplicationContext());
        onCompletionListener = new MediaPlayerOnCompletionListener(
                serviceMain.getApplicationContext(), this);
        currentPlaylist = mediaData.getMasterPlaylist();
    }

    private MediaPlayerWUri getCurrentMediaPlayerWUri() {
        if (currentSong != null) {
            return mediaData.getMediaPlayerWUri(currentSong.id);
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
                    new CallableCreateMediaPlayerWUri(context, this, songID).call();
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
        currentSong = MediaData.getAudioUri(serviceMain.getApplicationContext(), mediaPlayerWURI.id);
        serviceMain.updateNotification();
        currentPlaylist.setIndexTo(mediaPlayerWURI.id);
    }

    public void pauseOrPlay(Context context) {
        if (currentSong != null) {
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
        serviceMain.updateNotification();
    }

    private void stopCurrentSong() {
        if (currentSong != null) {
            MediaPlayerWUri mediaPlayerWURI = mediaData.getMediaPlayerWUri(currentSong.id);
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
            mediaPlayerWURI.seekTo(serviceMain.getApplicationContext(), currentSong, 0);
            mediaPlayerWURI.shouldPlay(true);
            // TODO make a setting?
            //addToQueueAtCurrentIndex(currentSong.getUri());
        } else {
            makeIfNeededAndPlay(context, currentSong.id);
        }
    }

    public void playNext(Context context) {
        stopPrevious();
        if (loopingOne) {
            playLoopingOne(context);
        } else if (!playNextInQueue(context)) {
            playNextInPlaylist(context);
        }
    }

    private boolean playNextInQueue(Context context) {
        if (songQueue.hasNext()) {
            makeIfNeededAndPlay(context, songQueue.next());
        } else if (looping) {
            if (shuffling) {
                songQueue.goToFront();
                if (songQueue.hasNext()) {
                    makeIfNeededAndPlay(context, songQueue.next());
                }
            } else {
                return false;
            }
        } else if (shuffling) {
                currentSong = currentPlaylist.next(context, random);
                addToQueueAndPlay(context, currentSong.id);
            }
        return true;
    }

    private void playNextInPlaylist(Context context) {
        currentSong = currentPlaylist.next(context, random, looping, shuffling);
        addToQueueAndPlay(context, currentSong.id);
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
            } else if (looping) {
                if (shuffling) {
                    songQueue.goToBack();
                    if (songQueue.hasPrevious()) {
                        makeIfNeededAndPlay(context, songQueue.previous());
                        songQueue.next();
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
            return true;
        }
        return looping;
    }

    private void playPreviousInPlaylist(Context context) {
        currentSong = currentPlaylist.previous(context, random, looping, shuffling);
        addToQueueAndPlay(context, currentSong.id);

    }

    public void seekTo(Context context, int progress) {
        MediaPlayerWUri mediaPlayerWUri = getCurrentMediaPlayerWUri();
        if (mediaPlayerWUri != null) {
            mediaPlayerWUri.seekTo(serviceMain.getApplicationContext(), currentSong, progress);
        }
        if (!isPlaying()) {
            pauseOrPlay(context);
        }
    }

    private boolean requestAudioFocus(final Context context) {
        if (haveAudioFocus) {
            return true;
        }
        AudioManager audioManager = (AudioManager) serviceMain.getSystemService(Context.AUDIO_SERVICE);
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

}