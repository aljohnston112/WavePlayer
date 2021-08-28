package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import android.content.Context
import androidx.annotation.GuardedBy
import androidx.lifecycle.AndroidViewModel
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ToastUtil
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import java.util.ArrayList

class ViewModelPlaylists(application: Application): AndroidViewModel(application) {

    private val playlistsRepo = PlaylistsRepo.getInstance(application)

    @GuardedBy("this")
    private val userPickedSongs: MutableList<Song> = ArrayList()
    @Synchronized
    fun getUserPickedSongs(): List<Song> {
        return userPickedSongs
    }
    @Synchronized
    fun addUserPickedSong(songs: Song) {
        userPickedSongs.add(songs)
    }
    @Synchronized
    fun removeUserPickedSong(song: Song) {
        userPickedSongs.remove(song)
    }
    @Synchronized
    fun clearUserPickedSongs() {
        for (song in userPickedSongs) {
            song.setSelected(false)
        }
        userPickedSongs.clear()
    }

    @GuardedBy("this")
    @Volatile
    private var userPickedPlaylist: RandomPlaylist? = null
    @Synchronized
    fun getUserPickedPlaylist(): RandomPlaylist? {
        return userPickedPlaylist
    }
    @Synchronized
    fun setUserPickedPlaylist(userPickedPlaylist: RandomPlaylist?) {
        this.userPickedPlaylist = userPickedPlaylist
    }

    fun getPlaylistTitles(): Array<String?> {
        return getPlaylistTitles(playlistsRepo.getPlaylists())
    }

    private fun getPlaylistTitles(randomPlaylists: List<RandomPlaylist>): Array<String?> {
        val titles: MutableList<String> = ArrayList(randomPlaylists.size)
        for (randomPlaylist in randomPlaylists) {
            titles.add(randomPlaylist.getName())
        }
        val titlesArray = arrayOfNulls<String>(titles.size)
        var i = 0
        for (title in titles) {
            titlesArray[i++] = title
        }
        return titlesArray
    }

    fun doesPlaylistExist(playlistName: String): Boolean {
        var playlistIndex = -1
        for ((i, randomPlaylist) in getPlaylists().withIndex()) {
            if (randomPlaylist.getName() == playlistName) {
                playlistIndex = i
            }
        }
        return playlistIndex == -1
    }

    fun getPlaylists(): List<RandomPlaylist> {
        return playlistsRepo.getPlaylists()
    }

    fun addNewPlaylist(randomPlaylist: RandomPlaylist) {
        playlistsRepo.addPlaylist(randomPlaylist)
    }

    fun removePlaylist(userPickedPlaylist: RandomPlaylist) {
        playlistsRepo.removePlaylist(userPickedPlaylist)
    }

    fun getSong(songID: Long): Song? {
        return playlistsRepo.getSong(songID)
    }

    fun addNewPlaylist(position: Int, randomPlaylist: RandomPlaylist) {
        playlistsRepo.addPlaylist(position, randomPlaylist)
    }

    fun getAllSongs(): List<Song>? {
        return playlistsRepo.getAllSongs()
    }

    fun getMasterPlaylist(): RandomPlaylist? {
        return playlistsRepo.getMasterPlaylist()
    }

    fun getPlaylist(it: String): RandomPlaylist? {
        return playlistsRepo.getPlaylist(it)
    }

    fun addNewPlaylist(name: String, songs: MutableList<Song>) {
        playlistsRepo.addPlaylist(RandomPlaylist(name, songs, false))
        clearUserPickedSongs()
    }

    fun fragmentEditPlaylistviewCreated() {
        setPickedSongsToCurrentPlaylist()
    }

    @Synchronized
    private fun setPickedSongsToCurrentPlaylist() {
        userPickedPlaylist?.let {
            for (song in it.getSongs()) {
                addUserPickedSong(song)
            }
        }
    }

    fun populateUserPickedSongs() {
        // userPickedSongs.isEmpty() when the user is editing a playlist
        // TODO if user is editing a playlist, unselects all the songs and returns here, ERROR
        if (userPickedSongs.isEmpty()) {
            getUserPickedPlaylist()?.getSongs()?.let {
                userPickedSongs.addAll(it)
            }
        }
    }

    fun validatePlaylistFields(context: Context, playlistName: String): Boolean {
        val userPickedSongs = getUserPickedSongs().toMutableList()
        val userPickedPlaylist = getUserPickedPlaylist()
        val makingNewPlaylist = (userPickedPlaylist == null)
        return if (userPickedSongs.size == 0) {
            ToastUtil.showToast(context, R.string.not_enough_songs_for_playlist)
            false
        } else if (playlistName.isEmpty()) {
            ToastUtil.showToast(context, R.string.no_name_playlist)
            false
        } else if (
            doesPlaylistExist(playlistName) &&
            makingNewPlaylist
        ) {
            ToastUtil.showToast(context, R.string.duplicate_name_playlist)
            false
        } else {
            true
        }
    }

}