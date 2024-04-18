package io.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.fourthFinger.pinkyPlayer.ApplicationMain
import io.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import io.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import io.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import io.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import io.fourthFinger.pinkyPlayer.random_playlist.Song
import io.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker

class ViewModelFragmentSelectSongs(
    val playlistsRepo: PlaylistsRepo,
    val mediaSession: MediaSession,
    val mediaPlayerManager: MediaPlayerManager,
    val songPicker: UseCaseSongPicker,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun unselectedSong(song: Song) {
        songPicker.unselectedSong(song)
    }

    fun selectSong(song: Song) {
        songPicker.selectSong(song)
    }

    fun selectSongs(playlist: RandomPlaylist?) {
        if (playlist != null) {
            songPicker.selectSongsInPlaylist(playlist)
        } else {
            songPicker.unselectAllSongs()
        }
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
                return ViewModelFragmentSelectSongs(
                    (application as ApplicationMain).playlistsRepo,
                    application.mediaSession,
                    application.mediaPlayerManager,
                    application.songPicker,
                    savedStateHandle
                ) as T
            }
        }
    }

}