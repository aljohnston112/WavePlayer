package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import android.content.Context
import androidx.annotation.GuardedBy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ToastUtil
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.media_controller.MediaModel
import com.fourthFinger.pinkyPlayer.media_controller.MediaPlayerModel
import com.fourthFinger.pinkyPlayer.media_controller.SaveFile
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import com.fourthFinger.pinkyPlayer.random_playlist.SongQueue
import java.util.*

class ViewModelPlaylists(application: Application) : AndroidViewModel(application) {

    private val playlistsRepo = PlaylistsRepo.getInstance(application)

    private val _playlistToAddToQueue = MutableLiveData<RandomPlaylist?>()
    val playlistToAddToQueue = _playlistToAddToQueue as LiveData<RandomPlaylist?>
    fun setPlaylistToAddToQueue(playlistToAddToQueue: RandomPlaylist?) {
        _playlistToAddToQueue.value = playlistToAddToQueue
    }

    private val _songToAddToQueue = MutableLiveData<Long?>()
    val songToAddToQueue = _songToAddToQueue as LiveData<Long?>
    fun setSongToAddToQueue(songToAddToQueue: Long?) {
        _songToAddToQueue.value = songToAddToQueue
    }

    fun getSongToAddToQueue(): Song? {
        return songToAddToQueue.value?.let { playlistsRepo.getSong(it) }
    }

    @GuardedBy("this")
    private val userPickedSongs: MutableList<Song> = ArrayList()

    @Synchronized
    fun getUserPickedSongs(): List<Song> {
        return userPickedSongs
    }

    @Synchronized
    fun songSelected(songs: Song) {
        userPickedSongs.add(songs)
    }

    @Synchronized
    fun songUnselected(song: Song) {
        userPickedSongs.remove(song)
    }

    @Synchronized
    fun clearUserPickedSongs() {
        for (song in userPickedSongs) {
            song.setSelected(false)
        }
        userPickedSongs.clear()
    }

    @GuardedBy("this")
    @Volatile
    private var userPickedPlaylist: RandomPlaylist? = null

    @Synchronized
    fun getUserPickedPlaylist(): RandomPlaylist? {
        return userPickedPlaylist
    }

    @Synchronized
    fun setUserPickedPlaylist(userPickedPlaylist: RandomPlaylist?) {
        this.userPickedPlaylist = userPickedPlaylist
    }

    fun getPlaylistTitles(): Array<String?> {
        return getPlaylistTitles(playlistsRepo.getPlaylists())
    }

    private fun getPlaylistTitles(randomPlaylists: List<RandomPlaylist>): Array<String?> {
        val titles: MutableList<String> = ArrayList(randomPlaylists.size)
        for (randomPlaylist in randomPlaylists) {
            titles.add(randomPlaylist.getName())
        }
        val titlesArray = arrayOfNulls<String>(titles.size)
        var i = 0
        for (title in titles) {
            titlesArray[i++] = title
        }
        return titlesArray
    }

    fun getPlaylists(): List<RandomPlaylist> {
        return playlistsRepo.getPlaylists()
    }

    fun addNewPlaylist(context: Context, randomPlaylist: RandomPlaylist) {
        playlistsRepo.addPlaylist(randomPlaylist)
        SaveFile.saveFile(context)
    }

    fun addNewPlaylist(context: Context, position: Int, randomPlaylist: RandomPlaylist) {
        playlistsRepo.addPlaylist(position, randomPlaylist)
        SaveFile.saveFile(context)
    }

    private fun addNewPlaylistWithUserPickedSongs(context: Context, name: String) {
        playlistsRepo.addPlaylist(
            RandomPlaylist(name, userPickedSongs, false)
        )
        clearUserPickedSongs()
        SaveFile.saveFile(context)
    }

    private fun removePlaylist(context: Context, userPickedPlaylist: RandomPlaylist) {
        playlistsRepo.removePlaylist(userPickedPlaylist)
        SaveFile.saveFile(context)
    }

    fun getSong(songID: Long): Song? {
        return playlistsRepo.getSong(songID)
    }

    fun getAllSongs(): List<Song>? {
        return playlistsRepo.getAllSongs()
    }

    fun getMasterPlaylist(): RandomPlaylist? {
        return playlistsRepo.getMasterPlaylist()
    }

    fun getPlaylist(it: String): RandomPlaylist? {
        return playlistsRepo.getPlaylist(it)
    }

    fun fragmentEditPlaylistViewCreated() {
        setPickedSongsToCurrentPlaylist()
    }

    @Synchronized
    private fun setPickedSongsToCurrentPlaylist() {
        userPickedPlaylist?.let {
            for (song in it.getSongs()) {
                songSelected(song)
            }
        }
    }

    private fun doesPlaylistExist(playlistName: String): Boolean {
        var playlistIndex = -1
        for ((i, randomPlaylist) in getPlaylists().withIndex()) {
            if (randomPlaylist.getName() == playlistName) {
                playlistIndex = i
            }
        }
        return playlistIndex == -1
    }

    fun fragmentEditPlaylistOnResume() {
        val makingNewPlaylist = (userPickedPlaylist == null)
        if (!makingNewPlaylist) {
            editingCurrentPlaylist()
        }
    }

    private fun editingCurrentPlaylist() {
        // userPickedSongs.isEmpty() when the user is editing a playlist
        // TODO
        //  if (user is editing a playlist,
        //      unselects all the songs, and
        //      returns here){
        //          ERROR
        //  }
        // TODO how come only when it is empty?
        if (userPickedSongs.isEmpty()) {
            getUserPickedPlaylist()?.getSongs()?.let {
                userPickedSongs.addAll(it)
            }
        }
    }

    fun editPlaylistFabClicked(context: Context, playlistName: String) {
        if (validateInput(context, playlistName)) {
            val makingNewPlaylist = (userPickedPlaylist == null)
            if (makingNewPlaylist) {
                addNewPlaylistWithUserPickedSongs(context, playlistName)
            } else if (!makingNewPlaylist) {
                makeNewPlaylist(context, playlistName)
            }
        }
    }

    private fun makeNewPlaylist(context: Context, playlistName: String) {
        val playlistNames = getPlaylistTitles()
        // TODO Is this a deep copy
        val finalPlaylist: RandomPlaylist = userPickedPlaylist!!
        if (finalPlaylist.getName() == playlistName ||
            !playlistNames.contains(playlistName)
        ) {
            finalPlaylist.setName(playlistName)
            for (song in finalPlaylist.getSongs()) {
                if (!userPickedSongs.contains(song)) {
                    finalPlaylist.remove(song)
                }
            }
            for (song in userPickedSongs) {
                finalPlaylist.add(song)
                song.setSelected(false)
            }
        }
        SaveFile.saveFile(context)
    }

    private fun validateInput(context: Context, playlistName: String): Boolean {
        val userPickedSongs = getUserPickedSongs().toMutableList()
        val userPickedPlaylist = getUserPickedPlaylist()
        val makingNewPlaylist = (userPickedPlaylist == null)
        return if (userPickedSongs.size == 0) {
            ToastUtil.showToast(context, R.string.not_enough_songs_for_playlist)
            false
        } else if (playlistName.isEmpty()) {
            ToastUtil.showToast(context, R.string.no_name_playlist)
            false
        } else if (
            doesPlaylistExist(playlistName) &&
            makingNewPlaylist
        ) {
            ToastUtil.showToast(context, R.string.duplicate_name_playlist)
            false
        } else {
            true
        }
    }

    fun notifySongMoved(context: Context, fromPosition: Int, toPosition: Int) {
        // TODO make sure changes are persistent across app restarts
        userPickedPlaylist?.swapSongPositions(
            fromPosition,
            toPosition
        )
        SaveFile.saveFile(context)
    }

    private var song: Song? = null
    private var probability: Double? = null

    fun notifySongRemoved(context: Context, position: Int) {
        song = userPickedPlaylist?.getSongs()?.get(position)
        probability = song?.let { userPickedPlaylist?.getProbability(it) }
        if (userPickedPlaylist?.size() == 1) {
            userPickedPlaylist?.let {
                removePlaylist(context, it)
            }
            // TODO test what happens when user is listening to a playlist and then removes it
            setUserPickedPlaylist(null)
        } else {
            song?.let { userPickedPlaylist?.remove(it) }
        }
    }

    fun notifyItemInserted(context: Context, position: Int) {
        song?.let { probability?.let { it1 -> userPickedPlaylist?.add(it, it1) } }
        userPickedPlaylist?.let {
            it.switchSongPositions(it.size() - 1, position)
            if (it.size() == 1) {
                addNewPlaylist(context, it)
            }
        }
        song = null
        probability = null
    }


    fun notifyPlaylistMoved(context: Context, fromPosition: Int, toPosition: Int) {
        // TODO make sure changes are persistent across app restarts
        Collections.swap(
            getPlaylists(),
            fromPosition,
            toPosition
        )
        SaveFile.saveFile(context)
    }

    private var playlist: RandomPlaylist? = null

    fun notifyPlaylistRemoved(context: Context, position: Int) {
        playlist = getPlaylists()[position]
        playlist?.let { removePlaylist(context, it) }
    }

    fun notifyPlaylistInserted(context: Context, position: Int) {
        playlist?.let { addNewPlaylist(context, position, it) }
        playlist = null
    }


    fun filterPlaylistSongs(string: String): List<Song> {
        // TODO fix bug where you can reorder songs when sifted
        // I think this is fixed
        val songs: List<Song>? = userPickedPlaylist?.getSongs()
        val sifted: MutableList<Song> = ArrayList<Song>()
        if (songs != null) {
            for (song in songs) {
                if (song.title.lowercase(Locale.ROOT)
                        .contains(string.lowercase(Locale.ROOT))
                ) {
                    sifted.add(song)
                }
            }
        }
        return sifted
    }

    fun filterPlaylists(newText: String): List<RandomPlaylist> {
        val sifted: MutableList<RandomPlaylist> = ArrayList<RandomPlaylist>()
        for (randomPlaylist in getPlaylists()) {
            if (randomPlaylist.getName().lowercase(Locale.ROOT)
                    .contains(newText.lowercase(Locale.ROOT))
            ) {
                sifted.add(randomPlaylist)
            }
        }
        return sifted
    }


    fun filterAllSongs(newText: String): List<Song> {
        val sifted: MutableList<Song> = ArrayList<Song>()
        for (song in getAllSongs()?: listOf<Song>()) {
            if (song.title.lowercase(Locale.ROOT)
                    .contains(newText.lowercase(Locale.ROOT))
            ) {
                sifted.add(song)
            }
        }
        return sifted
    }

    fun songClicked(context: Context, navController: NavController, song: Song) {
        val mediaPlayerModel = MediaPlayerModel.getInstance()
        val mediaModel: MediaModel = MediaModel.getInstance(context)
        val songQueue = SongQueue.getInstance()
        synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
            if (song == mediaPlayerModel.currentAudioUri.value?.id?.let {
                    getSong(it)
                }
            ) {
                mediaModel.seekTo(context, 0)
            }
            getUserPickedPlaylist()?.let { mediaModel.setCurrentPlaylist(it) }
            songQueue.newSessionStarted(song)
            val audioUri = AudioUri.getAudioUri(context, song.id)
            if (audioUri != null) {
                mediaPlayerModel.setCurrentAudioUri(audioUri)
            }
            mediaModel.playNext(context)
        }
        NavUtil.navigate(
            navController,
            FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong()
        )
    }


    fun playlistClicked(navController: NavController, playlist: RandomPlaylist) {
        setUserPickedPlaylist(playlist)
        NavUtil.navigate(
            navController,
            FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist()
        )
    }

    fun fragmentPlaylistFABClicked(navController: NavController) {
        clearUserPickedSongs()
        NavUtil.navigate(
            navController,
            FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist()
        )
    }

    fun fragmentPlaylistsFABClicked(navController: NavController) {
        setUserPickedPlaylist(null)
        clearUserPickedSongs()
        NavUtil.navigate(
            navController,
            FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist()
        )
    }

    fun addToQueueClicked(context: Context, song: Song) {
        val mediaModel: MediaModel = MediaModel.getInstance(context)
        val songQueue = SongQueue.getInstance()
        songQueue.addToQueue(song.id)
        if (!mediaModel.isSongInProgress()) {
            mediaModel.playNext(context)
        }
    }


    fun addToQueueClicked(context: Context, randomPlaylist: RandomPlaylist) {
        // TODO stop MasterPlaylist from continuing after queue is done
        // shuffle is off and looping is on or something like that?
        val mediaModel: MediaModel = MediaModel.getInstance(context)
        mediaModel.setCurrentPlaylistToMaster(context)
        val songQueue = SongQueue.getInstance()
        val songs = randomPlaylist.getSongs()
        for (song in songs) {
            songQueue.addToQueue(song.id)
        }
        if (!mediaModel.isSongInProgress()) {
            mediaModel.playNext(context)
        }
    }

    fun editSongsClicked(navController: NavController){
        for (song in getUserPickedSongs()) {
            song.setSelected(true)
        }
        NavUtil.navigate(navController, FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs())
    }

}