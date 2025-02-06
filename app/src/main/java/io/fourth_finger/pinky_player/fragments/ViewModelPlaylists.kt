package io.fourth_finger.pinky_player.fragments

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import io.fourth_finger.pinky_player.ApplicationMain
import io.fourth_finger.pinky_player.NavUtil
import io.fourth_finger.pinky_player.random_playlist.MediaSession
import io.fourth_finger.pinky_player.random_playlist.UseCaseSongPicker
import io.fourth_finger.playlist_data_source.PlaylistsRepo
import io.fourth_finger.playlist_data_source.RandomPlaylist
import io.fourth_finger.playlist_data_source.Song
import java.util.*

class ViewModelPlaylists(
    private val playlistsRepo: PlaylistsRepo,
    private val mediaSession: MediaSession,
    private val songPicker: UseCaseSongPicker,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var playlistForUndo: RandomPlaylist? = null

    val playlists = playlistsRepo.playlists.map {
        it.playlists
    }

    fun notifyPlaylistMoved(
        context: Context,
        fromPosition: Int,
        toPosition: Int
    ) {
        // TODO make sure changes are persistent across app restarts
        // Definitely not saved if this is the code to swap
        Collections.swap(
            playlists.value!!,
            fromPosition,
            toPosition
        )
    }

    fun notifyPlaylistRemoved(context: Context, position: Int) {
        playlistForUndo = playlists.value!![position]
        playlistForUndo?.let { playlistsRepo.removePlaylist(context, it) }
    }

    fun notifyPlaylistInserted(context: Context, position: Int) {
        playlistForUndo?.let { playlistsRepo.addPlaylist(context, position, it) }
        playlistForUndo = null
    }

    fun siftPlaylists(newText: String): List<RandomPlaylist> {
        val sifted: MutableList<RandomPlaylist> = mutableListOf()
        for (randomPlaylist in playlists.value!!) {
            if (randomPlaylist.name.lowercase(Locale.ROOT)
                    .contains(newText.lowercase(Locale.ROOT))
            ) {
                sifted.add(randomPlaylist)
            }
        }
        return sifted
    }

    fun getPlaylistTitles(): Array<String> {
        return playlistsRepo.getPlaylistTitles()
    }

    fun playlistClicked(
        navController: NavController,
        playlist: RandomPlaylist
    ) {
        mediaSession.setCurrentPlaylist(playlist)
        NavUtil.navigate(
            navController,
            FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist()
        )
    }

    fun fabClicked(navController: NavController) {
        songPicker.unselectAllSongs()
        NavUtil.navigate(
            navController,
            FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist()
        )
    }

    fun addToPlaylist(
        context: Context,
        index: Int,
        song: Song
    ) {
        playlistsRepo.addSong(
            context,
            playlists.value!![index],
            song
        )
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val savedStateHandle = extras.createSavedStateHandle()
                return ViewModelPlaylists(
                    (application as ApplicationMain).playlistsRepo!!,
                    application.mediaSession!!,
                    application.songPicker!!,
                    savedStateHandle
                ) as T
            }
        }
    }

}