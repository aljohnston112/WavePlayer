package io.fourth_finger.pinky_player.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.fourth_finger.pinky_player.ApplicationMain
import io.fourth_finger.pinky_player.random_playlist.MediaSession

class ViewModelFragmentQueue(
    private val mediaSession: MediaSession,
    savedStateHandle: SavedStateHandle,
): ViewModel() {

    val songQueue = mediaSession.songList


    fun notifyItemInserted(position: Int) {
        mediaSession.notifyItemInserted(position)
    }

    fun notifySongRemoved(position: Int): Boolean {
       return  mediaSession.notifySongRemoved(position)
    }

    fun notifySongMoved(from: Int, to: Int) {
        mediaSession.notifySongMoved(from, to)
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
                return ViewModelFragmentQueue(
                    (application as ApplicationMain).mediaSession!!,
                    savedStateHandle
                ) as T
            }
        }
    }

}
