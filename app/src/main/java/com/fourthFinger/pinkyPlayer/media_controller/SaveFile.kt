package com.fourthFinger.pinkyPlayer.media_controller

import android.content.Context
import com.fourthFinger.pinkyPlayer.FileUtil
import com.fourthFinger.pinkyPlayer.fragments.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Settings
import com.fourthFinger.pinkyPlayer.random_playlist.SettingsRepo

object SaveFile {

    private const val SAVE_FILE_VERIFICATION_NUMBER = 4596834290567902435L
    private const val FILE_NAME_SETTINGS = "SETTINGS"
    private const val FILE_NAME_MASTER_PLAYLIST = "MASTER_PLAYLIST"
    private const val FILE_NAME_PLAYLISTS = "PLAYLISTS"

    private val settingsRepo = SettingsRepo.getInstance()

    fun loadSaveFile(context: Context) {
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        FileUtil.load<Settings>(
            context,
            FILE_NAME_SETTINGS,
            SAVE_FILE_VERIFICATION_NUMBER
        )?.let { settingsRepo.setSettings(it) }
        FileUtil.load<RandomPlaylist>(
            context,
            FILE_NAME_MASTER_PLAYLIST,
            SAVE_FILE_VERIFICATION_NUMBER
        )?.let { playlistsRepo.setMasterPlaylist(it) }
        FileUtil.loadList<RandomPlaylist>(
            context,
            FILE_NAME_PLAYLISTS,
            SAVE_FILE_VERIFICATION_NUMBER
        )?.let { playlistsRepo.addPlaylistsFromSaveFile(it) }
    }


    fun saveFile(context: Context) {
        ServiceMain.executorServicePool.submit {
            val playlistsRepo = PlaylistsRepo.getInstance(context)
            val settingsRepo = SettingsRepo.getInstance()
            FileUtil.save(
                settingsRepo.getSettings(),
                context,
                FILE_NAME_SETTINGS,
                SAVE_FILE_VERIFICATION_NUMBER
            )
            playlistsRepo.getMasterPlaylist()?.let {
                FileUtil.save(
                    it,
                    context,
                    FILE_NAME_MASTER_PLAYLIST,
                    SAVE_FILE_VERIFICATION_NUMBER
                )
            }
            FileUtil.saveList(
                playlistsRepo.getPlaylists(),
                context,
                FILE_NAME_PLAYLISTS,
                SAVE_FILE_VERIFICATION_NUMBER
            )
        }
    }
}