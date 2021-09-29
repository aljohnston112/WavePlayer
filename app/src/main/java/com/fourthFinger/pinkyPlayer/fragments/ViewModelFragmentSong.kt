package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo

class ViewModelFragmentSong(application: Application): AndroidViewModel(application) {

    private val playlistsRepo = PlaylistsRepo.getInstance(application)

    private val mediaSession: MediaSession = MediaSession.getInstance(
        application.applicationContext
    )

    fun thumbDownClicked(currentAudioUri: AudioUri?) {
        currentAudioUri?.id?.let {
            playlistsRepo.getSong(it)?.let { song ->
                mediaSession.getCurrentPlaylist()?.bad(
                    getApplication<Application>().applicationContext,
                    song
                )
            }
        }
    }

    fun thumbUpClicked(currentAudioUri: AudioUri?) {
        currentAudioUri?.id?.let {
            playlistsRepo.getSong(it)?.let { song ->
                mediaSession.getCurrentPlaylist()?.good(
                    getApplication<Application>().applicationContext,
                    song
                )
            }
        }
    }

    fun shuffleClicked() {
        if (mediaSession.isShuffling()) {
            mediaSession.setShuffling(false)
        } else {
            mediaSession.setShuffling(true)
        }
    }

    fun prevClicked() {
        mediaSession.playPrevious(getApplication<Application>().applicationContext)
    }

    fun playPauseClicked() {
        mediaSession.pauseOrPlay(getApplication<Application>().applicationContext)
    }

    fun nextClicked(currentAudioUri: AudioUri?) {
        mediaSession.playNext(getApplication<Application>().applicationContext)
        currentAudioUri?.id?.let { id ->
            playlistsRepo.getSong(id)?.let {
                mediaSession.getCurrentPlaylist()?.bad(
                    getApplication<Application>().applicationContext,
                    it
                )
            }
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

}