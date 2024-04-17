package com.fourthFinger.pinkyPlayer.random_playlist

import androidx.annotation.GuardedBy

class UseCaseSongPicker {

    // TODO !!!Editing playlists is broken!!!

    @GuardedBy("this")
    private val userPickedSongs: MutableList<Song> = mutableListOf()

    @Synchronized
    fun getPickedSongs(): List<Song> {
        return userPickedSongs
    }

    @Synchronized
    fun selectSong(
        song: Song,
        masterPlaylist: RandomPlaylist
    ) {
        val allSongs = masterPlaylist.getSongs()
        val s = allSongs.first { it.id == song.id }
        userPickedSongs.add(s)
        s.setSelected(true)
    }

    @Synchronized
    fun unselectedSong(song: Song) {
        userPickedSongs.remove(song)
        song.setSelected(false)
    }

    @Synchronized
    fun unselectAllSongs() {
        for (song in userPickedSongs) {
            song.setSelected(false)
        }
        userPickedSongs.clear()
    }

    @Synchronized
    fun selectSongsInPlaylist(
        playlist: RandomPlaylist,
        masterPlaylist: RandomPlaylist
    ) {
        // TODO very inefficient and hacky
        val allSongs = masterPlaylist.getSongs()
        for (song in playlist.getSongs()) {
            val s = allSongs.first { it.id == song.id }
            userPickedSongs.add(s)
            s.setSelected(true)
        }
    }

}