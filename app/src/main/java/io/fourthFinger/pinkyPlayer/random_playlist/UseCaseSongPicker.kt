package io.fourthFinger.pinkyPlayer.random_playlist

import io.fourthFinger.playlistDataSource.Song

class UseCaseSongPicker(
    songRepo: SongRepo
) {

    private val allSongs = songRepo.getAllSongs()

    @Synchronized
    fun getPickedSongs(): List<Song> {
        return allSongs.filter { it.isSelected() }
    }

    @Synchronized
    fun selectSong(
        song: Song
    ) {
        val s = allSongs.first { it.id == song.id }
        s.setSelected(true)
    }

    @Synchronized
    fun unselectedSong(song: Song) {
        val s = allSongs.first { it.id == song.id }
        s.setSelected(false)
    }

    @Synchronized
    fun unselectAllSongs() {
        for (song in allSongs) {
            song.setSelected(false)
        }
    }

    @Synchronized
    fun selectSongsInPlaylist(
        playlist: io.fourthFinger.playlistDataSource.RandomPlaylist
    ) {
        for (song in playlist.getSongs()) {
            val s = allSongs.first { it.id == song.id }
            s.setSelected(true)
        }
    }

}