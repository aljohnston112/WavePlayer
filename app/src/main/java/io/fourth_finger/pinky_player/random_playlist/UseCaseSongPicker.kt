package io.fourth_finger.pinky_player.random_playlist

import io.fourth_finger.playlist_data_source.Song

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
        playlist: io.fourth_finger.playlist_data_source.RandomPlaylist
    ) {
        for (song in playlist.getSongs()) {
            val s = allSongs.first { it.id == song.id }
            s.setSelected(true)
        }
    }

}