package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.fourthFinger.playlistDataSource.AudioUri
import java.util.*

class MediaPlayerManager {

    private var haveAudioFocus: Boolean = false

    private var _songInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val songInProgress = _songInProgress as LiveData<Boolean>

    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying = _isPlaying as LiveData<Boolean>

    private var _currentAudioUri = MutableLiveData<AudioUri>()
    val currentAudioUri = _currentAudioUri as LiveData<AudioUri>

    var onCompletionListener: MediaPlayer.OnCompletionListener? = null
    var onErrorListener: MediaPlayer.OnErrorListener? = null
    private var onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    fun setUp(
        context: Context,
        mediaSession: MediaSession
    ) {
        onCompletionListener = MediaPlayer.OnCompletionListener {
            mediaSession.playNext(context)
        }
        onErrorListener = MediaPlayer.OnErrorListener { _: MediaPlayer?, _: Int, _: Int ->
            synchronized(MediaPlayerWUri.lock) {
                releaseMediaPlayers()
                if (isSongInProgress() == false) {
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
                                pauseOrPlay(context)
                            }
                        }
                    }

                    AudioManager.AUDIOFOCUS_LOSS -> {
                        synchronized(lock) {
                            haveAudioFocus = false
                            if (isPlaying.value == true) {
                                wasPlaying = true
                                pauseOrPlay(context)
                            } else {
                                wasPlaying = false
                            }
                        }
                    }
                }
            }
        }
    }

    fun isSongInProgress(): Boolean? {
        return _songInProgress.value
    }

    fun setIsPlaying(isPlaying: Boolean) {
        this._isPlaying.postValue(isPlaying)
        _songInProgress.value = isPlaying
    }

    fun setCurrentAudioUri(audioUri: AudioUri) {
        _currentAudioUri.value = audioUri
    }

    fun getCurrentTimeOfPlayingMedia(): Int {
        return getCurrentMediaPlayerWUri()?.getCurrentPosition() ?: -1
    }

    private fun getCurrentMediaPlayerWUri(): MediaPlayerWUri? {
        return _currentAudioUri.value?.id?.let { getMediaPlayerWUri(it) }
    }

    private val songIDToMediaPlayerWUriHashMap: HashMap<Long, MediaPlayerWUri> = HashMap()
    private fun getMediaPlayerWUri(songID: Long): MediaPlayerWUri? {
        return songIDToMediaPlayerWUriHashMap[songID]
    }

    private fun addMediaPlayerWUri(mediaPlayerWURI: MediaPlayerWUri) {
        songIDToMediaPlayerWUriHashMap[mediaPlayerWURI.id] = mediaPlayerWURI
    }

    fun removeMediaPlayerWUri(songID: Long) {
        songIDToMediaPlayerWUriHashMap.remove(songID)
    }

    private fun releaseMediaPlayers() {
        // TODO properly synchronize hashMap
        synchronized(this) {
            for (mediaPlayerWURI in songIDToMediaPlayerWUriHashMap.values) {
                mediaPlayerWURI.release()
            }
            songIDToMediaPlayerWUriHashMap.clear()
        }
        _isPlaying.postValue(false)
        _songInProgress.postValue(false)
    }

    fun cleanUp() {
        releaseMediaPlayers()
        onAudioFocusChangeListener = null
        onErrorListener = null
        onCompletionListener = null
        _currentAudioUri.postValue(null)
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
            result = audioManager.requestAudioFocus(
                onAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        haveAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun pauseOrPlay(context: Context) {
        val mediaPlayerWURI: MediaPlayerWUri? = getCurrentMediaPlayerWUri()
        if (mediaPlayerWURI != null) {
            if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                mediaPlayerWURI.pause()
                setIsPlaying(false)
            } else {
                if (requestAudioFocus(context)) {
                    mediaPlayerWURI.shouldPlay(true)
                    setIsPlaying(true)
                    _songInProgress.value = true
                }
            }
        }

    }

    fun stopCurrentSong(context: Context) {
        getCurrentMediaPlayerWUri()?.let {
            if (it.isPrepared() && it.isPlaying()) {
                currentAudioUri.value?.let { it1 ->
                    it.stop(context, it1)
                    it.prepareAsync()
                }
            } else {
                it.shouldPlay(false)
            }
        } ?: releaseMediaPlayers()
        _songInProgress.value = false
        setIsPlaying(false)
    }

    fun playLoopingOne(
        context: Context,
        mediaPlayerManager: MediaPlayerManager,
    ) {
        val mediaPlayerWURI: MediaPlayerWUri? = getCurrentMediaPlayerWUri()
        if (mediaPlayerWURI != null) {
            currentAudioUri.value?.let {
                mediaPlayerWURI.seekTo(context, it, 0)
                mediaPlayerWURI.shouldPlay(true)
                setIsPlaying(true)
                // TODO make a setting?
                //addToQueueAtCurrentIndex(currentSong.getUri());
            }
        } else {
            currentAudioUri.value?.let {
                makeIfNeededAndPlay(context, mediaPlayerManager, it.id)
            }
        }
    }

    fun makeIfNeededAndPlay(
        context: Context,
        mediaPlayerManager: MediaPlayerManager,
        songID: Long
    ) {
        val audioUriCurrent = AudioUri.getAudioUri(context, songID)
        if (audioUriCurrent != null) {
            setCurrentAudioUri(audioUriCurrent)
        }
        var mediaPlayerWUri: MediaPlayerWUri? = getMediaPlayerWUri(songID)
        if (mediaPlayerWUri == null) {
            try {
                mediaPlayerWUri = MediaPlayerWUri(
                    mediaPlayerManager,
                    MediaPlayer.create(context, AudioUri.getUri(songID)),
                    songID
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayerWUri?.let { addMediaPlayerWUri(it) }
        }
        if (requestAudioFocus(context)) {
            mediaPlayerWUri?.shouldPlay(true)
            setIsPlaying(true)
            _songInProgress.value = true
        }
    }

    fun seekTo(context: Context, progress: Int) {
        currentAudioUri.value?.let {
            getCurrentMediaPlayerWUri()?.seekTo(context, it, progress)
        }
    }

}