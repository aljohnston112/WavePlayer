package io.fourth_finger.pinky_player.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import io.fourth_finger.pinky_player.ApplicationMain
import io.fourth_finger.pinky_player.NavUtil
import io.fourth_finger.pinky_player.random_playlist.MediaSession
import io.fourth_finger.pinky_player.random_playlist.UseCaseSongPicker

class ViewModelFragmentPlaylist(
    private val songPicker: UseCaseSongPicker,
    private val mediaSession: MediaSession,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun fabClicked(navController: NavController) {
        songPicker.unselectAllSongs()
        mediaSession.currentlyPlayingPlaylist.value?.let {
            songPicker.selectSongsInPlaylist(it)
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
                    (application as ApplicationMain).songPicker!!,
                    application.mediaSession!!,
                    savedStateHandle
                ) as T
            }
        }
    }

}