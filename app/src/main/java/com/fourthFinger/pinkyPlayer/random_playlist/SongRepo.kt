package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context

class SongRepo {

    private val mediaDatasource = MediaDatasource.getInstance()
    val loadingText = mediaDatasource.loadingText
    val loadingProgress = mediaDatasource.loadingProgress

    fun loadSongs(
        playlistsRepo: PlaylistsRepo,
        mediaPlayerManager: MediaPlayerManager,
        context: Context
    ){
        mediaDatasource.loadSongs(context, playlistsRepo, mediaPlayerManager)
    }
    companion object {

        private var INSTANCE: SongRepo? = null

        fun getInstance(): SongRepo {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = SongRepo()
                }
                return INSTANCE!!
            }
        }
    }

}