package io.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.fourthFinger.pinkyPlayer.ApplicationMain
import io.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import io.fourthFinger.playlistDataSource.Song
import io.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker

class ViewModelDialogFragmentAddToPlaylist(
    private val songPicker: UseCaseSongPicker,
    private val mediaSession: MediaSession,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun startNewPlaylistWithSongs(songs: List<Song>) {
        mediaSession.setCurrentPlaylist(null)
        songPicker.unselectAllSongs()
        for (song in songs) {
            songPicker.selectSong(song)
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
                return ViewModelDialogFragmentAddToPlaylist(
                    (application as ApplicationMain).songPicker!!,
                    application.mediaSession!!,
                    savedStateHandle
                ) as T
            }
        }
    }

}