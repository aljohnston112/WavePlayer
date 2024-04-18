package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import androidx.room.Room
import io.fourthFinger.pinkyPlayer.ServiceMain
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

class SongRepo {

    private val mediaDatasource = MediaDatasource()
    val loadingText = mediaDatasource.loadingText
    val loadingProgress = mediaDatasource.loadingProgress

    fun loadDatabase(context: Context) {
        mediaDatasource.loadDatabase(context)
    }

    fun loadSongs(
        playlistsRepo: PlaylistsRepo,
        mediaPlayerManager: MediaPlayerManager,
        context: Context
    ){
        mediaDatasource.loadSongs(context, playlistsRepo, mediaPlayerManager)
    }

    fun getSong(id: Long): Song? {
        return mediaDatasource.getSong(id)
    }

    fun getAllSongs(): List<Song> {
        return mediaDatasource.allSongs
    }

}