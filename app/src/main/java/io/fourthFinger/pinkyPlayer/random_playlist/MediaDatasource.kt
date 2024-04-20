package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.pinkyPlayer.ServiceMain
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MediaDatasource {

    private val _loadingText: MutableLiveData<String> = MutableLiveData()
    val loadingText = _loadingText as LiveData<String>

    private val _loadingProgress: MutableLiveData<Double> = MutableLiveData(0.0)
    val loadingProgress = _loadingProgress as LiveData<Double>

    private lateinit var songDAO: SongDAO

    private val _allSongs = mutableListOf<Song>()
    val allSongs = _allSongs as List<Song>

    fun loadDatabase(context: Context) {
        val songDatabase = Room.databaseBuilder(
            context, SongDatabase::class.java,
            SONG_DATABASE_NAME
        ).build()
        songDAO = songDatabase.songDAO()
    }

    fun getSongFromDatabase(songID: Long): Song? {
        var song: Song? = null
        try {
            song = ServiceMain.executorServicePool.submit(
                Callable {
                    songDAO.getSong(songID)
                }
            ).get()
            // TODO better error handling
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return song
    }

    fun loadSongs(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        mediaPlayerManager: MediaPlayerManager
    ) {
        loadSongsFromMediaStore(
            context,
            playlistsRepo,
            mediaPlayerManager
        )
    }

    private fun loadSongsFromMediaStore(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        mediaPlayerManager: MediaPlayerManager
    ) {
        val executorFIFO: ExecutorService = Executors.newSingleThreadExecutor()
        executorFIFO.execute {
            val masterPlaylist = playlistsRepo.getMasterPlaylist()
            _allSongs.addAll(masterPlaylist.getSongs())
            val songsThatExist =  mutableListOf<Long>()
            val newSongs = scanDatabaseForNewMusic(
                context,
                masterPlaylist.getSongIDs(),
                songsThatExist
            )
            insertNewSongsIntoDatabase(
                context,
                newSongs
            )
            val resources = context.resources
            _loadingText.postValue(
                resources.getString(R.string.loading3)
            )
            addNewSongsToMasterPlaylist(
                context,
                playlistsRepo,
                newSongs
            )
            _loadingText.postValue(
                resources.getString(R.string.loading4)
            )
            removeMissingSongsFromMasterPlaylist(
                context,
                playlistsRepo,
                mediaPlayerManager,
                songsThatExist
            )
            SaveFile.saveFile(
                context,
                playlistsRepo
            )

            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = context.resources.getString(R.string.action_loaded)
            context.sendBroadcast(intent)
        }
    }

    private fun scanDatabaseForNewMusic(
        context: Context,
        currentMusic: List<Long>,
        songsThatExist: MutableList<Long>
    ): List<Song> {
        val resources = context.resources
        val newSongs = mutableListOf<Song>()
        _loadingText.postValue(
            resources.getString(R.string.loading1)
        )
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
                    val song = Song(id, title)
                    _allSongs.add(song)
                    newSongs.add(song)
                }
                songsThatExist.add(id)
                _loadingProgress.postValue(currentSongNumber.toDouble() / numberOfSongs.toDouble())
                currentSongNumber++
            }
            _loadingText.postValue(resources.getString(R.string.loading2))
        }
        return newSongs
    }

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

    private fun insertNewSongsIntoDatabase(
        context: Context,
        newSongs: List<Song>
    ) {
        val numberOfNewSongs = newSongs.size
        _loadingText.postValue(
            context.resources.getString(R.string.loading2)
        )
        for ((k, song) in newSongs.withIndex()) {
            songDAO.insertAll(song)
            _loadingProgress.postValue(
                k.toDouble() / numberOfNewSongs.toDouble()
            )
        }
    }


    private fun addNewSongsToMasterPlaylist(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        newSongs: List<Song>
    ) {
        val masterPlaylist = playlistsRepo.getMasterPlaylist()
        val numberOfNewSongs = newSongs.size
        for ((i, songID) in newSongs.withIndex()) {
            masterPlaylist.add(songID)
            SaveFile.saveFile(
                context,
                playlistsRepo
            )
            _loadingProgress.postValue(
                i.toDouble() / numberOfNewSongs.toDouble()
            )
        }
    }

    private fun removeMissingSongsFromMasterPlaylist(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        mediaPlayerManager: MediaPlayerManager,
        songsThatExist: List<Long>
    ) {
        val masterPlaylist = playlistsRepo.getMasterPlaylist()
        val numberOfSongs = songsThatExist.size
        val ids = masterPlaylist.getSongIDs()
        for ((j, songID) in ids.withIndex()) {
            if (!songsThatExist.contains(songID)) {
                getSongFromDatabase(songID)?.let {
                    masterPlaylist.remove(it)
                    SaveFile.saveFile(
                        context,
                        playlistsRepo
                    )
                    songDAO.delete(it)
                }
                mediaPlayerManager.removeMediaPlayerWUri(songID)
            }
            _loadingProgress.postValue(
                j.toDouble() / numberOfSongs.toDouble()
            )
        }
    }

    companion object {
        const val SONG_DATABASE_NAME: String = "SONG_DATABASE_NAME"
    }

}