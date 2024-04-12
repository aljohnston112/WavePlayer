package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.activity_main.DialogFragmentAddToPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.*

class ViewModelAddToQueue(
    val playlistsRepo: PlaylistsRepo,
    val mediaSession: MediaSession,
    val mediaPlayerManager: MediaPlayerManager,
    val songQueue: SongQueue,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    // TODO add setting to stop playlist from continuing after queue is done
    // shuffle is off and looping is on or something like that?

    private val _playlistToAddToQueue = MutableLiveData<RandomPlaylist?>()
    private val playlistToAddToQueue = _playlistToAddToQueue as LiveData<RandomPlaylist?>
    private fun getPlaylistToAddToQueue(): RandomPlaylist? {
        return playlistToAddToQueue.value?.let { playlistsRepo.getPlaylist(it.getName()) }
    }
    fun setPlaylistToAddToQueue(playlistToAddToQueue: RandomPlaylist?) {
        _playlistToAddToQueue.postValue(playlistToAddToQueue)
        _songToAddToQueue.postValue(null)
    }

    private val _songToAddToQueue = MutableLiveData<Long?>()
    private val songToAddToQueue = _songToAddToQueue as LiveData<Long?>
    private fun getSongToAddToQueue(): Song? {
        return songToAddToQueue.value?.let { playlistsRepo.getSong(it) }
    }
    fun setSongToAddToQueue(songToAddToQueue: Long?) {
        _songToAddToQueue.postValue(songToAddToQueue)
        _playlistToAddToQueue.postValue(null)
    }

    fun addToQueue(context: Context, song: Song) {
        songQueue.addToQueue(song.id)
        if (mediaPlayerManager.isSongInProgress() == false) {
            mediaSession.playNext(context)
        }
    }

    fun addToQueue(context: Context, randomPlaylist: RandomPlaylist) {
        val songs = randomPlaylist.getSongs()
        for (song in songs) {
            songQueue.addToQueue(song.id)
        }
        if (mediaPlayerManager.isSongInProgress() == false) {
            mediaSession.playNext(context)
        }
    }

    fun actionAddToQueue(context: Context) {
        // TODO pretty sure song and playlist could be non-null at the same time
        songToAddToQueue.value?.let { songQueue.addToQueue(it) }
        playlistToAddToQueue.value?.getSongs()?.let {
            for (songs in it) {
                songQueue.addToQueue(songs.id)
            }
        }
        // TODO Song will play even though user might not want it. Make a setting.
        if (mediaPlayerManager.isSongInProgress() == false) {
            mediaSession.playNext(context)
        }
    }

    fun actionAddToPlaylist(supportFragmentManager: FragmentManager) {
        val bundle = Bundle()
        getSongToAddToQueue()?.let {
            bundle.putSerializable(
                DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG,
                it
            )
        }
        getPlaylistToAddToQueue()?.let {
            bundle.putSerializable(
                DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST,
                it
            )
        }
        val dialogFragment: DialogFragment = DialogFragmentAddToPlaylist()
        dialogFragment.arguments = bundle
        dialogFragment.show(supportFragmentManager, DialogFragmentAddToPlaylist.TAG)
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
                return ViewModelAddToQueue(
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