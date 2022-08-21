package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import com.fourthFinger.pinkyPlayer.FileUtil
import com.fourthFinger.pinkyPlayer.ServiceMain

object SaveFile {

    // TODO do not queue every single save. Only the last save needs to be done.

    private const val SAVE_FILE_VERIFICATION_NUMBER = 4596834290567902435L

    private const val FILE_NAME_MASTER_PLAYLIST = "MASTER_PLAYLIST"
    private const val FILE_NAME_PLAYLISTS = "PLAYLISTS"

    private val FILE_LOCK: Any = Any()

    fun loadSaveFile(context: Context) {
        synchronized(FILE_LOCK) {
            val playlistsRepo = PlaylistsRepo.getInstance(context)
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
    }


    fun saveFile(context: Context) {
        ServiceMain.executorServicePool.submit {
            synchronized(FILE_LOCK) {
                val playlistsRepo = PlaylistsRepo.getInstance(context)
                FileUtil.save(
                    playlistsRepo.getMasterPlaylist(),
                    context,
                    FILE_NAME_MASTER_PLAYLIST,
                    SAVE_FILE_VERIFICATION_NUMBER
                )
                FileUtil.saveList(
                    playlistsRepo.getPlaylists(),
                    context,
                    FILE_NAME_PLAYLISTS,
                    SAVE_FILE_VERIFICATION_NUMBER
                )
            }
        }
    }

}