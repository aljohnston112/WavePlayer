package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import android.content.Intent
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ServiceMain
import java.util.*

class MediaSession private constructor(context: Context) {

    private val mediaPlayerSession = MediaPlayerSession.getInstance(context)
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

    fun resetProbabilities(context: Context) {
        currentPlaylist?.resetProbabilities(context)
    }
    fun lowerProbabilities(context: Context) {
        currentPlaylist?.lowerProbabilities(context)
    }

    @Volatile
    private var shuffling: Boolean = true
    @Volatile
    private var looping: Boolean = false
    @Volatile
    private var loopingOne: Boolean = false

    @Synchronized
    fun isShuffling(): Boolean {
        return shuffling
    }

    @Synchronized
    fun setShuffling(shuffling: Boolean) {
        this.shuffling = shuffling
    }

    @Synchronized
    fun isLooping(): Boolean {
        return looping
    }

    @Synchronized
    fun setLooping(looping: Boolean) {
        this.looping = looping
    }

    @Synchronized
    fun isLoopingOne(): Boolean {
        return loopingOne
    }

    @Synchronized
    fun setLoopingOne(loopingOne: Boolean) {
        this.loopingOne = loopingOne
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

    /** Stops the current song only if there is a current song:
     * songInProgress will be false and
     * isPlaying will be false
     * if there is a current song.
     */
    private fun stopCurrentSong(context: Context) {
        mediaPlayerSession.stopCurrentSong(context)
    }

    /** Restarts the current song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was successfully restarted.
     * and for the broken MKV seek functionality.
     */
    private fun playLoopingOne(context: Context) {
        mediaPlayerSession.playLoopingOne(context)
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
            }
        }
        // The implementation is left to playNextInPlaylist
        return false
    }

    /** Makes a [MediaPlayerWUri] for the song if one doesn't exist, and then plays the song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a [MediaPlayerWUri] was made, there is audio focus, and the song is playing.
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     * @param song The id of the song to make and play.
     */
    private fun makeIfNeededAndPlay(context: Context, song: Song) {
        stopCurrentSong(context)
        currentPlaylist?.setIndexTo(song.id)
        mediaPlayerSession.makeIfNeededAndPlay(context, song.id)
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
            songQueue.addToQueue(context, audioUriCurrent.id)
            makeIfNeededAndPlay(context, songQueue.next())
            mediaPlayerSession.setCurrentAudioUri(audioUriCurrent)
        } else {
            mediaPlayerSession.setIsPlaying(false)
        }
    }

    private fun sendBroadcastNewSong(context: Context) {
        val intent = Intent()
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.action = context.resources.getString(
            R.string.action_new_song
        )
        context.sendBroadcast(intent)
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
        }
        sendBroadcastNewSong(context)
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
                    if (songQueue.hasPrevious()) {
                        makeIfNeededAndPlay(context, songQueue.previous())
                        songQueue.next()
                        return true
                    }
                }
            } else {
                return false
            }
        }
        return false
        // TODO what?
        // return !looping
    }

    /** Plays the previous song in the playlist.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     */
    private fun playPreviousInPlaylist(context: Context) {
        // TODO loop through playlist rather than relying on queue
        // This is clearly bugged
        val audioUriCurrent = currentPlaylist?.previous(context, random, looping, shuffling)
        if (audioUriCurrent != null) {
            mediaPlayerSession.setCurrentAudioUri(audioUriCurrent)
        }
        makeIfNeededAndPlay(context, songQueue.next())
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
        if(mediaPlayerSession.currentAudioUri.value == null){
            playNext(context)
        } else {
            mediaPlayerSession.pauseOrPlay(context)
        }
    }

    fun seekTo(context: Context, progress: Int) {
        mediaPlayerSession.seekTo(context, progress)
        if (mediaPlayerSession.isPlaying.value == false) {
            pauseOrPlay(context)
        }
    }

    companion object {
        private val random: Random = Random()
        @Volatile
        private var INSTANCE: MediaSession? = null

        @Synchronized
        fun getInstance(context: Context): MediaSession {
            if (INSTANCE == null) {
                INSTANCE = MediaSession(context)
            }
            return INSTANCE!!
        }
    }

    init {
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        ServiceMain.executorServiceFIFO.execute {
            currentPlaylist = playlistsRepo.getMasterPlaylist()
        }
        mediaPlayerSession.currentAudioUri.observeForever {
            // TODO why is this even here?
            // getMediaPlayerWUri(it.id)?.resetIfMKV(it, context)
            currentPlaylist?.setIndexTo(it.id)
            // songQueue.addToQueue(it.id)
        }
    }
}