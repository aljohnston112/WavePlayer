package io.fourth_finger.pinky_player.random_playlist

import android.content.Context
import io.fourth_finger.pinky_player.settings.SettingsRepo
import io.fourth_finger.playlist_data_source.PlaylistsRepo
import io.fourth_finger.playlist_data_source.Song

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