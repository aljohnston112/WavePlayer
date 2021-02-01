package com.example.waveplayer.media_controller;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.random_playlist.SongQueue;
import com.example.waveplayer.service_main.ServiceMain;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class MediaController {

    public static MediaController INSTANCE;

    private static final Random random = new Random();

    private static final String TAG = "MediaController";

    protected final MediaPlayer.OnCompletionListener onCompletionListener;

    private final MediaData mediaData = MediaData.getInstance();

    private final SongQueue songQueue = new SongQueue();

    private RandomPlaylist currentPlaylist;

    private AudioUri currentAudioUri;

    private boolean haveAudioFocus = false;
    private boolean songInProgress = false;
    private boolean isPlaying = false;
    private boolean shuffling = true;
    private boolean looping = false;
    private boolean loopingOne = false;

    public static synchronized MediaController getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MediaController(context);
        }
        return INSTANCE;
    }

    private MediaController(Context context) {
        onCompletionListener = mediaPlayer -> {
            AudioUri audioUri = getCurrentAudioUri();
            getMediaPlayerWUri(audioUri.id).resetIfMKV(audioUri, context);
            playNext(context);
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setAction(context.getResources().getString(R.string.broadcast_receiver_action_on_completion));
            context.sendBroadcast(intent);
        };
        mediaData.loadData(context);
        ServiceMain.executorServiceFIFO.execute(
                () -> currentPlaylist = mediaData.getMasterPlaylist());
    }

    public void addToQueue(Long songID) {
        songQueue.addToQueue(songID);
    }

    public boolean songQueueIsEmpty() {
        return songQueue.isEmpty();
    }

    public void clearSongQueue() {
        songQueue.clearSongQueue();
    }

    public void goToFrontOfQueue() {
        songQueue.goToFront();
    }

    public RandomPlaylist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setCurrentPlaylist(RandomPlaylist currentPlaylist) {
        this.currentPlaylist = currentPlaylist;
    }

    public void setCurrentPlaylistToMaster() {
        setCurrentPlaylist(mediaData.getMasterPlaylist());
    }

    public void clearProbabilities(Context context) {
        currentPlaylist.clearProbabilities(context);
    }

    public void lowerProbabilities(Context context) {
        currentPlaylist.lowerProbabilities(context, mediaData.getLowerProb());
    }

    public AudioUri getCurrentAudioUri() {
        return currentAudioUri;
    }

    public Uri getCurrentUri() {
        return getCurrentAudioUri().getUri();
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
        if (currentAudioUri != null) {
            return mediaData.getMediaPlayerWUri(currentAudioUri.id);
        }
        return null;
    }

    public void releaseMediaPlayers() {
        mediaData.releaseMediaPlayers();
    }

    public boolean isSongInProgress() {
        return songInProgress;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isShuffling() {
        return shuffling;
    }

    public void setShuffling(boolean shuffling) {
        this.shuffling = shuffling;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public boolean isLoopingOne() {
        return loopingOne;
    }

    public void setLoopingOne(boolean loopingOne) {
        this.loopingOne = loopingOne;
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

    private void stopCurrentSongAndPlay(Context context, MediaPlayerWUri mediaPlayerWURI) {
        stopCurrentSong();
        if (requestAudioFocus(context)) {
            mediaPlayerWURI.shouldPlay(true);
            isPlaying = true;
            songInProgress = true;
        }
        currentAudioUri = AudioUri.getAudioUri(context, mediaPlayerWURI.id);
        currentPlaylist.setIndexTo(mediaPlayerWURI.id);
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

    public void addToQueueAndPlay(Context context, Long songID) {
        songQueue.addToQueue(songID);
        playNext(context);
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

    public void playPrevious(Context context) {
        if (loopingOne) {
            playLoopingOne(context);
        } else if (!playPreviousInQueue(context)) {
            playPreviousInPlaylist(context);
        }
    }

    private void makeIfNeededAndPlay(Context context, Long songID) {
        MediaPlayerWUri mediaPlayerWUri = mediaData.getMediaPlayerWUri(songID);
        if (mediaPlayerWUri == null) {
            try {
                mediaPlayerWUri =
                        ((Callable<MediaPlayerWUri>) () -> {
                            MediaPlayerWUri mediaPlayerWURI = new MediaPlayerWUri(
                                    context,
                                    MediaPlayer.create(context, AudioUri.getUri(currentAudioUri.id)),
                                    currentAudioUri.id);
                            mediaPlayerWURI.setOnCompletionListener(onCompletionListener);
                            return mediaPlayerWURI;
                        }).call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaData.addMediaPlayerWUri(mediaPlayerWUri.id, mediaPlayerWUri);
        }
        stopCurrentSongAndPlay(context, mediaPlayerWUri);
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

    private void playLoopingOne(Context context) {
        MediaPlayerWUri mediaPlayerWURI = getCurrentMediaPlayerWUri();
        if (mediaPlayerWURI != null) {
            mediaPlayerWURI.seekTo(context, currentAudioUri, 0);
            mediaPlayerWURI.shouldPlay(true);
            isPlaying = true;
            songInProgress = true;
            // TODO make a setting?
            //addToQueueAtCurrentIndex(currentSong.getUri());
        } else {
            makeIfNeededAndPlay(context, currentAudioUri.id);
        }
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
                    boolean wasPlaying;

                    @Override
                    public void onAudioFocusChange(int i) {
                        switch (i) {
                            case AudioManager.AUDIOFOCUS_GAIN: {
                                synchronized (lock) {
                                    // TODO DO NOT PLAY MUSIC PAUSED BEFORE FOCUS LOSS!
                                    haveAudioFocus = true;
                                    if (!isPlaying && wasPlaying) {
                                        pauseOrPlay(context);
                                    }
                                }
                                break;
                            }
                            case AudioManager.AUDIOFOCUS_LOSS: {
                                synchronized (lock) {
                                    haveAudioFocus = false;
                                    if (isPlaying) {
                                        wasPlaying = true;
                                        pauseOrPlay(context);
                                    } else {
                                        wasPlaying = false;
                                    }
                                    break;
                                }
                            }
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

    // Forwarding methods

    public MediaPlayerWUri getMediaPlayerWUri(Long songID) {
        return mediaData.getMediaPlayerWUri(songID);
    }

    public Song getSong(long songID) {
        return mediaData.getSong(songID);
    }

    public List<Song> getAllSongs() {
        return mediaData.getAllSongs();
    }

    public RandomPlaylist getMasterPlaylist() {
        return mediaData.getMasterPlaylist();
    }

    public RandomPlaylist getPlaylist(String playlistName) {
        return mediaData.getPlaylist(playlistName);
    }

    public List<RandomPlaylist> getPlaylists() {
        return mediaData.getPlaylists();
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        mediaData.addPlaylist(randomPlaylist);
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        mediaData.addPlaylist(position, randomPlaylist);
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        mediaData.removePlaylist(randomPlaylist);
    }

    public double getMaxPercent() {
        return mediaData.getMaxPercent();
    }

    public void setMaxPercent(double maxPercent) {
        mediaData.setMaxPercent(maxPercent);
    }

    public double getPercentChangeUp() {
        return mediaData.getPercentChangeUp();
    }

    public void setPercentChangeUp(double percentChangeUp) {
        mediaData.setPercentChangeUp(percentChangeUp);
    }

    public double getPercentChangeDown() {
        return mediaData.getPercentChangeDown();
    }

    public void setPercentChangeDown(double percentChangeDown) {
        mediaData.setPercentChangeDown(percentChangeDown);
    }

}