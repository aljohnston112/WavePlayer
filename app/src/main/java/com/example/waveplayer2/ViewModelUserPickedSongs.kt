package com.example.waveplayer2

import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import com.example.waveplayer2.random_playlist.Song
import java.util.*

class ViewModelUserPickedSongs : ViewModel() {

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
        userPickedSongs.clear()
    }
}