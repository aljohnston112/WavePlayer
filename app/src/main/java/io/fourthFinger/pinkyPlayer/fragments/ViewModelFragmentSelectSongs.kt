package io.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.fourthFinger.pinkyPlayer.ApplicationMain
import io.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker
import io.fourthFinger.playlistDataSource.Song

class ViewModelFragmentSelectSongs(
    private val songPicker: UseCaseSongPicker,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun unselectedSong(song: Song) {
        songPicker.unselectedSong(song)
    }

    fun selectSong(song: Song) {
        songPicker.selectSong(song)
    }

    fun selectSongs(playlist: io.fourthFinger.playlistDataSource.RandomPlaylist?) {
        if (playlist != null) {
            songPicker.selectSongsInPlaylist(playlist)
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
                    (application as ApplicationMain).songPicker!!,
                    savedStateHandle
                ) as T
            }
        }
    }

}