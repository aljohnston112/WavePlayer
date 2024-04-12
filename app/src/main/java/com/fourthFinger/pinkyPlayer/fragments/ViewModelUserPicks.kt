package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ToastUtil
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.random_playlist.*
import com.fourthFinger.pinkyPlayer.settings.SettingsRepo
import java.util.*

class ViewModelUserPicks(
    val settingsRepo: SettingsRepo,
    val playlistsRepo: PlaylistsRepo,
    val mediaSession: MediaSession,
    val mediaPlayerManager: MediaPlayerManager,
    val songQueue: SongQueue,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // TODO log user picked songs and playlist during program run to verify correct operation

    @GuardedBy("this")
    private val userPickedSongs: MutableList<Song> = mutableListOf()

    @Synchronized
    fun getUserPickedSongs(): List<Song> {
        return userPickedSongs
    }

    @Synchronized
    fun selectSong(songs: Song) {
        userPickedSongs.add(songs)
        songs.setSelected(true)
    }

    @Synchronized
    fun unselectedSong(song: Song) {
        userPickedSongs.remove(song)
        song.setSelected(false)
    }

    @Synchronized
    fun unselectUserPickedSongs() {
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
        synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
            // TODO pass this in?
            if (song == mediaPlayerManager.currentAudioUri.value?.id?.let {
                    playlistsRepo.getSong(it)
                }
            ) {
                mediaSession.seekTo(context, 0)
            }
            if (navController.currentDestination?.id == R.id.fragmentPlaylist) {
                getUserPickedPlaylist()?.let { mediaSession.setCurrentPlaylist(it) }
            } else if(navController.currentDestination?.id == R.id.fragmentSongs){
                mediaSession.setCurrentPlaylistToMaster()
            }
            songQueue.newSessionStarted(song.id)
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
        unselectUserPickedSongs()
        selectSongsInUserPickedPlaylist()
        NavUtil.navigate(
            navController,
            FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist()
        )
    }

    fun fragmentPlaylistsFABClicked(navController: NavController) {
        setUserPickedPlaylist(null)
        unselectUserPickedSongs()
        NavUtil.navigate(
            navController,
            FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist()
        )
    }

    @Synchronized
    private fun selectSongsInUserPickedPlaylist() {
        userPickedPlaylist?.let {
            for (song in it.getSongs()) {
                selectSong(song)
                userPickedSongs.add(song)
            }
        }
    }

    fun editPlaylistFabClicked(fragment: Fragment, playlistName: String) {
        val context = fragment.requireActivity().applicationContext
        if (validatePlaylistInput(context, playlistName)) {
            val finalPlaylist: RandomPlaylist? = userPickedPlaylist
            // userPickedPlaylist is non-null when editing a playlist.
            if (finalPlaylist != null) {
                updatePlaylist(context, finalPlaylist, playlistName)
            } else {
                makeNewPlaylist(context, playlistName)
            }
            unselectUserPickedSongs()
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
            playlistsRepo.playlistExists(playlistName) && makingNewPlaylist) {
            ToastUtil.showToast(context, R.string.duplicate_name_playlist)
            false
        } else {
            true
        }
    }

    private fun updatePlaylist(
        context: Context,
        finalPlaylist: RandomPlaylist,
        playlistName: String
    ) {
        playlistsRepo.removePlaylist(context, finalPlaylist)
        finalPlaylist.setName(
            context,
            playlistsRepo,
            playlistName
        )
        removeMissingSongs(context, finalPlaylist)
        addNewSongs(context, finalPlaylist)
    }

    private fun removeMissingSongs(context: Context, finalPlaylist: RandomPlaylist) {
        val songs = finalPlaylist.getSongs()
        for (song in songs) {
            if (!userPickedSongs.contains(song)) {
                finalPlaylist.remove(context, playlistsRepo, song)
            }
        }
    }

    private fun addNewSongs(context: Context, finalPlaylist: RandomPlaylist) {
        for (song in userPickedSongs) {
            finalPlaylist.add(
                context,
                playlistsRepo,
                song
            )
            song.setSelected(false)
        }
        userPickedSongs.clear()
    }

    private fun makeNewPlaylist(context: Context, playlistName: String) {
        val finalPlaylist = RandomPlaylist(
            context,
            playlistName,
            userPickedSongs,
            false,
            settingsRepo.settings.value!!.maxPercent,
            playlistsRepo
        )
        playlistsRepo.addPlaylist(context, finalPlaylist)
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

    fun playlistCreatedFromFolder(navController: NavController, playlist: RandomPlaylist?) {
        playlist?.let {
            setUserPickedPlaylist(it)
            NavUtil.navigate(
                navController,
                FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists()
            )}
    }

    fun startNewPlaylistWithSongs(songs: List<Song>) {
        setUserPickedPlaylist(null)
        unselectUserPickedSongs()
        for (song in songs) {
            selectSong(song)
        }
    }

    fun notifySongMoved(context: Context, fromPosition: Int, toPosition: Int) {
        // TODO make sure changes are persistent across app restarts
        userPickedPlaylist?.swapSongPositions(
            context,
            playlistsRepo,
            fromPosition,
            toPosition
        )
    }

    private var songForUndo: Song? = null
    private var probabilityForUndo: Double? = null

    fun notifySongRemoved(fragment: Fragment, position: Int) {
        val context = fragment.requireActivity().applicationContext
        songForUndo = userPickedPlaylist?.getSongs()?.toList()?.get(position)
        probabilityForUndo = songForUndo?.let { userPickedPlaylist?.getProbability(it) }
        if (userPickedPlaylist?.size() == 1) {
            userPickedPlaylist?.let {
                playlistsRepo.removePlaylist(context, it)
                // TODO test what happens when user is listening to a playlist and then removes it
                setUserPickedPlaylist(null)
                NavUtil.popBackStack(fragment)
            }
        } else {
            songForUndo?.let { userPickedPlaylist?.remove(context, playlistsRepo, it) }
        }
    }

    fun notifyItemInserted(context: Context, position: Int) {
        songForUndo?.let {
            probabilityForUndo?.let { it1 ->
                userPickedPlaylist?.add(
                    context,
                    playlistsRepo,
                    it,
                    it1
                )
            }
        }
        userPickedPlaylist?.let {
            it.switchSongPositions(
                context,
                playlistsRepo,
                it.size() - 1,
                position
            )
            if (it.size() == 1) {
                playlistsRepo.addPlaylist(context, it)
            }
        }
        songForUndo = null
        probabilityForUndo = null
    }

    fun siftPlaylistSongs(string: String): List<Song> {
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

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val savedStateHandle = extras.createSavedStateHandle()
                return ViewModelUserPicks(
                    (application as ApplicationMain).settingsRepo,
                    application.playlistsRepo,
                    application.mediaSession,
                    application.mediaPlayerManager,
                    application.songQueue,
                    savedStateHandle
                ) as T
            }
        }
    }

}