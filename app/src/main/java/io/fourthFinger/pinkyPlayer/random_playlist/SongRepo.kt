package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import io.fourthFinger.pinkyPlayer.settings.SettingsRepo
import io.fourthFinger.playlistDataSource.PlaylistsRepo
import io.fourthFinger.playlistDataSource.Song

class SongRepo(
    context: Context,
    private val settingsRepo: SettingsRepo
) {

    private val mediaDatasource = MediaDatasource(context)
    val loadingText = mediaDatasource.loadingText
    val loadingProgress = mediaDatasource.loadingProgress

    fun loadSongs(
        playlistsRepo: PlaylistsRepo,
        context: Context
    ){
        mediaDatasource.loadSongsFromMediaStore(
            context,
            playlistsRepo,
            settingsRepo.loadSettings(context).maxPercent
        )
    }

    fun getSong(id: Long): Song? {
        return mediaDatasource.getSongFromDatabase(id)
    }

    fun getAllSongs(): List<Song> {
        return mediaDatasource.allSongs
    }

    fun cleanUp() {
        mediaDatasource.cleanUp()
    }

}