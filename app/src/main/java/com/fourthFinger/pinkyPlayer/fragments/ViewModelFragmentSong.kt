package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.fourthFinger.pinkyPlayer.media_controller.MediaModel
import com.fourthFinger.pinkyPlayer.media_controller.SaveFile
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri

class ViewModelFragmentSong(application: Application): AndroidViewModel(application) {

    private val playlistsRepo = PlaylistsRepo.getInstance(application)

    private val mediaModel: MediaModel = MediaModel.getInstance(
        application.applicationContext
    )

    fun thumbDownClicked(currentAudioUri: AudioUri?) {
        currentAudioUri?.id?.let {
            playlistsRepo.getSong(it)?.let { song ->
                mediaModel.getCurrentPlaylist()?.globalBad(song)
                SaveFile.saveFile(getApplication<Application>().applicationContext)
            }
        }
    }

    fun thumbUpClicked(currentAudioUri: AudioUri?) {
        currentAudioUri?.id?.let {
            playlistsRepo.getSong(it)?.let { song ->
                mediaModel.getCurrentPlaylist()?.globalGood(song)
                SaveFile.saveFile(getApplication<Application>().applicationContext)
            }
        }
    }

    fun shuffleClicked() {
        if (mediaModel.isShuffling()) {
            mediaModel.setShuffling(false)
        } else {
            mediaModel.setShuffling(true)
        }
    }

    fun prevClicked() {
        mediaModel.playPrevious(getApplication<Application>().applicationContext)
    }

    fun playPauseClicked() {
        mediaModel.pauseOrPlay(getApplication<Application>().applicationContext)
    }

    fun nextClicked() {
        mediaModel.playNext(getApplication<Application>().applicationContext)
    }

    fun repeatClicked() {
        when {
            mediaModel.isLoopingOne() -> {
                mediaModel.setLoopingOne(false)
            }
            mediaModel.isLooping() -> {
                mediaModel.setLooping(false)
                mediaModel.setLoopingOne(true)
            }
            else -> {
                mediaModel.setLooping(true)
                mediaModel.setLoopingOne(false)
            }
        }
    }

    fun nextLongClicked(currentAudioUri: AudioUri?) {
        currentAudioUri?.id?.let { id ->
            playlistsRepo.getSong(id)?.let {
                mediaModel.getCurrentPlaylist()?.globalBad(it)
            }
        }
    }

}