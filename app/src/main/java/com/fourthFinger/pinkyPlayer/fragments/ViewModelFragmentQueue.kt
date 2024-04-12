package com.fourthFinger.pinkyPlayer.fragments

import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.random_playlist.*

class ViewModelFragmentQueue(
    val playlistsRepo: PlaylistsRepo,
    val mediaSession: MediaSession,
    val mediaPlayerManager: MediaPlayerManager,
    private val _songQueue: SongQueue,
    savedStateHandle: SavedStateHandle,
): ViewModel() {

    val songQueue = _songQueue.songQueue

    fun songClicked(fragment: Fragment, queuePosition: Int) {
        val song: Song = _songQueue.setIndex(queuePosition)
        val context = fragment.requireContext()
        synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
            if (song == mediaPlayerManager.currentAudioUri.value?.id?.let {
                    playlistsRepo.getSong(it)
                }
            ) {
                mediaSession.seekTo(context,0)
            }
            mediaSession.playNext(context)
        }
        NavUtil.navigateTo(fragment, R.id.fragmentSong)
    }

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
                    (application as ApplicationMain).playlistsRepo,
                    application.mediaSession,
                    application.mediaPlayerManager,
                    application.songQueue,
                    savedStateHandle
                ) as T
            }
        }
    }

}
