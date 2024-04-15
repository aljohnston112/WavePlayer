package com.fourthFinger.pinkyPlayer

import android.app.Application
import com.fourthFinger.pinkyPlayer.random_playlist.MediaPlayerManager
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.SaveFile
import com.fourthFinger.pinkyPlayer.random_playlist.SongQueue
import com.fourthFinger.pinkyPlayer.random_playlist.SongRepo
import com.fourthFinger.pinkyPlayer.settings.SettingsRepo

class ApplicationMain : Application() {

    val songRepo = SongRepo.getInstance()
    val settingsRepo = SettingsRepo.getInstance()
    val playlistsRepo = PlaylistsRepo.getInstance()
    val songQueue = SongQueue.getInstance(playlistsRepo)
    val mediaPlayerManager = MediaPlayerManager.getInstance()
    lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        val settings = settingsRepo.loadSettings(this)
        playlistsRepo.loadDatabase(this)
        // Loading the save file is needed to ensure the master playlist is valid
        // before getting songs from the MediaStore.
        SaveFile.loadSaveFile(
            this,
            playlistsRepo,
            settings.maxPercent
        )
        mediaSession = MediaSession.getInstance(
            playlistsRepo,
            mediaPlayerManager,
            songQueue
        )
        mediaPlayerManager.setUp(
            this,
            mediaSession
        )
    }

}