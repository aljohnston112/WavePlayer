package com.fourthFinger.pinkyPlayer.media_controller

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.Build
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.fragments.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.SettingsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.SongQueue
import java.util.*
import java.util.concurrent.Callable

class MediaModel private constructor(context: Context) {

    private val mediaPlayerModel = MediaPlayerModel.getInstance()

    private val onCompletionListener: OnCompletionListener
    fun getOnCompletionListener(): OnCompletionListener {
        return onCompletionListener
    }

    private val songQueue: SongQueue = SongQueue.getInstance()

    private var currentPlaylist: RandomPlaylist? = null
    fun getCurrentPlaylist(): RandomPlaylist? {
        return currentPlaylist
    }

    fun setCurrentPlaylist(currentPlaylist: RandomPlaylist) {
        this.currentPlaylist = currentPlaylist
    }

    fun setCurrentPlaylistToMaster(context: Context) {
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        playlistsRepo.getMasterPlaylist()?.let { setCurrentPlaylist(it) }
    }

    private var haveAudioFocus: Boolean = false
    private var songInProgress: Boolean = false
    private var shuffling: Boolean = true
    private var looping: Boolean = false
    private var loopingOne: Boolean = false

    private fun sendBroadcastNewSong(context: Context) {
        val intent = Intent()
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.action = context.resources.getString(
            R.string.broadcast_receiver_action_new_song
        )
        context.sendBroadcast(intent)
    }

    fun clearProbabilities(context: Context) {
        currentPlaylist?.clearProbabilities(context)
    }

    fun lowerProbabilities(context: Context) {
        currentPlaylist?.lowerProbabilities(context, SettingsRepo.getInstance().getLowerProb())
    }

    fun isSongInProgress(): Boolean {
        return songInProgress
    }

    fun releaseMediaPlayers() {
        mediaPlayerModel.releaseMediaPlayers()
        songInProgress = false
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
     */
    fun pauseOrPlay(context: Context) {
        val mediaPlayerWURI: MediaPlayerWUri? = mediaPlayerModel.getCurrentMediaPlayerWUri()
        if (mediaPlayerWURI != null) {
            if (mediaPlayerWURI.isPrepared() && mediaPlayerWURI.isPlaying()) {
                mediaPlayerWURI.pause()
                mediaPlayerModel.setIsPlaying(false)
            } else {
                if (requestAudioFocus(context)) {
                    mediaPlayerWURI.shouldPlay(true)
                    mediaPlayerModel.setIsPlaying(true)
                    songInProgress = true
                }
            }
        }
    }

    /** Stops the current song only if there is a current song:
     * songInProgress will be false and
     * isPlaying will be false
     * if there is a current song.
     */
    private fun stopCurrentSong(context: Context) {
        mediaPlayerModel.currentAudioUri.value?.let { a ->
            a.id.let { it2 ->
                mediaPlayerModel.getMediaPlayerWUri(it2)?.let { it ->
                if (it.isPrepared() && it.isPlaying()) {
                    it.stop(context, a)
                    it.prepareAsync()
                }
            }
        } ?: releaseMediaPlayers()
            songInProgress = false
            mediaPlayerModel.setIsPlaying(false)
        }
    }

    /** Plays the next song.
     * First, if looping one the current song wil start over.
     * Second, if the queue can play a song, that will be played.
     * Third, if the playlist can play a song, that will be played.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     */
    fun playNext(context: Context) {
        stopCurrentSong(context)
        if (loopingOne) {
            playLoopingOne(context)
        } else if (!playNextInQueue(context)) {
            playNextInPlaylist(context)
        }
        sendBroadcastNewSong(context)
    }

    /** Plays the previous song.
     * First, if looping one the current song wil start over.
     * Second, if the queue can play a song, that will be played.
     * Third, if the playlist can play a song, that will be played.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     */
    fun playPrevious(context: Context) {
        if (loopingOne) {
            playLoopingOne(context)
        } else if (!playPreviousInQueue(context)) {
            playPreviousInPlaylist(context)
        } else {
            songInProgress = true
            mediaPlayerModel.setIsPlaying(true)
        }
        sendBroadcastNewSong(context)
    }

    /** Makes a [MediaPlayerWUri] for the song if one doesn't exist, and then plays the song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a [MediaPlayerWUri] was made, there is audio focus, and the song is playing.
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     * @param songID The id of the song to make and play.
     */
    private fun makeIfNeededAndPlay(context: Context, songID: Long) {
        stopCurrentSong(context)
        val audioUriCurrent = AudioUri.getAudioUri(context, songID)
        if(audioUriCurrent != null) {
            mediaPlayerModel.setCurrentAudioUri(audioUriCurrent)
        }
        currentPlaylist?.setIndexTo(songID)
        var mediaPlayerWUri: MediaPlayerWUri? = mediaPlayerModel.getMediaPlayerWUri(songID)
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
            mediaPlayerWUri?.let { mediaPlayerModel.addMediaPlayerWUri(it) }
        }
        if (requestAudioFocus(context)) {
            mediaPlayerWUri?.shouldPlay(true)
            mediaPlayerModel.setIsPlaying(true)
            songInProgress = true
        }
    }

    /** Plays the next song in the queue
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @return True if there was a song to play, else false.
     */
    private fun playNextInQueue(context: Context): Boolean {
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
                val audioUriCurrent = currentPlaylist?.next(context, random)
                if (audioUriCurrent != null) {
                    mediaPlayerModel.setCurrentAudioUri(audioUriCurrent)
                }
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
     */
    private fun playNextInPlaylist(context: Context) {
        val audioUriCurrent = currentPlaylist?.next(context, random, looping, shuffling)
        if (audioUriCurrent != null) {
            currentPlaylist?.setIndexTo(audioUriCurrent.id)
            songQueue.addToQueue(audioUriCurrent.id)
            makeIfNeededAndPlay(context, songQueue.next())
            mediaPlayerModel.setCurrentAudioUri(audioUriCurrent)
        } else {
            mediaPlayerModel.setIsPlaying(false)
            songInProgress = false
        }
    }

    /** Plays the previous song in the queue.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @return True if a song was played, else false.
     */
    private fun playPreviousInQueue(context: Context): Boolean {
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
     */
    private fun playPreviousInPlaylist(context: Context) {
        val audioUriCurrent = currentPlaylist?.previous(context, random, looping, shuffling)
        if (audioUriCurrent != null) {
            mediaPlayerModel.setCurrentAudioUri(audioUriCurrent)
        }
        makeIfNeededAndPlay(context, songQueue.next())
    }

    /** Restarts the current song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was successfully restarted.
     * and for the broken MKV seek functionality.
     */
    private fun playLoopingOne(context: Context) {
        val mediaPlayerWURI: MediaPlayerWUri? = mediaPlayerModel.getCurrentMediaPlayerWUri()
        if (mediaPlayerWURI != null) {
            mediaPlayerModel.currentAudioUri.value?.let {
                mediaPlayerWURI.seekTo(context, it, 0)
                mediaPlayerWURI.shouldPlay(true)
                mediaPlayerModel.setIsPlaying(true)
                songInProgress = true
                // TODO make a setting?
                //addToQueueAtCurrentIndex(currentSong.getUri());
            }
        } else {
            mediaPlayerModel.currentAudioUri.value?.let { makeIfNeededAndPlay(context, it.id) }
        }
    }

    fun seekTo(context: Context, progress: Int) {
        mediaPlayerModel.currentAudioUri.value?.let {
            mediaPlayerModel.getCurrentMediaPlayerWUri()?.seekTo(context, it, progress)
        }
        if (mediaPlayerModel.isPlaying.value == false) {
            pauseOrPlay(context)
        }
    }

    private fun requestAudioFocus(context: Context): Boolean {
        if (haveAudioFocus) {
            return true
        }
        val audioManager: AudioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val onAudioFocusChangeListener: OnAudioFocusChangeListener =
            object : OnAudioFocusChangeListener {
                val lock: Any = Any()
                var wasPlaying: Boolean = false
                override fun onAudioFocusChange(i: Int) {
                    when (i) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            synchronized(lock) {
                                haveAudioFocus = true
                                if (mediaPlayerModel.isPlaying.value == false && wasPlaying) {
                                    pauseOrPlay(context)
                                }
                            }
                        }
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            synchronized(lock) {
                                haveAudioFocus = false
                                if (mediaPlayerModel.isPlaying.value == true) {
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
                onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }
        haveAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    companion object {
        private val random: Random = Random()
        private var INSTANCE: MediaModel? = null

        @Synchronized
        fun getInstance(context: Context): MediaModel {
            if (INSTANCE == null) {
                INSTANCE = MediaModel(context)
            }
            return INSTANCE!!
        }
    }

    init {
        // TODO store somewhere else
        onCompletionListener = OnCompletionListener {
            playNext(context)
            mediaPlayerModel.currentAudioUri.observeForever {
                mediaPlayerModel.getMediaPlayerWUri(it.id)?.resetIfMKV(it, context)
                currentPlaylist?.setIndexTo(it.id)
                songQueue.addToQueue(it.id)
            }
            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = context.resources.getString(R.string.broadcast_receiver_action_new_song)
            context.sendBroadcast(intent)
        }
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        ServiceMain.executorServiceFIFO.execute { currentPlaylist = playlistsRepo.getMasterPlaylist() }
    }
}