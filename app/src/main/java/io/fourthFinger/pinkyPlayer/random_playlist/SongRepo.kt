package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context

class SongRepo(context: Context) {

    private val mediaDatasource = MediaDatasource(context)
    val loadingText = mediaDatasource.loadingText
    val loadingProgress = mediaDatasource.loadingProgress

    fun loadSongs(
        playlistsRepo: PlaylistsRepo,
        context: Context
    ){
        mediaDatasource.loadSongsFromMediaStore(context, playlistsRepo)
    }

    fun getSong(id: Long): Song? {
        return mediaDatasource.getSongFromDatabase(id)
    }

    fun getAllSongs(): List<Song> {
        return mediaDatasource.allSongs
    }

}