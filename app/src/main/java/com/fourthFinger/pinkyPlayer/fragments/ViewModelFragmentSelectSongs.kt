package com.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import com.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker

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
        songPicker.selectSong(song, playlistsRepo.getMasterPlaylist())
    }

    fun selectSongs(playlist: RandomPlaylist?) {
        if (playlist != null) {
            songPicker.selectSongsInPlaylist(playlist, playlistsRepo.getMasterPlaylist())
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