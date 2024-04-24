package io.fourthFinger.pinkyPlayer
import android.app.Application
import io.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import io.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import io.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import io.fourthFinger.pinkyPlayer.random_playlist.SaveFile
import io.fourthFinger.pinkyPlayer.random_playlist.SongQueue
import io.fourthFinger.pinkyPlayer.random_playlist.SongRepo
import io.fourthFinger.pinkyPlayer.random_playlist.UseCaseEditPlaylist
import io.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker
import io.fourthFinger.pinkyPlayer.settings.SettingsRepo

class ApplicationMain : Application() {

    val settingsRepo = SettingsRepo()
    val playlistsRepo = PlaylistsRepo()
    val mediaPlayerManager = MediaPlayerManager()

    lateinit var songRepo: SongRepo
    lateinit var songQueue: SongQueue
    lateinit var songPicker: UseCaseSongPicker
    lateinit var mediaSession: MediaSession
    lateinit var playlistEditor: UseCaseEditPlaylist

    override fun onCreate() {
        super.onCreate()
        val settings = settingsRepo.loadSettings(this)
        // Loading the save file is needed to ensure the master playlist is valid
        // before getting songs from the MediaStore.
        SaveFile.loadSaveFile(
            this,
            playlistsRepo,
            settings.maxPercent
        )

        songRepo = SongRepo(this)
        songQueue = SongQueue(songRepo)
        songPicker = UseCaseSongPicker(songRepo)
        mediaSession = MediaSession(
            playlistsRepo,
            mediaPlayerManager,
            songQueue
        )
        mediaPlayerManager.setUp(
            this,
            mediaSession
        )
        playlistEditor = UseCaseEditPlaylist(
            playlistsRepo,
            mediaSession
        )
    }

}