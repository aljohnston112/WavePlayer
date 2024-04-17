package com.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import com.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker

class ViewModelDialogFragmentAddToPlaylist(
    private val songPicker: UseCaseSongPicker,
    private val mediaSession: MediaSession,
    private val playlistsRepo: PlaylistsRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun startNewPlaylistWithSongs(songs: List<Song>) {
        mediaSession.setCurrentPlaylist(null)
        songPicker.unselectAllSongs()
        for (song in songs) {
            songPicker.selectSong(song, playlistsRepo.getMasterPlaylist())
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
                    (application as ApplicationMain).songPicker,
                    application.mediaSession,
                    application.playlistsRepo,
                    savedStateHandle
                ) as T
            }
        }
    }

}