package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import com.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.settings.SettingsRepo

class ViewModelFragmentSong(
    val playlistsRepo: PlaylistsRepo,
    val settingsRepo: SettingsRepo,
    val mediaPlayerManager: MediaPlayerManager,
    val mediaSession: MediaSession,
    savedStateHandle: SavedStateHandle
) : ViewModel() {


    val currentAudioUri = mediaPlayerManager.currentAudioUri
    val isPlaying = mediaPlayerManager.isPlaying

    fun thumbDownClicked(
        context: Context,
        currentAudioUri: AudioUri
    ) {
        playlistsRepo.getSong(currentAudioUri.id)?.let { song ->
            mediaSession.getCurrentPlaylist().bad(
                context,
                playlistsRepo,
                song,
                settingsRepo.settings.value!!.percentChangeDown
            )
        }
    }

    fun thumbUpClicked(
        context: Context,
        currentAudioUri: AudioUri
    ) {
        playlistsRepo.getSong(currentAudioUri.id)?.let { song ->
            mediaSession.getCurrentPlaylist().good(
                context,
                playlistsRepo,
                song,
                settingsRepo.settings.value!!.percentChangeUp
            )
        }
    }

    fun shuffleClicked(context: Context) {
        if (mediaSession.isShuffling()) {
            mediaSession.setShuffling(context, false)
        } else {
            mediaSession.setShuffling(context, true)
        }
    }

    fun prevClicked(context: Context) {
        mediaSession.playPrevious(context)
    }

    fun playPauseClicked(context: Context) {
        mediaSession.pauseOrPlay(context)
    }

    fun nextClicked(
        context: Context,
        currentAudioUri: AudioUri
    ) {
        mediaSession.playNext(context)
        playlistsRepo.getSong(currentAudioUri.id)?.let {
            mediaSession.getCurrentPlaylist().bad(
                context,
                playlistsRepo,
                it,
                settingsRepo.settings.value!!.percentChangeDown
            )
        }
    }

    fun repeatClicked() {
        when {
            mediaSession.isLoopingOne() -> {
                mediaSession.setLoopingOne(false)
            }

            mediaSession.isLooping() -> {
                mediaSession.setLooping(false)
                mediaSession.setLoopingOne(true)
            }

            else -> {
                mediaSession.setLooping(true)
                mediaSession.setLoopingOne(false)
            }
        }
    }

    fun playNext(context: Context) {
        mediaSession.playNext(context)
    }

    fun getCurrentTime(): Int {
        return mediaPlayerManager.getCurrentTime()
    }

    fun seekTo(context: Context, progress: Int) {
        mediaPlayerManager.seekTo(context, progress)
    }

    fun isShuffling(): Boolean {
        return mediaSession.isShuffling()
    }

    fun isLoopingOne(): Boolean {
        return mediaSession.isLoopingOne()
    }

    fun isLooping(): Boolean {
        return mediaSession.isLooping()
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
                return ViewModelFragmentSong(
                    (application as ApplicationMain).playlistsRepo,
                    application.settingsRepo,
                    application.mediaPlayerManager,
                    application.mediaSession,
                    savedStateHandle
                ) as T
            }
        }
    }

}