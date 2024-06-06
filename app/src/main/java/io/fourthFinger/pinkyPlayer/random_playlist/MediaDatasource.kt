package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.playlistDataSource.AudioUri
import io.fourthFinger.playlistDataSource.PlaylistsRepo
import io.fourthFinger.playlistDataSource.Song
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A data source for music in the MediaStore.
 *
 * @param context
 */
class MediaDatasource(context: Context) {

    private var executorServicePool: ExecutorService? = Executors.newCachedThreadPool()

    private var database: SongDatabase? = null
    private var songDAO: SongDAO? = null

    private val _loadingText = MutableLiveData<String>()
    val loadingText = _loadingText as LiveData<String>

    private val _loadingProgress = MutableLiveData(0.0)
    val loadingProgress = _loadingProgress as LiveData<Double>

    private val _allSongs = mutableListOf<Song>()
    val allSongs = _allSongs as List<Song>

    init {
        loadDatabase(context)
    }

    /**
     * Loads this app's song database.
     *
     * @param context
     */
    private fun loadDatabase(context: Context) {
        database = Room.databaseBuilder(
            context, SongDatabase::class.java,
            SONG_DATABASE_NAME
        ).build()
        songDAO = database?.songDAO()
    }

    /**
     * Gets a Song from the database.
     *
     * @param songID The id of the Song to get from the database.
     *
     * @return The Song if it was found in the database, else null.
     */
    fun getSongFromDatabase(songID: Long): Song? {
        var song: Song? = null
        try {
            song = executorServicePool?.submit(
                Callable {
                    songDAO?.getSong(songID)
                }
            )?.get()
            // TODO better error handling
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return song
    }

    /**
     * Loads songs from the MediaStore and updates the app's backend cache.
     *
     * @param context
     * @param playlistsRepo
     */
    fun loadSongsFromMediaStore(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        maxPercent: Double
    ) {
        val executorFIFO = Executors.newSingleThreadExecutor()
        executorFIFO.execute {
            val resources = context.resources
            playlistsRepo.loadPlaylists(
                context,
                maxPercent
            )

            val masterPlaylist = playlistsRepo.playlists.value?.masterPlaylist!!

            _loadingText.postValue(
                resources.getString(R.string.loading1)
            )
            val newSongs = scanMediaStoreForNewMusic(
                context,
                masterPlaylist.getSongIDs()
            )

            _loadingText.postValue(
                resources.getString(R.string.loading2)
            )
            addNewSongs(
                context,
                playlistsRepo,
                newSongs
            )

            _loadingText.postValue(
                resources.getString(R.string.loading3)
            )
            removeMissingSongs(
                context,
                playlistsRepo
            )

            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = context.resources.getString(R.string.action_loaded)
            context.sendBroadcast(intent)
        }
    }

    /**
     * Gets new music from the MediaStore.
     *
     * @param context
     * @param currentMusic A list of all the cached music.
     *
     * @return Music in the MediaStore that is not in currentMusic.
     */
    private fun scanMediaStoreForNewMusic(
        context: Context,
        currentMusic: List<Long>
    ): List<Song> {
        val newSongs = mutableListOf<Song>()
        val cursor = queryMediaStoreForMusic(context)
        if (cursor != null) {
            val idCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media._ID
            )
            val nameCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.DISPLAY_NAME
            )
            val titleCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.TITLE
            )
            val artistCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ARTIST_ID
            )
            val numberOfSongs = cursor.count
            var currentSongNumber = 0
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol)
                val title = cursor.getString(titleCol)
                val artist = cursor.getString(artistCol)

                val song = Song(id, title)
                if (!currentMusic.contains(id)) {
                    AudioUri.saveAudioUri(
                        context,
                        AudioUri(
                            displayName,
                            artist,
                            title,
                            id
                        )
                    )
                    newSongs.add(song)
                }
                _allSongs.add(song)
                _loadingProgress.postValue(
                    currentSongNumber.toDouble() / numberOfSongs.toDouble()
                )
                currentSongNumber++
            }
        }
        cursor?.close()
        return newSongs
    }

    /**
     * Does a query to get all the music in the MediaStore.
     *
     * @param context
     *
     * @return A cursor for the results of the query,
     * or null if the query failed.
     */
    private fun queryMediaStoreForMusic(context: Context): Cursor? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.TITLE
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != ?"
        val selectionArgs = arrayOf("0")
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        return context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
    }

    /**
     *  Adds new songs to the master playlist.
     *
     *  @param context
     *  @param playlistsRepo
     *  @param newSongs A list of new songs.
     */
    private fun addNewSongs(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        newSongs: List<Song>
    ) {
        val numberOfNewSongs = newSongs.size
        for ((i, song) in newSongs.withIndex()) {
            songDAO?.insertAll(song)
            playlistsRepo.addToMasterPlaylist(
                context,
                song
            )
            _loadingProgress.postValue(
                i.toDouble() / numberOfNewSongs.toDouble()
            )
        }
    }

    /**
     * Removes missing songs from the master playlist.
     *
     * @param context
     * @param playlistsRepo
     */
    private fun removeMissingSongs(
        context: Context,
        playlistsRepo: PlaylistsRepo
    ) {
        val masterPlaylist = playlistsRepo.getMasterPlaylist()
        val numberOfSongs = _allSongs.size
        val oldSongs = masterPlaylist.getSongs()
        for ((j, song) in oldSongs.withIndex()) {
            if (!_allSongs.contains(song)) {
                getSongFromDatabase(song.id)?.let {
                    songDAO?.delete(it)
                    playlistsRepo.removeFromMasterPlaylist(
                        context,
                        song
                    )
                }
            }
            _loadingProgress.postValue(
                j.toDouble() / numberOfSongs.toDouble()
            )
        }
    }

    fun cleanUp() {
        executorServicePool?.shutdownNow()
        executorServicePool = null
        database?.close()
        database = null
        songDAO = null
    }

    companion object {
        const val SONG_DATABASE_NAME: String = "SONG_DATABASE_NAME"
    }

}