package io.fourth_finger.pinky_player
import android.app.Application
import io.fourth_finger.pinky_player.random_playlist.MediaPlayerManager
import io.fourth_finger.pinky_player.random_playlist.MediaSession
import io.fourth_finger.pinky_player.random_playlist.SongRepo
import io.fourth_finger.pinky_player.random_playlist.UseCaseEditPlaylist
import io.fourth_finger.pinky_player.random_playlist.UseCaseSongPicker
import io.fourth_finger.pinky_player.settings.SettingsRepo
import io.fourth_finger.playlist_data_source.PlaylistsRepo

class ApplicationMain : Application() {

    var settingsRepo: SettingsRepo? = SettingsRepo()
    var playlistsRepo: PlaylistsRepo? = PlaylistsRepo()

    var songRepo: SongRepo? = null
    var songPicker: UseCaseSongPicker? = null
    var mediaSession: MediaSession? = null
    var playlistEditor: UseCaseEditPlaylist? = null

    override fun onCreate() {
        super.onCreate()
        val settings = settingsRepo?.loadSettings(this)
        // Loading the save file is needed to ensure the master playlist is valid
        // before getting songs from the MediaStore.
        settings?.maxPercent?.let {
            playlistsRepo?.loadPlaylists(
                this,
                it
            )
        }

        songRepo = settingsRepo?.let { SongRepo(this, it) }!!
        songPicker = UseCaseSongPicker(songRepo!!)

        val mediaPlayerManager = MediaPlayerManager()
        mediaSession = playlistsRepo?.let {
            MediaSession(
                it,
                songRepo!!,
                mediaPlayerManager
            )
        }!!
        mediaPlayerManager.setUp(
            this,
            mediaSession!!
        )
        playlistEditor = UseCaseEditPlaylist(
            playlistsRepo!!,
            mediaSession!!
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
    
}