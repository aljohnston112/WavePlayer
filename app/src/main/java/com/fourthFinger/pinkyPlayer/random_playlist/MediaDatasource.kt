package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ServiceMain
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MediaDatasource() {

    private val _loadingText: MutableLiveData<String> = MutableLiveData()
    val loadingText = _loadingText as LiveData<String>

    private val _loadingProgress: MutableLiveData<Double> = MutableLiveData(0.0)
    val loadingProgress = _loadingProgress as LiveData<Double>

    private lateinit var songDAO: SongDAO

    fun loadDatabase(context: Context) {
        val songDatabase = Room.databaseBuilder(
            context, SongDatabase::class.java,
            SONG_DATABASE_NAME
        ).build()
        songDAO = songDatabase.songDAO()
    }

    fun loadSongs(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        mediaPlayerManager: MediaPlayerManager
    ) {
        getSongsFromMediaStore(context, playlistsRepo, mediaPlayerManager)
    }

    private fun getSongsFromMediaStore(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        mediaPlayerManager: MediaPlayerManager
    ) {
        val resources = context.resources
        val newSongs = mutableListOf<Song>()
        val filesThatExist = mutableListOf<Long>()
        val executorFIFO: ExecutorService = Executors.newSingleThreadExecutor()
        executorFIFO.execute {
            _loadingText.postValue(resources.getString(R.string.loading1))
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
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            ).use { cursor ->
                if (cursor != null) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                    val i = cursor.count
                    var j = 0
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val displayName = cursor.getString(nameCol)
                        val title = cursor.getString(titleCol)
                        val artist = cursor.getString(artistCol)
                        val file = File(context.filesDir, id.toString())
                        // TODO put in user triggered scan
                        // !file.exists() || (songDAO.getSong(id) == null)
                        if (!playlistsRepo.isMasterPlaylistInitialized() ||
                            (playlistsRepo.isMasterPlaylistInitialized() &&
                                    !playlistsRepo.getMasterPlaylist().contains(id))
                        ) {
                            AudioUri.saveAudioUri(context, AudioUri(displayName, artist, title, id))
                            newSongs.add(Song(id, title))
                        }
                        filesThatExist.add(id)
                        _loadingProgress.postValue(j.toDouble() / i.toDouble())
                        j++
                    }
                    _loadingText.postValue(resources.getString(R.string.loading2))
                }
            }
        }
        executorFIFO.execute {
            val i = newSongs.size
            _loadingText.postValue(context.resources.getString(R.string.loading2))
            for ((k, song) in newSongs.withIndex()) {
                songDAO.insertAll(song)
                _loadingProgress.postValue(k.toDouble() / i.toDouble())
            }
        }
        executorFIFO.execute {
            if (playlistsRepo.isMasterPlaylistInitialized()) {
                _loadingText.postValue(resources.getString(R.string.loading3))
                addNewSongs(context, playlistsRepo, newSongs)
                _loadingText.postValue(resources.getString(R.string.loading4))
                removeMissingSongs(context, playlistsRepo, mediaPlayerManager, filesThatExist)
            }
        }
        executorFIFO.execute {
            SaveFile.saveFile(context, playlistsRepo)
            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = context.resources.getString(R.string.action_loaded)
            context.sendBroadcast(intent)
        }
    }

    private fun addNewSongs(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        newSongs: List<Song>
    ) {
        val i = newSongs.size
        for ((j, songID) in newSongs.withIndex()) {
            playlistsRepo.getMasterPlaylist().add(context, playlistsRepo, songID)
            _loadingProgress.postValue(j.toDouble() / i.toDouble())
        }
    }

    private fun removeMissingSongs(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        mediaPlayerManager: MediaPlayerManager,
        filesThatExist: List<Long>
    ) {
        val i = filesThatExist.size
        val ids = playlistsRepo.getMasterPlaylist().getSongIDs()
        for ((j, songID) in ids.withIndex()) {
            if (!filesThatExist.contains(songID)) {
                getSong(songID)?.let {
                    playlistsRepo.getMasterPlaylist().remove(context, playlistsRepo, it)
                    songDAO.delete(it)
                }
                mediaPlayerManager.removeMediaPlayerWUri(songID)
            }
            _loadingProgress.postValue(j.toDouble() / i.toDouble())
        }
    }

    fun getSong(songID: Long): Song? {
        var song: Song? = null
        try {
            song = ServiceMain.executorServicePool.submit(
                Callable { songDAO.getSong(songID) }
            ).get()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return song
    }

    companion object {
        const val SONG_DATABASE_NAME: String = "SONG_DATABASE_NAME"
        private val MEDIA_DATA_LOCK: Any = Any()
    }

}