package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import android.content.Context
import androidx.annotation.GuardedBy
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ToastUtil
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.random_playlist.*
import java.util.*

class ViewModelUserPicks(application: Application) : AndroidViewModel(application)  {

    // TODO log user picked songs and playlist during program run to verify correct operation

    private val playlistsRepo = PlaylistsRepo.getInstance(application)
    private val songQueue = SongQueue.getInstance()

    @GuardedBy("this")
    private val userPickedSongs: MutableList<Song> = ArrayList()

    @Synchronized
    fun getUserPickedSongs(): List<Song> {
        return userPickedSongs
    }

    @Synchronized
    fun songSelected(songs: Song) {
        userPickedSongs.add(songs)
        songs.setSelected(true)
    }

    @Synchronized
    fun songUnselected(song: Song) {
        userPickedSongs.remove(song)
        song.setSelected(false)
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

    fun songClicked(context: Context, navController: NavController, song: Song) {
        val mediaPlayerSession = MediaPlayerSession.getInstance(context)
        val mediaSession: MediaSession = MediaSession.getInstance(context)
        synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
            // TODO pass this in?
            if (song == mediaPlayerSession.currentAudioUri.value?.id?.let {
                    playlistsRepo.getSong(it)
                }
            ) {
                mediaSession.seekTo(context, 0)
            }
            getUserPickedPlaylist()?.let { mediaSession.setCurrentPlaylist(it) }
            songQueue.newSessionStarted(song)
            mediaSession.playNext(context)
        }
        if (navController.currentDestination?.id == R.id.fragmentPlaylist) {
            NavUtil.navigate(
                navController,
                FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong()
            )
        } else if (navController.currentDestination?.id == R.id.fragmentSongs) {
            NavUtil.navigate(
                navController,
                FragmentSongsDirections.actionFragmentSongsToFragmentSong()
            )
        }
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

    fun fragmentEditPlaylistViewCreated() {
        selectSongsInUserPickedPlaylist()
    }

    @Synchronized
    private fun selectSongsInUserPickedPlaylist() {
        userPickedPlaylist?.let {
            for (song in it.getSongs()) {
                songSelected(song)
            }
        }
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

    fun editPlaylistFabClicked(fragment: Fragment, context: Context, playlistName: String) {
        if (validatePlaylistInput(context, playlistName)) {
            val makingNewPlaylist = (userPickedPlaylist == null)
            if (makingNewPlaylist) {
                makeNewPlaylist(context, playlistName)
            } else if (!makingNewPlaylist) {
                makeNewPlaylist(context, playlistName)
            }
            clearUserPickedSongs()
            NavUtil.popBackStack(fragment)
        }
    }

    private fun validatePlaylistInput(context: Context, playlistName: String): Boolean {
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
            playlistsRepo.doesPlaylistExist(playlistName) &&
            makingNewPlaylist
        ) {
            ToastUtil.showToast(context, R.string.duplicate_name_playlist)
            false
        } else {
            true
        }
    }

    private fun makeNewPlaylist(context: Context, playlistName: String) {
        val playlistNames = playlistsRepo.getPlaylistTitles()
        // TODO Is this a deep copy
        var finalPlaylist: RandomPlaylist? = userPickedPlaylist
        if (finalPlaylist?.getName() == playlistName ||
            playlistNames.contains(playlistName)
        ) {
            finalPlaylist?.setName(
                context,
                playlistName
            )
            val songs = finalPlaylist?.getSongs()
            if (songs != null) {
                for (song in songs) {
                    if (!userPickedSongs.contains(song)) {
                        finalPlaylist?.remove(context, song)
                    }
                }
            }
            for (song in userPickedSongs) {
                finalPlaylist?.add(
                    context,
                    song
                )
                song.setSelected(false)
            }
        } else {
            finalPlaylist = RandomPlaylist(
                context,
                playlistName,
                userPickedSongs,
                false
            )
        }
        finalPlaylist?.let { playlistsRepo.addPlaylist(context, it) }
    }

    fun editSongsClicked(navController: NavController) {
        for (song in getUserPickedSongs()) {
            song.setSelected(true)
        }
        NavUtil.navigate(
            navController,
            FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs()
        )
    }

    fun fragmentSongsViewCreated() {
        setUserPickedPlaylist(playlistsRepo.getMasterPlaylist())
    }

    fun playlistCreatedFromFolder(navController: NavController, playlist: RandomPlaylist?) {
        setUserPickedPlaylist(playlist)
        NavUtil.navigate(
            navController,
            FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists()
        )
    }

    fun startNewPlaylistWithSongs(songs: MutableList<Song>) {
        setUserPickedPlaylist(null)
        clearUserPickedSongs()
        for(song in songs){
            songSelected(song)
        }
    }

    fun notifySongMoved(context: Context, fromPosition: Int, toPosition: Int) {
        // TODO make sure changes are persistent across app restarts
        userPickedPlaylist?.swapSongPositions(
            context,
            fromPosition,
            toPosition
        )
        SaveFile.saveFile(context)
    }

    private var song: Song? = null
    private var probability: Double? = null

    fun notifySongRemoved(context: Context, position: Int) {
        song = userPickedPlaylist?.getSongs()?.toList()?.get(position)
        probability = song?.let { userPickedPlaylist?.getProbability(it) }
        if (userPickedPlaylist?.size() == 1) {
            userPickedPlaylist?.let {
                playlistsRepo.removePlaylist(context, it)
            }
            // TODO test what happens when user is listening to a playlist and then removes it
            setUserPickedPlaylist(null)
        } else {
            song?.let { userPickedPlaylist?.remove(context, it) }
        }
    }

    fun notifyItemInserted(context: Context, position: Int) {
        song?.let { probability?.let { it1 -> userPickedPlaylist?.add(
            getApplication<Application>().applicationContext,
            it,
            it1
        ) } }
        userPickedPlaylist?.let {
            it.switchSongPositions(
                context,
                it.size() - 1,
                position
            )
            if (it.size() == 1) {
                playlistsRepo.addPlaylist(context, it)
            }
        }
        song = null
        probability = null
    }

    fun filterPlaylistSongs(string: String): List<Song> {
        // TODO fix bug where you can reorder songs when sifted
        // I think this is fixed
        val songs: Set<Song>? = userPickedPlaylist?.getSongs()
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

}