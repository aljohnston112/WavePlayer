package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fourthFinger.pinkyPlayer.activity_main.DialogFragmentAddToPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.*

class ViewModelAddToQueue(application: Application): AndroidViewModel(application) {

    // TODO add setting to stop playlist from continuing after queue is done
    // shuffle is off and looping is on or something like that?

    private val _playlistToAddToQueue = MutableLiveData<RandomPlaylist?>()
    private val playlistToAddToQueue = _playlistToAddToQueue as LiveData<RandomPlaylist?>
    private fun getPlaylistToAddToQueue(): RandomPlaylist? {
        val playlistsRepo = PlaylistsRepo.getInstance(getApplication())
        return playlistToAddToQueue.value?.let { playlistsRepo.getPlaylist(it.getName()) }
    }
    fun setPlaylistToAddToQueue(playlistToAddToQueue: RandomPlaylist?) {
        _playlistToAddToQueue.postValue(playlistToAddToQueue)
        _songToAddToQueue.postValue(null)
    }

    private val _songToAddToQueue = MutableLiveData<Long?>()
    private val songToAddToQueue = _songToAddToQueue as LiveData<Long?>
    private fun getSongToAddToQueue(): Song? {
        val playlistsRepo = PlaylistsRepo.getInstance(getApplication())
        return songToAddToQueue.value?.let { playlistsRepo.getSong(it) }
    }
    fun setSongToAddToQueue(songToAddToQueue: Long?) {
        _songToAddToQueue.postValue(songToAddToQueue)
        _playlistToAddToQueue.postValue(null)
    }

    fun addToQueue(context: Context, song: Song) {
        val mediaSession: MediaSession = MediaSession.getInstance(context)
        val songQueue = SongQueue.getInstance()
        songQueue.addToQueue(context, song.id)
        val mediaPlayerSession = MediaPlayerManager.getInstance(context)
        if (mediaPlayerSession.isSongInProgress() == false) {
            mediaSession.playNext(context)
        }
    }

    fun addToQueue(context: Context, randomPlaylist: RandomPlaylist) {
        val mediaSession: MediaSession = MediaSession.getInstance(context)
        val mediaPlayerSession = MediaPlayerManager.getInstance(context)
        val songQueue = SongQueue.getInstance()
        val songs = randomPlaylist.getSongs()
        for (song in songs) {
            songQueue.addToQueue(context, song.id)
        }
        if (mediaPlayerSession.isSongInProgress() == false) {
            mediaSession.playNext(context)
        }
    }

    fun actionAddToQueue(context: Context) {
        val songQueue = SongQueue.getInstance()
        // TODO pretty sure song and playlist could be non-null at the same time
        songToAddToQueue.value?.let { songQueue.addToQueue(context, it) }
        playlistToAddToQueue.value?.getSongs()?.let {
            for (songs in it) {
                songQueue.addToQueue(context, songs.id)
            }
        }
        // TODO Song will play even though user might not want it. Make a setting.
        val mediaPlayerManager = MediaPlayerManager.getInstance(context)
        val mediaSession: MediaSession = MediaSession.getInstance(context)
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

}