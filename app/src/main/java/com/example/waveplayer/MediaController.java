package com.example.waveplayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.List;
import java.util.Random;

public class MediaController {

    private static final Random random = new Random();

    public final MediaPlayer.OnCompletionListener onCompletionListener;

    private final ServiceMain serviceMain;

    private final MediaData mediaData;

    private AudioUri currentSong;

    public AudioUri getCurrentSong() {
        return currentSong;
    }

    private RandomPlaylist currentPlaylist;

    public void setCurrentPlaylistToMaster() {
        setCurrentPlaylist(mediaData.getMasterPlaylist());
    }

    public void clearProbabilities() {
        currentPlaylist.clearProbabilities();
    }
    
    private final SongQueue songQueue = new SongQueue();

    private boolean isPlaying = false;

    public boolean isPlaying() {
        return isPlaying;
    }

    private boolean songInProgress = false;

    public boolean songInProgress() {
        return songInProgress;
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

    private boolean haveAudioFocus = false;

    public static MediaController INSTANCE;

    private MediaController(ServiceMain serviceMain){
        this.serviceMain = serviceMain;
        onCompletionListener = new MediaPlayerOnCompletionListener(
                serviceMain.getApplicationContext(), this);
        mediaData = MediaData.getInstance(serviceMain.getApplicationContext());
        currentPlaylist = mediaData.getMasterPlaylist();
    }

    public static MediaController getInstance(ServiceMain serviceMain){
        if(INSTANCE == null){
            INSTANCE = new MediaController(serviceMain);
        }
        return INSTANCE;
    }

    public void pauseOrPlay() {
        if (currentSong != null) {
            MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.pause();
                    isPlaying = false;
                } else {
                    if (requestAudioFocus()) {
                        mediaPlayerWURI.shouldPlay(true);
                        isPlaying = true;
                        songInProgress = true;
                    }
                }
            }
        } else if (!isPlaying) {
            playNext();
        }
        serviceMain.updateNotification();
    }

    public boolean playNextInQueue(boolean addNew) {
        if (songQueue.hasNext()) {
            playAndMakeIfNeeded(songQueue.next());
        } else if (looping) {
            if (shuffling) {
                songQueue.goToFront();
                if (songQueue.hasNext()) {
                    playAndMakeIfNeeded(songQueue.next());
                }
            } else {
                return false;
            }
        } else if (addNew) {
            if (shuffling) {
                currentSong = currentPlaylist.next(random);
                addToQueueAndPlay(currentSong.getID());
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean playPreviousInQueue() {
        if (songQueue.hasPrevious()) {
            songQueue.previous();
            if (songQueue.hasPrevious()) {
                playAndMakeIfNeeded(songQueue.previous());
                songQueue.next();
            } else if (looping) {
                if (shuffling) {
                    songQueue.goToBack();
                    if (songQueue.hasPrevious()) {
                        playAndMakeIfNeeded(songQueue.previous());
                        songQueue.next();
                    } else {
                        return false;
                    }
                }
            } else {
                seekTo(0);
            }
            return true;
        }
        return false;
    }

    public void playNextInPlaylist() {
        if (currentPlaylist.hasNext()) {
            playAndMakeIfNeeded(currentPlaylist.next());
        } else if (looping) {
            currentPlaylist.goToFront();
            if (currentPlaylist.hasNext()) {
                playAndMakeIfNeeded(currentPlaylist.next());
            }
        }
    }

    public void playPreviousInPlaylist() {
        if (currentPlaylist.hasPrevious()) {
            playAndMakeIfNeeded(currentPlaylist.previous());
        } else if (looping) {
            currentPlaylist.goToBack();
            if (currentPlaylist.hasPrevious()) {
                playAndMakeIfNeeded(currentPlaylist.previous());
            }
        }
    }

    public void playNext() {
        if (loopingOne) {
            playLoopingOne();
        } else if (!playNextInQueue(true)) {
            playNextInPlaylist();
        }
    }

    public void playPrevious() {
        if (loopingOne) {
            playLoopingOne();
        } else if (!playPreviousInQueue()) {
            playPreviousInPlaylist();
        }
    }

    void playLoopingOne() {
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            mediaPlayerWURI.seekTo(serviceMain.getApplicationContext(), currentSong, 0);
            mediaPlayerWURI.shouldPlay(true);
            // TODO make a setting?
            //addToQueueAtCurrentIndex(currentSong.getUri());
        } else {
            // TODO what if current song is in playlist?
            playAndMakeIfNeeded(currentSong.getID());
        }
    }

    public void addToQueueAndPlay(Long songID) {
        playAndMakeIfNeeded(songID);
        songQueue.addToQueue(songID);
    }

    void playAndMakeIfNeeded(Long songID) {
        MediaPlayerWUri mediaPlayerWURI = mediaData.getMediaPlayerWUri(songID);
        if (mediaPlayerWURI != null) {
            playAndMakeIfNeeded(mediaPlayerWURI);
        } else {
                makeMediaPlayerWURIAndPlay(serviceMain.getApplicationContext(), songID);
        }
    }

    private void playAndMakeIfNeeded(MediaPlayerWUri mediaPlayerWURI) {
        stopCurrentSong();
        if (requestAudioFocus()) {
            mediaPlayerWURI.shouldPlay(true);
            isPlaying = true;
            songInProgress = true;
        }
        currentSong = AudioFileLoader.getAudioUri(serviceMain.getApplicationContext(), mediaPlayerWURI.id);
        serviceMain.updateNotification();
        currentPlaylist.setIndexTo(mediaPlayerWURI.id);
    }

    private void stopCurrentSong() {
        if (currentSong != null) {
            MediaPlayerWUri mediaPlayerWURI = mediaData.getMediaPlayerWUri(currentSong.getID());
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

    public void seekTo(int progress) {
        MediaPlayerWUri mediaPlayerWUri = getCurrentMediaPlayerWUri();
        if (mediaPlayerWUri != null) {
            mediaPlayerWUri.seekTo(serviceMain.getApplicationContext(), currentSong, progress);
        }
    }

    private void makeMediaPlayerWURIAndPlay(Context context, Long songID) {
        MediaPlayerWUri mediaPlayerWURI =
                new CallableCreateMediaPlayerWURI(context, this, songID).call();
        mediaData.addMediaPlayerWUri(mediaPlayerWURI.id, mediaPlayerWURI);
        playAndMakeIfNeeded(mediaPlayerWURI);
    }

    void stopAndPreparePrevious() {
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

    public RandomPlaylist getMasterPlaylist(){
        return mediaData.getMasterPlaylist();
    }

    public RandomPlaylist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setCurrentPlaylist(RandomPlaylist currentPlaylist) {
        this.currentPlaylist = currentPlaylist;
        songQueue.clearSongQueue();
    }

    public int getCurrentTime() {
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            return mediaPlayerWURI.getCurrentPosition();
        } else {
            return -1;
        }
    }

    private MediaPlayerWUri getCurrentMediaPlayerWUri() {
        if (currentSong != null) {
            return mediaData.getMediaPlayerWUri(currentSong.getID());
        }
        return null;
    }

    public void releaseMediaPlayers() {
        mediaData.releaseMediaPlayers();
    }

    private boolean requestAudioFocus() {
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
                                        pauseOrPlay();
                                    }
                                }
                                break;
                            case AudioManager.AUDIOFOCUS_LOSS:
                                synchronized (lock) {
                                    mResumeOnFocusGain = false;
                                }
                                if (isPlaying) {
                                    pauseOrPlay();
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

    public void loadMediaFiles(List<Long> newUris) {
        mediaData.loadMediaFiles(newUris);
    }

    public List<Song> getAllSongs() {
        return mediaData.getAllSongs();
    }

    MediaPlayerWUri getMediaPlayerWUri(Long songID) {
        return mediaData.getMediaPlayerWUri(songID);
    }

    public void addToQueue(Long songID) {
        songQueue.addToQueue(songID);
    }

    public boolean songQueueIsEmpty() {
        return songQueue.isEmpty();
    }
}