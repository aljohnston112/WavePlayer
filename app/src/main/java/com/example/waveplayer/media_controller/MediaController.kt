package com.example.waveplayer.media_controller

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.waveplayer.R
import com.example.waveplayer.random_playlist.AudioUri
import com.example.waveplayer.random_playlist.RandomPlaylist
import com.example.waveplayer.random_playlist.SongQueue
import java.util.*
import java.util.concurrent.Callable

class MediaController private constructor(val context: Context) {

    val onCompletionListener: OnCompletionListener
    private val mediaData: MediaData = MediaData.getInstance(context)
    private val songQueue: SongQueue = SongQueue.getInstance()
    private var currentPlaylist: RandomPlaylist? = null

    private var _currentAudioUri = MutableLiveData<AudioUri>()
    val currentAudioUri = _currentAudioUri as LiveData<AudioUri>


    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData()
    val isPlaying = _isPlaying as LiveData<Boolean>

    private var haveAudioFocus: Boolean = false
    private var songInProgress: Boolean = false
    private var shuffling: Boolean = true
    private var looping: Boolean = false
    private var loopingOne: Boolean = false

    private fun sendBroadcastNewSong() {
        val intent = Intent()
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.action = context.resources.getString(
                R.string.broadcast_receiver_action_new_song)
        context.sendBroadcast(intent)
    }

    fun getCurrentPlaylist(): RandomPlaylist? {
        return currentPlaylist
    }

    fun setCurrentPlaylist(currentPlaylist: RandomPlaylist) {
        this.currentPlaylist = currentPlaylist
    }

    fun setCurrentPlaylistToMaster() {
        mediaData.getMasterPlaylist()?.let { setCurrentPlaylist(it) }
    }

    fun clearProbabilities(context: Context) {
        currentPlaylist?.clearProbabilities(context)
    }

    fun lowerProbabilities(context: Context) {
        currentPlaylist?.lowerProbabilities(context, mediaData.getLowerProb())
    }

    fun getCurrentUri(): Uri? {
        return currentAudioUri.value?.getUri()
    }

    fun getCurrentTime(): Int {
        return getCurrentMediaPlayerWUri()?.getCurrentPosition() ?: -1
    }

    fun getCurrentMediaPlayerWUri(): MediaPlayerWUri? {
        if (_currentAudioUri.value != null) {
            return _currentAudioUri.value?.id?.let { mediaData.getMediaPlayerWUri(it) }
        }
        return null
    }

    fun releaseMediaPlayers() {
        mediaData.releaseMediaPlayers()
        songInProgress = false
        _isPlaying.postValue(false)
    }

    fun isSongInProgress(): Boolean {
        return songInProgress
    }

    fun setIsPlaying(isPlaying: Boolean) {
        this._isPlaying.postValue(isPlaying)
    }

    fun isShuffling(): Boolean {
        return shuffling
    }

    fun setShuffling(shuffling: Boolean) {
        this.shuffling = shuffling
    }

    fun isLooping(): Boolean {
        return looping
    }

    fun setLooping(looping: Boolean) {
        this.looping = looping
    }

    fun isLoopingOne(): Boolean {
        return loopingOne
    }

    fun setLoopingOne(loopingOne: Boolean) {
        this.loopingOne = loopingOne
    }

    /** If a song is playing, it will be paused:
     * songInProgress will be unchanged and
     * isPlaying will be false
     * if a song if paused.
     * If a song is started and/or paused, but not playing, it will be played:
     * songInProgress will be true and
     * isPlaying will be true
     * if a song is played
     * If there is no song in progress, nothing will be done.
     * @param context Context used to request audio focus if needed
     */
    fun pauseOrPlay() {
        if (_currentAudioUri.value != null) {
            val mediaPlayerWURI: MediaPlayerWUri? = getCurrentMediaPlayerWUri()
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.pause()
                    _isPlaying.postValue(false)
                } else {
                    if (requestAudioFocus(context)) {
                        mediaPlayerWURI.shouldPlay(true)
                        _isPlaying.postValue(true)
                        songInProgress = true
                    }
                }
            }
        }
    }

    /** Stops the current song only if there is a current song:
     * songInProgress will be false and
     * isPlaying will be false
     * if there is a current song.
     */
    private fun stopCurrentSong() {
        _currentAudioUri.value?.id?.let { id ->
            mediaData.getMediaPlayerWUri(id)?.let {
                if (it.isPrepared() && it.isPlaying()) {
                    _currentAudioUri.value?.let { auri ->
                        it.stop(context, auri)
                        it.prepareAsync()
                    }
                }
            }
        } ?: releaseMediaPlayers()
        songInProgress = false
        _isPlaying.postValue(false)
    }

    /** Plays the next song.
     * First, if looping one the current song wil start over.
     * Second, if the queue can play a song, that will be played.
     * Third, if the playlist can play a song, that will be played.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @param context The context used for making a MediaPlayer if needed,
     * and for the broken MKV seeking.
     */
    fun playNext() {
        stopCurrentSong()
        if (loopingOne) {
            playLoopingOne()
        } else if (!playNextInQueue()) {
            playNextInPlaylist()
        }
        sendBroadcastNewSong()
    }

    /** Plays the previous song.
     * First, if looping one the current song wil start over.
     * Second, if the queue can play a song, that will be played.
     * Third, if the playlist can play a song, that will be played.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @param context The context used for making a MediaPlayer if needed,
     * and for the broken MKV seeking.
     */
    fun playPrevious() {
        if (loopingOne) {
            playLoopingOne()
        } else if (!playPreviousInQueue()) {
            playPreviousInPlaylist()
        } else {
            songInProgress = true
            _isPlaying.postValue(true)
        }
        sendBroadcastNewSong()
    }

    /** Makes a [MediaPlayerWUri] for the song if one doesn't exist, and then plays the song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a [MediaPlayerWUri] was made, there is audio focus, and the song is playing.
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     * @param songID The id of the song to make and play.
     */
    private fun makeIfNeededAndPlay(context: Context, songID: Long) {
        _currentAudioUri.value = AudioUri.getAudioUri(context, songID)
        currentPlaylist?.setIndexTo(songID)
        var mediaPlayerWUri: MediaPlayerWUri? = mediaData.getMediaPlayerWUri(songID)
        if (mediaPlayerWUri == null) {
            try {
                mediaPlayerWUri = (Callable {
                    val mediaPlayerWURI = MediaPlayerWUri(
                            context,
                            MediaPlayer.create(context, AudioUri.getUri(songID)),
                            songID
                    )
                    mediaPlayerWURI.setOnCompletionListener(onCompletionListener)
                    mediaPlayerWURI
                } as Callable<MediaPlayerWUri>).call()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayerWUri?.let { mediaData.addMediaPlayerWUri(it) }
        }
        stopCurrentSong()
        if (requestAudioFocus(context)) {
            mediaPlayerWUri?.shouldPlay(true)
            _isPlaying.postValue(true)
            songInProgress = true
        }
    }

    /** Plays the next song in the queue
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     * @return True if there was a song to play, else false.
     */
    private fun playNextInQueue(): Boolean {
        if (songQueue.hasNext()) {
            makeIfNeededAndPlay(context, songQueue.next())
            return true
        } else if (looping) {
            if (shuffling) {
                songQueue.goToFront()
                if (songQueue.hasNext()) {
                    makeIfNeededAndPlay(context, songQueue.next())
                    return true
                }
            } else {
                return false
            }
        } else if (shuffling) {
            return if (songQueue.hasNext()) {
                makeIfNeededAndPlay(context, songQueue.next())
                _currentAudioUri.value = currentPlaylist?.next(context, random)
                _currentAudioUri.value?.id?.let { currentPlaylist?.setIndexTo(it) }
                _currentAudioUri.value?.id?.let { songQueue.addToQueue(it) }
                true
            } else {
                false
            }
        }
        return false
    }

    /** Plays the next song in the current playlist if there is one.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played.
     * songInProgress will be false and
     * isPlaying will be false
     * if the playlist did not have a song to play.
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     */
    private fun playNextInPlaylist() {
        val cau = currentPlaylist?.next(context, random, looping, shuffling)
        if (cau != null) {
            currentPlaylist?.setIndexTo(cau.id)
            songQueue.addToQueue(cau.id)
            makeIfNeededAndPlay(context, songQueue.next())
            _currentAudioUri.value = cau as AudioUri
        } else {
            _isPlaying.postValue(false)
            songInProgress = false
        }
    }

    /** Plays the previous song in the queue.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     * @return True if a song was played, else false.
     */
    private fun playPreviousInQueue(): Boolean {
        if (songQueue.hasPrevious()) {
            songQueue.previous()
            if (songQueue.hasPrevious()) {
                makeIfNeededAndPlay(context, songQueue.previous())
                songQueue.next()
                return true
            } else if (looping) {
                if (shuffling) {
                    songQueue.goToBack()
                    return if (songQueue.hasPrevious()) {
                        makeIfNeededAndPlay(context, songQueue.previous())
                        songQueue.next()
                        true
                    } else {
                        false
                    }
                }
            } else {
                return false
            }
        }
        return !looping
    }

    /** Plays the previous song in the playlist.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     */
    private fun playPreviousInPlaylist() {
        _currentAudioUri.value = currentPlaylist?.previous(context, random, looping, shuffling)
        _currentAudioUri.value?.id?.let { currentPlaylist?.setIndexTo(it) }
        _currentAudioUri.value?.id?.let { songQueue.addToQueue(it) }
        makeIfNeededAndPlay(context, songQueue.next())
    }

    /** Restarts the current song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was successfully restarted.
     * @param context Context used to request audio focus, make a MediaPlayer if needed,
     * and for the broken MKV seek functionality.
     */
    private fun playLoopingOne() {
        val mediaPlayerWURI: MediaPlayerWUri? = getCurrentMediaPlayerWUri()
        if (mediaPlayerWURI != null) {
            _currentAudioUri.value?.let { mediaPlayerWURI.seekTo(context, it, 0) }
            mediaPlayerWURI.shouldPlay(true)
            _isPlaying.postValue(true)
            songInProgress = true
            // TODO make a setting?
            //addToQueueAtCurrentIndex(currentSong.getUri());
        } else {
            _currentAudioUri.value?.id?.let { makeIfNeededAndPlay(context, it) }
        }
    }

    fun seekTo(progress: Int) {
        _currentAudioUri.value?.let { getCurrentMediaPlayerWUri()?.seekTo(context, it, progress) }
        if (_isPlaying.value == false) {
            pauseOrPlay()
        }
    }

    private fun requestAudioFocus(context: Context): Boolean {
        if (haveAudioFocus) {
            return true
        }
        val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val onAudioFocusChangeListener: OnAudioFocusChangeListener = object : OnAudioFocusChangeListener {
            val lock: Any = Any()
            var wasPlaying: Boolean = false
            override fun onAudioFocusChange(i: Int) {
                when (i) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        synchronized(lock) {
                            haveAudioFocus = true
                            if (_isPlaying.value == false && wasPlaying) {
                                pauseOrPlay()
                            }
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        synchronized(lock) {
                            haveAudioFocus = false
                            if (_isPlaying.value == true) {
                                wasPlaying = true
                                pauseOrPlay()
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
                    AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                    .build()
            result = audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            result = audioManager.requestAudioFocus(
                    onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        haveAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    companion object {
        private val random: Random = Random()
        private var INSTANCE: MediaController? = null

        @Synchronized
        fun getInstance(context: Context): MediaController {
            if (INSTANCE == null) {
                INSTANCE = MediaController(context)
            }
            return INSTANCE!!
        }
    }

    init {
        onCompletionListener = OnCompletionListener {
            playNext()
            val audioUri: AudioUri? = currentAudioUri.value
            audioUri?.id?.let { it1 -> MediaData.getInstance(context).getMediaPlayerWUri(it1)?.resetIfMKV(audioUri, context) }
            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = context.resources.getString(R.string.broadcast_receiver_action_new_song)
            context.sendBroadcast(intent)
        }
        ServiceMain.executorServiceFIFO.execute { currentPlaylist = mediaData.getMasterPlaylist() }
    }
}