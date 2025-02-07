package io.fourth_finger.pinky_player.random_playlist

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.fourth_finger.playlist_data_source.AudioUri
import java.util.*

class MediaPlayerManager {

    private var haveAudioFocus: Boolean = false

    private var _songInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val songInProgress = _songInProgress as LiveData<Boolean>

    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying = _isPlaying as LiveData<Boolean>

    private var _currentAudioUri = MutableLiveData<AudioUri?>()
    val currentAudioUri = _currentAudioUri as LiveData<AudioUri?>

    var onCompletionListener: MediaPlayer.OnCompletionListener? = null
    var onErrorListener: MediaPlayer.OnErrorListener? = null
    private var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    private val songIDToMediaPlayerWithUriHashMap: HashMap<Long, MediaPlayerWithUri> = HashMap()

    fun setUp(
        context: Context,
        mediaSession: MediaSession
    ) {
        onCompletionListener = MediaPlayer.OnCompletionListener {
            mediaSession.playNext(context)
        }
        onErrorListener = MediaPlayer.OnErrorListener { _: MediaPlayer?, _: Int, _: Int ->
            synchronized(MediaPlayerWithUri.lock) {
                releaseMediaPlayers()
                if (songInProgress.value == false) {
                    mediaSession.playNext(context)
                }
                return@OnErrorListener false
            }
        }
        onAudioFocusChangeListener = object : AudioManager.OnAudioFocusChangeListener {
            val lock: Any = Any()
            var wasPlaying: Boolean = false
            override fun onAudioFocusChange(i: Int) {
                when (i) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        synchronized(lock) {
                            haveAudioFocus = true
                            if (isPlaying.value == false && wasPlaying) {
                                togglePlay(context)
                            }
                        }
                    }

                    AudioManager.AUDIOFOCUS_LOSS -> {
                        synchronized(lock) {
                            haveAudioFocus = false
                            if (isPlaying.value == true) {
                                wasPlaying = true
                                togglePlay(context)
                            } else {
                                wasPlaying = false
                            }
                        }
                    }
                }
            }
        }
    }

    fun setIsPlaying(isPlaying: Boolean) {
        this._isPlaying.postValue(isPlaying)
    }

    fun setSongInProgress(songInProgress: Boolean) {
        _songInProgress.postValue(songInProgress)
    }


    fun setCurrentAudioUri(audioUri: AudioUri) {
        _currentAudioUri.postValue(audioUri)
    }

    /**
     * @return The current position of the current media or
     * TIME_UNKNOWN if the time was not able to be extracted.
     */
    fun getCurrentTimeOfCurrentMedia(): Int {
        return getCurrentMediaPlayerWithUri()?.getCurrentPosition() ?: TIME_UNKNOWN
    }

    private fun getCurrentMediaPlayerWithUri(): MediaPlayerWithUri? {
        return _currentAudioUri.value?.id?.let { getMediaPlayerWithUri(it) }
    }

    private fun getMediaPlayerWithUri(songID: Long): MediaPlayerWithUri? {
        return songIDToMediaPlayerWithUriHashMap[songID]
    }

    private fun addMediaPlayerWithUri(mediaPlayerWithURI: MediaPlayerWithUri) {
        songIDToMediaPlayerWithUriHashMap[mediaPlayerWithURI.id] = mediaPlayerWithURI
    }

    private fun requestAudioFocus(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (haveAudioFocus) {
            return true
        }
        val result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes: AudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val audioFocusRequest: AudioFocusRequest = AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN
            )
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(onAudioFocusChangeListener!!)
                .build()
            result = audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            result = audioManager.requestAudioFocus(
                onAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        haveAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun startPlayback(
        context: Context,
        mediaPlayerWithURI: MediaPlayerWithUri
    ) {
        if (requestAudioFocus(context)) {
            mediaPlayerWithURI.shouldPlay(true)
            setIsPlaying(false)
            // TODO Song may not be in progress
            setSongInProgress(false)
        }
    }

    fun togglePlay(context: Context) {
        val mediaPlayerWithURI: MediaPlayerWithUri? = getCurrentMediaPlayerWithUri()
        if (mediaPlayerWithURI == null) {
            return
        }

        if (mediaPlayerWithURI.isPrepared() && mediaPlayerWithURI.isPlaying()) {
            mediaPlayerWithURI.pause()
            setIsPlaying(false)
        } else {
            startPlayback(
                context,
                mediaPlayerWithURI
            )
        }
    }

    fun seekTo(context: Context, progress: Int) {
        currentAudioUri.value?.let {
            getCurrentMediaPlayerWithUri()?.seekTo(
                context,
                it,
                progress
            )
        }
    }

    fun makeIfNeededAndPlay(
        context: Context,
        mediaPlayerManager: MediaPlayerManager,
        songID: Long
    ) {
        val audioUriCurrent = AudioUri.getAudioUri(
            context,
            songID
        )
        if (audioUriCurrent != null) {
            setCurrentAudioUri(audioUriCurrent)
        }
        var mediaPlayerWithUri: MediaPlayerWithUri? = getMediaPlayerWithUri(songID)
        if (mediaPlayerWithUri == null) {
            try {
                mediaPlayerWithUri = MediaPlayerWithUri(
                    mediaPlayerManager,
                    MediaPlayer.create(
                        context,
                        AudioUri.getUri(songID)
                    ),
                    songID
                )
                // TODO better error handling
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayerWithUri?.let { addMediaPlayerWithUri(it) }
        }
        mediaPlayerWithUri?.let {
            startPlayback(
                context,
                it
            )
        }
    }

    fun restartCurrentSong(
        context: Context,
        mediaPlayerManager: MediaPlayerManager,
    ) {
        val mediaPlayerWURI: MediaPlayerWithUri? = getCurrentMediaPlayerWithUri()
        if (mediaPlayerWURI != null) {
            currentAudioUri.value?.let {
                mediaPlayerWURI.seekTo(
                    context,
                    it,
                    0
                )
                mediaPlayerWURI.shouldPlay(true)
                setIsPlaying(true)
                // TODO may not start playing
                setSongInProgress(true)
            }
        } else {
            currentAudioUri.value?.let {
                makeIfNeededAndPlay(
                    context,
                    mediaPlayerManager,
                    it.id
                )
            }
        }
    }

    fun stopCurrentSong(context: Context) {
        getCurrentMediaPlayerWithUri()?.let { currentMediaPlayerWithUri ->
            if (currentMediaPlayerWithUri.isPrepared() && currentMediaPlayerWithUri.isPlaying()) {
                currentAudioUri.value?.let { audioUri ->
                    currentMediaPlayerWithUri.stop(
                        context,
                        audioUri
                    )
                    currentMediaPlayerWithUri.prepareAsync()
                }
            } else {
                currentMediaPlayerWithUri.shouldPlay(false)
            }
        } ?: releaseMediaPlayers()
        setIsPlaying(false)
        setSongInProgress(false)
    }

    private fun releaseMediaPlayers() {
        // TODO properly synchronize hashMap
        setIsPlaying(false)
        setSongInProgress(false)
        synchronized(this) {
            for (mediaPlayerWURI in songIDToMediaPlayerWithUriHashMap.values) {
                mediaPlayerWURI.release()
            }
            songIDToMediaPlayerWithUriHashMap.clear()
        }
    }

    fun cleanUp() {
        releaseMediaPlayers()
        onAudioFocusChangeListener = null
        onErrorListener = null
        onCompletionListener = null
        _currentAudioUri.postValue(null)
    }

    companion object {
        const val TIME_UNKNOWN = -1
    }

}