package io.fourth_finger.pinky_player.fragments

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.CreationExtras
import io.fourth_finger.pinky_player.ApplicationMain
import io.fourth_finger.pinky_player.random_playlist.SongRepo
import io.fourth_finger.playlist_data_source.PlaylistsRepo
import kotlin.math.roundToInt

class ViewModelFragmentLoading(
    private val songRepo: SongRepo,
    private val playlistsRepo: PlaylistsRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val loadingText = songRepo.loadingText

    val loadingProgress = songRepo.loadingProgress.map { percent ->
        (percent * 100.0).roundToInt()
    }

    private val _showLoadingBar = MutableLiveData(false)
    val showLoadingBar = _showLoadingBar as LiveData<Boolean>


    fun permissionGranted(
        context: Context
    ) {
        _showLoadingBar.postValue(true)
        songRepo.loadSongs(
            playlistsRepo,
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
                    (application as ApplicationMain).songRepo!!,
                    application.playlistsRepo!!,
                    savedStateHandle
                ) as T
            }
        }
    }

}