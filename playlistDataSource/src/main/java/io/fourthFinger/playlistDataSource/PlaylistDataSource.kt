package io.fourthFinger.playlistDataSource

import android.content.Context
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal class PlaylistDataSource {

    private var thread: ExecutorService? = Executors.newSingleThreadExecutor()

    private var latestSave: PlaylistList? = null
    private var saving = AtomicBoolean(false)

    /**
     * Loads the playlists saved on disk.
     * If a master playlist has not been saved, a new one will be created.
     *
     * @param context
     * @param maxPercent The max percent that a song can have in a playlist.
     */
    fun loadPlaylists(
        context: Context,
        maxPercent: Double
    ): PlaylistList {
        synchronized(FILE_LOCK) {
            val masterPlaylist = FileUtil.load(
                context,
                FILE_NAME_MASTER_PLAYLIST,
                SAVE_FILE_VERIFICATION_NUMBER
            ) ?: RandomPlaylist(
                MASTER_PLAYLIST_NAME,
                ArrayList(),
                true,
                maxPercent
            )

            val playlists = FileUtil.loadList<RandomPlaylist>(
                context,
                FILE_NAME_PLAYLISTS,
                SAVE_FILE_VERIFICATION_NUMBER
            ) ?: emptyList()

            return PlaylistList(masterPlaylist, playlists)
        }
    }

    /**
     * Saves all playlists to disk.
     *
     * @param context
     */
    fun savePlaylists(
        context: Context,
        playlistList: PlaylistList
    ): PlaylistList {
        if (saving.get()) {
            latestSave = playlistList
        } else {
            thread?.submit {
                synchronized(FILE_LOCK) {
                    saving.set(true)
                    try {
                        FileUtil.save(
                            playlistList.masterPlaylist,
                            context,
                            FILE_NAME_MASTER_PLAYLIST,
                            SAVE_FILE_VERIFICATION_NUMBER
                        )

                        FileUtil.saveList(
                            playlistList.playlists,
                            context,
                            FILE_NAME_PLAYLISTS,
                            SAVE_FILE_VERIFICATION_NUMBER
                        )
                    } finally {
                        saving.set(false)
                        thread?.submit {
                            latestSave?.let {
                                savePlaylists(
                                    context,
                                    it
                                )
                                latestSave = null
                            }
                        }
                    }
                }
            }
        }
        return playlistList
    }

    fun cleanUp(){
        thread?.shutdownNow()
        thread = null
    }

    companion object {
        // TODO do not queue every single save. Only the last save needs to be done.

        private const val SAVE_FILE_VERIFICATION_NUMBER = 4596834290567902435L

        private const val FILE_NAME_MASTER_PLAYLIST = "MASTER_PLAYLIST"
        private const val FILE_NAME_PLAYLISTS = "PLAYLISTS"
        private const val MASTER_PLAYLIST_NAME: String = "MASTER_PLAYLIST_NAME"

        private val FILE_LOCK: Any = Any()
    }

}