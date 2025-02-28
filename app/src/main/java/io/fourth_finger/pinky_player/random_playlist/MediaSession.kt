package io.fourth_finger.pinky_player.random_playlist

import io.fourth_finger.pinky_player.R
import io.fourth_finger.playlist_data_source.PlaylistsRepo
import io.fourth_finger.playlist_data_source.RandomPlaylist
import io.fourth_finger.playlist_data_source.Song

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MediaSession(
    private val playlistsRepo: PlaylistsRepo,
    private val songRepo: SongRepo,
    private val mediaPlayerManager: MediaPlayerManager,
) {

    private val songQueue = SongQueue(songRepo)

    private val _currentlyPlayingPlaylist = MutableLiveData<RandomPlaylist?>()
    val currentlyPlayingPlaylist = _currentlyPlayingPlaylist as LiveData<RandomPlaylist?>

    val currentAudioUri = mediaPlayerManager.currentAudioUri
    val isPlaying = mediaPlayerManager.isPlaying
    val songInProgress = mediaPlayerManager.songInProgress
    val songList = songQueue.songQueue

    @Volatile
    private var shuffling: Boolean = true

    @Volatile
    private var looping: Boolean = false

    @Volatile
    private var loopingOne: Boolean = false

    fun setCurrentPlaylist(currentPlaylist: RandomPlaylist?) {
        this._currentlyPlayingPlaylist.postValue(currentPlaylist)
    }

    fun setCurrentPlaylistToMaster() {
        this._currentlyPlayingPlaylist.postValue(playlistsRepo.getMasterPlaylist())
    }

    fun resetProbabilities(context: Context) {
        currentlyPlayingPlaylist.value?.let {
            playlistsRepo.resetProbabilities(
                context,
                it
            )
        }
    }

    fun lowerProbabilities(
        context: Context,
        lowestProbability: Double
    ) {
        currentlyPlayingPlaylist.value?.let {
            playlistsRepo.lowerProbabilities(
                context,
                it,
                lowestProbability
            )
        }
    }

    fun getCurrentTimeOfCurrentMedia(): Int {
        return mediaPlayerManager.getCurrentTimeOfCurrentMedia()
    }

    @Synchronized
    fun isShuffling(): Boolean {
        return shuffling
    }

    private fun setUpShuffle(startFrom: Int = -1) {
        val songList = currentlyPlayingPlaylist.value?.getSongIDs()?.toMutableList()
        if (songList == null || songList.isEmpty()) {
            return
        }

        if (startFrom != -1) {
            val songToStartAt = songList[startFrom]
            songQueue.clearQueueAndAddSong(songToStartAt)
            songList.remove(songToStartAt)
        } else if (songQueue.hasPrevious()) {
            val previous = songQueue.previous().id
            if (songList.contains(previous)) {
                // Current playing song counts as first song in the shuffle
                songQueue.clearQueueAndAddSong(previous)
                songQueue.next()
                songList.remove(previous)
            } else {
                songQueue.clearSongQueue()
            }
        } else {
            songQueue.clearSongQueue()
        }

        // Add the rest of the shuffled music to the queue
        songList.shuffle()
        for (i in 0 until songList.size) {
            songQueue.addToQueue(songList[i])
        }

    }

    private fun startNonShuffle(index: Int) {
        val songList = currentlyPlayingPlaylist.value?.getSongIDs()?.toMutableList()
        if (songList != null) {
            songQueue.clearSongQueue()
            for (j in 0 until songList.size) {
                songQueue.addToQueue(songList[j])
            }
            (0 until index).forEach { _ ->
                songQueue.next()
            }
        }
    }

    @Synchronized
    fun setShuffling(
        shuffling: Boolean
    ) {
        this.shuffling = shuffling
        if (shuffling) {
            setUpShuffle()
        } else {
            if (songQueue.hasPrevious()) {
                val songList = currentlyPlayingPlaylist.value?.getSongIDs()?.toMutableList()
                if (songList != null) {
                    startNonShuffle(songList.indexOf(songQueue.previous().id) + 1)
                }
            } else {
                startNonShuffle(0)
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

    /** Stops the current song only if there is a current song:
     * songInProgress will be false and
     * isPlaying will be false
     * if there is a current song.
     */
    private fun stopCurrentSong(context: Context) {
        mediaPlayerManager.stopCurrentSong(context)
    }


    /** Makes a [MediaPlayerWithUri] for the song if one doesn't exist, and then plays the song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a [MediaPlayerWithUri] was made, there is audio focus, and the song is playing.
     * @param context Context used to request audio focus and make a MediaPlayer if needed.
     * @param song The id of the song to make and play.
     */
    private fun makeIfNeededAndPlay(
        context: Context,
        song: Song
    ) {
        stopCurrentSong(context)
        mediaPlayerManager.makeIfNeededAndPlay(
            context,
            mediaPlayerManager,
            song.id
        )
    }

    /** Restarts the current song.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was successfully restarted.
     * and for the broken MKV seek functionality.
     */
    private fun restartCurrentSong(
        context: Context
    ) {
        mediaPlayerManager.restartCurrentSong(
            context,
            mediaPlayerManager
        )
    }

    /** Plays the next song in the queue
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     * @return True if there was a song to play, else false.
     */
    private fun playNextInQueue(
        context: Context,
    ): Boolean {
        var playedNext = false
        if (songQueue.hasNext()) {
            makeIfNeededAndPlay(
                context,
                songQueue.next()
            )
            playedNext = true
        } else if (looping) {
            songQueue.goToFront()
            makeIfNeededAndPlay(context, songQueue.next())
            playedNext = true
        } else {
            // End of queue and not looping
            if (shuffling) {
                val songList = currentlyPlayingPlaylist.value?.getSongIDs()?.toMutableList()
                songList?.shuffle()
                songList?.forEach {
                    songQueue.addToQueue(it)
                }
                makeIfNeededAndPlay(
                    context,
                    songQueue.next()
                )
                playedNext = true
            }
        }
        return playedNext
    }

    private fun sendBroadcastSongCompleted(context: Context) {
        val intent = Intent()
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.action = context.resources.getString(
            R.string.action_new_song
        )
        context.sendBroadcast(intent)
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

    /** Plays the next song.
     * First, if looping one the current song wil start over.
     * Second, if the queue can play a song, that will be played.
     * Third, if the playlist can play a song, that will be played.
     * songInProgress will be true and
     * isPlaying will be true
     * if a song was played
     */
    fun playNext(context: Context, songClicked: Boolean = false) {
        stopCurrentSong(context)
        if (loopingOne && !songClicked) {
            restartCurrentSong(context)
        } else {
            playNextInQueue(context)
        }
        sendBroadcastSongCompleted(context)
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
            restartCurrentSong(context)
            sendBroadcastSongCompleted(context)
        } else if (!playPreviousInQueue(context)) {
            if (looping) {
                if (shuffling) {
                    setUpShuffle()
                } else {
                    val songList = currentlyPlayingPlaylist.value?.getSongIDs()?.toMutableList()
                    var i = songList?.indexOf(currentAudioUri.value!!.id)!!
                    if (i == songList.size - 1) {
                        i = 0
                    }
                    startNonShuffle(i)
                }
                songQueue.goToBack()
                if (songQueue.hasPrevious()) {
                    songQueue.previous()
                    playNext(context)
                }
            }
            sendBroadcastSongCompleted(context)
        }
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
            mediaPlayerManager.togglePlay(context)
        }
    }

    fun seekTo(
        context: Context,
        progress: Int
    ) {
        mediaPlayerManager.seekTo(
            context,
            progress
        )
        if (mediaPlayerManager.isPlaying.value == false) {
            pauseOrPlay(context)
        }
    }

    fun addToQueueAndStartIfNeeded(
        context: Context,
        songID: Long
    ) {
        songQueue.addToQueue(songID)
        if (songInProgress.value == false) {
            playNext(context)
        }
    }

    fun addToQueueAndStartIfNeeded(
        context: Context,
        randomPlaylist: RandomPlaylist
    ) {
        // TODO popup asking if the songs should be shuffled?
        val songs = randomPlaylist.getSongs()
        for (song in songs) {
            songQueue.addToQueue(song.id)
        }
        if (songInProgress.value == false) {
            playNext(context)
        }
    }

    fun startFromIndex(
        context: Context,
        index: Int
    ) {
        val song = songQueue.setIndex(index)
        if (song == currentAudioUri.value?.id?.let {
                songRepo.getSong(it)
            }
        ) {
            seekTo(
                context,
                0
            )
        }
        playNext(context)
    }

    fun startPlaylistFromSong(
        context: Context,
        id: Long
    ) {
        val songList = currentlyPlayingPlaylist.value?.getSongIDs()?.toMutableList()
        if (songList != null) {
            val index = songList.indexOf(id)
            if (shuffling) {
                setUpShuffle(index)
            } else {
                startNonShuffle(index)
            }
        }
        playNext(context, true)
    }

    fun notifyItemInserted(position: Int) {
        songQueue.notifyItemInserted(position)
    }

    fun notifySongRemoved(position: Int): Boolean {
        return songQueue.notifySongRemoved(position)
    }

    fun notifySongMoved(
        from: Int,
        to: Int
    ) {
        songQueue.notifySongMoved(from, to)
    }

    fun cleanUp(context: Context) {
        if (isPlaying.value == true) {
            pauseOrPlay(context)
        }
        mediaPlayerManager.cleanUp()
    }

}
