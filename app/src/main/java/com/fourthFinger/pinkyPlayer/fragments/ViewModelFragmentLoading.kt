package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.CreationExtras
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.SongRepo
import kotlin.math.roundToInt

class ViewModelFragmentLoading(
    val songRepo: SongRepo,
    val playlistsRepo: PlaylistsRepo,
    val mediaPlayerManager: MediaPlayerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var loadingText: LiveData<String> = MutableLiveData()
        private set

    var loadingProgress: LiveData<Int> = MutableLiveData()
        private set

    fun permissionGranted(context: Context) {
        loadingText = songRepo.loadingText
        loadingProgress = songRepo.loadingProgress.map { percent ->
            (percent * 100.0).roundToInt()
        }
        songRepo.loadSongs(
            playlistsRepo,
            mediaPlayerManager,
            context
        )
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
                return ViewModelFragmentLoading(
                    (application as ApplicationMain).songRepo,
                    application.playlistsRepo,
                    application.mediaPlayerManager,
                    savedStateHandle
                ) as T
            }
        }
    }

}