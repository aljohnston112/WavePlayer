package io.fourth_finger.pinky_player.fragments

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.fourth_finger.pinky_player.ApplicationMain
import io.fourth_finger.pinky_player.NavUtil
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.random_playlist.MediaSession

class ViewModelFragmentPaneSong(
    private val mediaSession: MediaSession,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    fun clicked(fragment: Fragment, @IdRes id: Int) {
        val context = fragment.requireActivity().applicationContext
        when (id) {
            R.id.imageButtonSongPaneNext -> {
                mediaSession.playNext(context)
            }
            R.id.imageButtonSongPanePlayPause -> {
                mediaSession.pauseOrPlay(context)
            }
            R.id.imageButtonSongPanePrev -> {
                mediaSession.playPrevious(context)
            }
            R.id.textViewSongPaneSongName, R.id.imageViewSongPaneSongArt -> {
                NavUtil.navigateTo(fragment, R.id.fragmentSong)
            }
        }
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val savedStateHandle = extras.createSavedStateHandle()
                return ViewModelFragmentPaneSong(
                    (application as ApplicationMain).mediaSession!!,
                    savedStateHandle
                ) as T
            }
        }

    }

}