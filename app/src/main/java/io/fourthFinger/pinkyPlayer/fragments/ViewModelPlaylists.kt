package io.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import io.fourthFinger.pinkyPlayer.ApplicationMain
import io.fourthFinger.pinkyPlayer.NavUtil
import io.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import io.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import io.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import io.fourthFinger.pinkyPlayer.random_playlist.SaveFile
import io.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker
import java.util.*

class ViewModelPlaylists(
    val playlistsRepo: PlaylistsRepo,
    val mediaSession: MediaSession,
    val songPicker: UseCaseSongPicker,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun getPlaylists(): List<RandomPlaylist> {
        return playlistsRepo.getPlaylists()
    }

    fun notifyPlaylistMoved(context: Context, fromPosition: Int, toPosition: Int) {
        // TODO make sure changes are persistent across app restarts
        Collections.swap(
            getPlaylists(),
            fromPosition,
            toPosition
        )
        SaveFile.saveFile(context, playlistsRepo)
    }

    private var playlistForUndo: RandomPlaylist? = null

    fun notifyPlaylistRemoved(context: Context, position: Int) {
        playlistForUndo = getPlaylists()[position]
        playlistForUndo?.let { playlistsRepo.removePlaylist(context, it) }
    }

    fun notifyPlaylistInserted(context: Context, position: Int) {
        playlistForUndo?.let { playlistsRepo.addPlaylist(context, position, it) }
        playlistForUndo = null
    }

    fun siftPlaylists(newText: String): List<RandomPlaylist> {
        val sifted: MutableList<RandomPlaylist> = mutableListOf()
        for (randomPlaylist in getPlaylists()) {
            if (randomPlaylist.getName().lowercase(Locale.ROOT)
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
                    (application as ApplicationMain).playlistsRepo,
                    application.mediaSession,
                    application.songPicker,
                    savedStateHandle
                ) as T
            }
        }
    }

}