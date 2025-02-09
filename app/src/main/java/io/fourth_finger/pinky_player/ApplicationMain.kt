package io.fourth_finger.pinky_player

import android.app.Application
import io.fourth_finger.pinky_player.random_playlist.MediaPlayerManager
import io.fourth_finger.pinky_player.random_playlist.MediaSession
import io.fourth_finger.pinky_player.random_playlist.SongRepo
import io.fourth_finger.pinky_player.random_playlist.UseCaseEditPlaylist
import io.fourth_finger.pinky_player.random_playlist.UseCaseSongPicker
import io.fourth_finger.playlist_data_source.PlaylistsRepo
import io.fourth_finger.settings_repository.SettingsRepo

class ApplicationMain : Application() {

    var settingsRepo: SettingsRepo? =
        SettingsRepo()
    var playlistsRepo: PlaylistsRepo? = PlaylistsRepo()

    var songRepo: SongRepo? = null
    var songPicker: UseCaseSongPicker? = null
    var mediaSession: MediaSession? = null
    var playlistEditor: UseCaseEditPlaylist? = null

    override fun onCreate() {
        super.onCreate()


        val settingsRepo = this.settingsRepo
        val playlistsRepo = this.playlistsRepo
        if (settingsRepo == null || playlistsRepo == null) {
            return
        }

        val settings = settingsRepo.loadSettings(this)
        // Loading the save file is needed to ensure the master playlist is valid
        // before getting songs from the MediaStore.
        playlistsRepo.loadPlaylists(
            this,
            settings.maxPercent
        )

        val songRepo = SongRepo(
            this,
            settingsRepo
        )
        this.songRepo = songRepo
        songPicker = UseCaseSongPicker(songRepo)

        val mediaPlayerManager = MediaPlayerManager()
        val mediaSession = MediaSession(
            playlistsRepo,
            songRepo,
            mediaPlayerManager
        )
        this.mediaSession = mediaSession
        mediaPlayerManager.setUpListeners(
            this,
            mediaSession
        )
        playlistEditor = UseCaseEditPlaylist(
            playlistsRepo,
            mediaSession
        )
    }

    fun cleanUp() {
        mediaSession?.cleanUp(
            applicationContext
        )
        playlistsRepo?.cleanup()
        songRepo?.cleanUp()
        settingsRepo = null
        playlistsRepo = null
        songRepo = null
        songPicker = null
        mediaSession = null
        playlistEditor = null
    }

    // TODO When the queue is empty,
    //  it uses the playlist that the current user is in to refill the queue

}