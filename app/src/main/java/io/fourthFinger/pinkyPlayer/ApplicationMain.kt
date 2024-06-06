package io.fourthFinger.pinkyPlayer
import android.app.Application
import io.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import io.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import io.fourthFinger.pinkyPlayer.random_playlist.SongQueue
import io.fourthFinger.pinkyPlayer.random_playlist.SongRepo
import io.fourthFinger.pinkyPlayer.random_playlist.UseCaseEditPlaylist
import io.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker
import io.fourthFinger.pinkyPlayer.settings.SettingsRepo
import io.fourthFinger.playlistDataSource.PlaylistsRepo

class ApplicationMain : Application() {

    var settingsRepo: SettingsRepo? = SettingsRepo()
    var playlistsRepo: PlaylistsRepo? = PlaylistsRepo()

    var songRepo: SongRepo? = null
    var songQueue: SongQueue? = null
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
        songQueue = SongQueue(songRepo!!)
        songPicker = UseCaseSongPicker(songRepo!!)

        val mediaPlayerManager = MediaPlayerManager()
        mediaSession = playlistsRepo?.let {
            MediaSession(
                it,
                mediaPlayerManager,
                songQueue!!
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
        songQueue = null
        songPicker = null
        mediaSession = null
        playlistEditor = null
    }
    
}