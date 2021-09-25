package com.fourthFinger.pinkyPlayer.media_controller

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import java.util.*
import java.util.concurrent.Callable

class MediaPlayerSession private constructor(context: Context) {

    private var haveAudioFocus: Boolean = false
    private var songInProgress: Boolean = false
    fun isSongInProgress(): Boolean {
        return songInProgress
    }

    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying = _isPlaying as LiveData<Boolean>
    fun setIsPlaying(isPlaying: Boolean) {
        this._isPlaying.postValue(isPlaying)
        songInProgress = isPlaying
    }

    private var _currentAudioUri = MutableLiveData<AudioUri>()
    val currentAudioUri = _currentAudioUri as LiveData<AudioUri>
    fun setCurrentAudioUri(audioUri: AudioUri) {
        _currentAudioUri.postValue(audioUri)
    }

    fun getCurrentTime(): Int {
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
        songInProgress = false
    }

    fun cleanUp(context: Context) {
        val mediaSession = MediaSession.getInstance(context)
        if (isPlaying.value == true) {
            mediaSession.pauseOrPlay(context)
        }
        releaseMediaPlayers()
    }

    val onCompletionListener: MediaPlayer.OnCompletionListener =
        MediaPlayer.OnCompletionListener {
            val mediaSession = MediaSession.getInstance(context)
            mediaSession.playNext(context)
            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = context.resources.getString(R.string.action_new_song)
            context.sendBroadcast(intent)
        }

    var onErrorListener: MediaPlayer.OnErrorListener
     = MediaPlayer.OnErrorListener { _: MediaPlayer?, _: Int, _: Int ->
        synchronized(MediaPlayerWUri.lock) {
            val mediaSession: MediaSession = MediaSession.getInstance(context)
            releaseMediaPlayers()
            if (!isSongInProgress()) {
                mediaSession.playNext(context)
            }
            return@OnErrorListener false
        }
    }

    private fun requestAudioFocus(context: Context): Boolean {
        if (haveAudioFocus) {
            return true
        }
        val audioManager: AudioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener =
            object : AudioManager.OnAudioFocusChangeListener {
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
                .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
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
                    songInProgress = true
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
            }
        } ?: releaseMediaPlayers()
        songInProgress = false
        setIsPlaying(false)
    }

    fun playLoopingOne(context: Context) {
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
            currentAudioUri.value?.let { makeIfNeededAndPlay(context, it.id) }
        }
    }

    fun makeIfNeededAndPlay(context: Context, songID: Long) {
        val audioUriCurrent = AudioUri.getAudioUri(context, songID)
        if (audioUriCurrent != null) {
            setCurrentAudioUri(audioUriCurrent)
        }
        var mediaPlayerWUri: MediaPlayerWUri? = getMediaPlayerWUri(songID)
        if (mediaPlayerWUri == null) {
            try {
                mediaPlayerWUri = (Callable {
                    val mediaPlayerWURI = MediaPlayerWUri(
                        context,
                        MediaPlayer.create(context, AudioUri.getUri(songID)),
                        songID
                    )
                    mediaPlayerWURI
                } as Callable<MediaPlayerWUri>).call()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayerWUri?.let { addMediaPlayerWUri(it) }
        }
        if (requestAudioFocus(context)) {
            mediaPlayerWUri?.shouldPlay(true)
            setIsPlaying(true)
            songInProgress = true
        }
    }

    fun seekTo(context: Context, progress: Int) {
        currentAudioUri.value?.let {
            getCurrentMediaPlayerWUri()?.seekTo(context, it, progress)
        }
    }

    companion object {

        private var INSTANCE: MediaPlayerSession? = null

        fun getInstance(context: Context): MediaPlayerSession {
            if (INSTANCE == null) {
                INSTANCE = MediaPlayerSession(context)
            }
            return INSTANCE!!
        }

    }

}