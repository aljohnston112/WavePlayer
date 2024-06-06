package io.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.fourthFinger.pinkyPlayer.ApplicationMain
import io.fourthFinger.pinkyPlayer.random_playlist.SongQueue

class ViewModelFragmentQueue(
    private val _songQueue: SongQueue,
    savedStateHandle: SavedStateHandle,
): ViewModel() {

    val songQueue = _songQueue.songQueue

    fun notifyItemInserted(position: Int) {
        _songQueue.notifyItemInserted(position)
    }

    fun notifySongRemoved(position: Int): Boolean {
       return  _songQueue.notifySongRemoved(position)
    }

    fun notifySongMoved(from: Int, to: Int) {
        _songQueue.notifySongMoved(from, to)
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
                    (application as ApplicationMain).songQueue!!,
                    savedStateHandle
                ) as T
            }
        }
    }

}
