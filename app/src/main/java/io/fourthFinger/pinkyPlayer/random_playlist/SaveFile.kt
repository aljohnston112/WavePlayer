package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import io.fourthFinger.pinkyPlayer.FileUtil
import io.fourthFinger.pinkyPlayer.ServiceMain
import java.util.ArrayList

object SaveFile {

    // TODO do not queue every single save. Only the last save needs to be done.

    private const val SAVE_FILE_VERIFICATION_NUMBER = 4596834290567902435L

    private const val FILE_NAME_MASTER_PLAYLIST = "MASTER_PLAYLIST"
    private const val FILE_NAME_PLAYLISTS = "PLAYLISTS"
    private const val MASTER_PLAYLIST_NAME: String = "MASTER_PLAYLIST_NAME"

    private val FILE_LOCK: Any = Any()

    /**
     * Loads the playlists saved on disk.
     * If a master playlist has not been saved, a new one will be created.
     *
     * @param context
     * @param playlistsRepo
     * @param maxPercent The max percent that a song can have in a playlist.
     */
    fun loadSaveFile(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        maxPercent: Double
    ) {
        synchronized(FILE_LOCK) {
            val loadedMasterPlaylist = FileUtil.load<RandomPlaylist>(
                context,
                FILE_NAME_MASTER_PLAYLIST,
                SAVE_FILE_VERIFICATION_NUMBER
            )
            loadedMasterPlaylist?.let { masterPlaylist ->
                playlistsRepo.setMasterPlaylist(masterPlaylist)
            } ?: run {
                playlistsRepo.setMasterPlaylist(
                    RandomPlaylist(
                        MASTER_PLAYLIST_NAME,
                        ArrayList(),
                        true,
                        maxPercent
                    )
                )
            }

            FileUtil.loadList<RandomPlaylist>(
                context,
                FILE_NAME_PLAYLISTS,
                SAVE_FILE_VERIFICATION_NUMBER
            )?.let { playlists ->
                playlistsRepo.addPlaylistsFromSaveFile(playlists)
            }
        }
    }

    /**
     * Saves all playlists to disk.
     *
     * @param context
     * @param playlistsRepo
     */
    fun saveFile(
        context: Context,
        playlistsRepo: PlaylistsRepo
    ) {
        ServiceMain.executorServicePool.submit {
            synchronized(FILE_LOCK) {
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