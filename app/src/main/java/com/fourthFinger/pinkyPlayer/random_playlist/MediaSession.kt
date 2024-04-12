package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import android.content.Intent
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ServiceMain

class MediaSession private constructor(
    private val playlistsRepo: PlaylistsRepo,
    private val mediaPlayerManager: MediaPlayerManager,
    val songQueue: SongQueue
) {

    private var currentPlaylist: RandomPlaylist = playlistsRepo.getMasterPlaylist()

    fun getCurrentPlaylist(): RandomPlaylist {
        return currentPlaylist
    }

    fun setCurrentPlaylist(currentPlaylist: RandomPlaylist) {
        this.currentPlaylist = currentPlaylist
    }

    fun setCurrentPlaylistToMaster() {
        setCurrentPlaylist(playlistsRepo.getMasterPlaylist())
    }

    fun resetProbabilities(context: Context) {
        currentPlaylist.resetProbabilities(context, playlistsRepo)
    }

    fun lowerProbabilities(context: Context, lowerProb: Double) {
        currentPlaylist.lowerProbabilities(context, playlistsRepo, lowerProb)
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
    fun setShuffling(context: Context, shuffling: Boolean) {
        this.shuffling = shuffling
        val songList = currentPlaylist.getSongIDs().toMutableList()
        if (shuffling) {
            if (looping) {
                restartLoopingShuffle()
            } else {
                // Not looping
                val song = currentPlaylist.nextRandomSong(context)
                if (song != null) {
                    songQueue.newSessionStarted(song.id)
                }
            }
        } else {
            // Not shuffling
            restartLoopingNonShuffle()
            if (songQueue.hasPrevious()) {
                val i = songList.indexOf(songQueue.previous().id)
                if (i != -1) {
                    for (j in 0 until i) {
                        songQueue.next()
                    }
                }
            }
        }
    }

    private fun restartLoopingNonShuffle() {
        val songList = currentPlaylist.getSongIDs().toMutableList()
        songQueue.newSessionStarted(songList[0])
        for (j in 1 until songList.size) {
            songQueue.addToQueue(songList[j])
        }
    }

    private fun restartLoopingShuffle() {
        val songList = currentPlaylist.getSongIDs().toMutableList()
        songList.shuffle()
        if (songList.isNotEmpty()) {
            if (songQueue.hasPrevious()) {
                val previous = songQueue.previous().id
                if (songList.contains(previous)) {
                    songQueue.newSessionStarted(previous)
                    songList.remove(previous)
                    songQueue.next()
                    songQueue.addToQueue(songList[0])
                } else {
                    songQueue.newSessionStarted(songList[0])
                }
            } else {
                songQueue.newSessionStarted(songList[0])
            }
            for (i in 1 until songList.size) {
                songQueue.addToQueue(songList[i])
            }
        }
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

    init {
        ServiceMain.executorServiceFIFO.execute {
            currentPlaylist = playlistsRepo.getMasterPlaylist()
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
        } else {
            loopSafePlayNextInQueue(context)
        }
        sendBroadcastNewSong(context)
    }

    /** Stops the current song only if there is a current song:
     * songInProgress will be false and
     * isPlaying will be false
     * if there is a current song.
     */
    private fun stopCurrentSong(context: Context) {
        mediaPlayerManager.stopCurrentSong(context)
    }

    /** Restarts the current song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was successfully restarted.
     * and for the broken MKV seek functionality.
     */
    private fun playLoopingOne(
        context: Context
    ) {
        mediaPlayerManager.playLoopingOne(context, mediaPlayerManager)
    }

    /** Plays the next song in the queue
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @return True if there was a song to play, else false.
     */
    private fun loopSafePlayNextInQueue(
        context: Context,
    ): Boolean {
        if (songQueue.hasNext()) {
            makeIfNeededAndPlay(context, songQueue.next())
            return true
        } else if (looping) {
            if (shuffling) {
                restartLoopingShuffle()
                makeIfNeededAndPlay(context, songQueue.next())
            } else {
                restartLoopingNonShuffle()
                makeIfNeededAndPlay(context, songQueue.next())
            }
            return true
        } else {
            // Not looping
            if(shuffling){
                val song = currentPlaylist.nextRandomSong(context)
                if (song != null) {
                    songQueue.addToQueue(song.id)
                }
                makeIfNeededAndPlay(context, songQueue.next())
                return true
            }
        }
        return false
    }

    /** Makes a [MediaPlayerWUri] for the song if one doesn't exist, and then plays the song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a [MediaPlayerWUri] was made, there is audio focus, and the song is playing.
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     * @param song The id of the song to make and play.
     */
    private fun makeIfNeededAndPlay(
        context: Context,
        song: Song
    ) {
        stopCurrentSong(context)
        mediaPlayerManager.makeIfNeededAndPlay(context, mediaPlayerManager, song.id)
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
            sendBroadcastNewSong(context)
        } else if(!playPreviousInQueue(context)){
            if (looping){
                if(shuffling){
                    restartLoopingShuffle()
                } else {
                    restartLoopingNonShuffle()
                }
                songQueue.goToBack()
                if(songQueue.hasPrevious()){
                    songQueue.previous()
                    playNext(context)
                }
            }
            sendBroadcastNewSong(context)
        }
    }

    /** Plays the previous song in the queue.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @return True if a song was played, else false.
     */
    private fun playPreviousInQueue(
        context: Context
    ): Boolean {
        if (songQueue.hasPrevious()) {
            songQueue.previous()
            if (songQueue.hasPrevious()) {
                makeIfNeededAndPlay(context, songQueue.previous())
                songQueue.next()
                return true
            }
        }
        songQueue.next()
        return false
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
    fun pauseOrPlay(
        context: Context
    ) {
        if (mediaPlayerManager.currentAudioUri.value == null) {
            playNext(context)
        } else {
            mediaPlayerManager.pauseOrPlay(context)
        }
    }

    fun seekTo(
        context: Context,
        progress: Int
    ) {
        mediaPlayerManager.seekTo(context, progress)
        if (mediaPlayerManager.isPlaying.value == false) {
            pauseOrPlay(context)
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: MediaSession? = null

        @Synchronized
        fun getInstance(
            playlistsRepo: PlaylistsRepo,
            mediaPlayerSession: MediaPlayerManager,
            songQueue: SongQueue
        ): MediaSession {
            if (INSTANCE == null) {
                INSTANCE = MediaSession(
                    playlistsRepo,
                    mediaPlayerSession,
                    songQueue
                )
            }
            return INSTANCE!!
        }
    }

}