package com.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker

class ViewModelFragmentPlaylist(
    private val songPicker: UseCaseSongPicker,
    private val mediaSession: MediaSession,
    private val playlistsRepo: PlaylistsRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun fabClicked(navController: NavController) {
        songPicker.unselectAllSongs()
        mediaSession.currentPlaylist.value?.let {
            songPicker.selectSongsInPlaylist(it, playlistsRepo.getMasterPlaylist())
        }
        NavUtil.navigate(
            navController,
            FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist()
        )
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
                return ViewModelFragmentPlaylist(
                    (application as ApplicationMain).songPicker,
                    application.mediaSession,
                    application.playlistsRepo,
                    savedStateHandle
                ) as T
            }
        }
    }

}