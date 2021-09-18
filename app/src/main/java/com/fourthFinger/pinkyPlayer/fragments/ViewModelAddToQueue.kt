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
import com.fourthFinger.pinkyPlayer.media_controller.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import com.fourthFinger.pinkyPlayer.random_playlist.SongQueue

class ViewModelAddToQueue(application: Application): AndroidViewModel(application) {

    private val playlistsRepo = PlaylistsRepo.getInstance(application)
    private val songQueue = SongQueue.getInstance()

    private val _playlistToAddToQueue = MutableLiveData<RandomPlaylist?>()
    private val playlistToAddToQueue = _playlistToAddToQueue as LiveData<RandomPlaylist?>
    fun setPlaylistToAddToQueue(playlistToAddToQueue: RandomPlaylist?) {
        _playlistToAddToQueue.value = playlistToAddToQueue
    }
    private fun getPlaylistToAddToQueue(): RandomPlaylist? {
        return playlistToAddToQueue.value?.let { playlistsRepo.getPlaylist(it.getName()) }
    }

    private val _songToAddToQueue = MutableLiveData<Long?>()
    private val songToAddToQueue = _songToAddToQueue as LiveData<Long?>
    private fun getSongToAddToQueue(): Song? {
        return songToAddToQueue.value?.let { playlistsRepo.getSong(it) }
    }

    fun newSong(songToAddToQueue: Long?) {
        _songToAddToQueue.value = songToAddToQueue
        setPlaylistToAddToQueue(null)
    }

    fun actionAddToQueue(context: Context) {
        // TODO pretty sure song and playlist could be non-null at the same time
        songToAddToQueue.value?.let { songQueue.addToQueue(it) }
        playlistToAddToQueue.value?.getSongs()?.let {
            for (songs in it) {
                songQueue.addToQueue(songs.id)
            }
        }
        // TODO Song will play even though user might not want it.
        // Should be able to show the song pane with the first song.
        val mediaSession: MediaSession = MediaSession.getInstance(context)
        if (!mediaSession.isSongInProgress()) {
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

    fun addToQueueClicked(context: Context, song: Song) {
        val mediaSession: MediaSession = MediaSession.getInstance(context)
        val songQueue = SongQueue.getInstance()
        songQueue.addToQueue(song.id)
        if (!mediaSession.isSongInProgress()) {
            mediaSession.playNext(context)
        }
    }


    fun addToQueueClicked(context: Context, randomPlaylist: RandomPlaylist) {
        // TODO stop MasterPlaylist from continuing after queue is done
        // shuffle is off and looping is on or something like that?
        val mediaSession: MediaSession = MediaSession.getInstance(context)
        mediaSession.setCurrentPlaylistToMaster(context)
        val songQueue = SongQueue.getInstance()
        val songs = randomPlaylist.getSongs()
        for (song in songs) {
            songQueue.addToQueue(song.id)
        }
        if (!mediaSession.isSongInProgress()) {
            mediaSession.playNext(context)
        }
    }

}