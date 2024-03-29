package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fourthFinger.pinkyPlayer.R
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MediaDatasource private constructor(context: Context, maxPercent: Double) {

    private val _loadingText: MutableLiveData<String> = MutableLiveData()
    val loadingText = _loadingText as LiveData<String>

    private val _loadingProgress: MutableLiveData<Double> = MutableLiveData(0.0)
    val loadingProgress = _loadingProgress as LiveData<Double>

    init {
        // Loading the save file is needed to ensure the master playlist is valid
        // before getting songs from the MediaStore.
        SaveFile.loadSaveFile(context)
        getSongsFromMediaStore(context, maxPercent)
    }

    private fun getSongsFromMediaStore(context: Context, maxPercent: Double) {
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        val resources = context.resources
        val newSongs = mutableListOf<Song>()
        val filesThatExist = mutableListOf<Long>()
        val executorFIFO: ExecutorService = Executors.newSingleThreadExecutor()
        executorFIFO.execute {
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
                    _loadingText.postValue(resources.getString(R.string.loading1))
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
                playlistsRepo.addSongToDB(song)
                _loadingProgress.postValue(k.toDouble() / i.toDouble())
            }
        }
        executorFIFO.execute {
            if (playlistsRepo.isMasterPlaylistInitialized()) {
                _loadingText.postValue(resources.getString(R.string.loading3))
                addNewSongs(context, newSongs)
                _loadingText.postValue(resources.getString(R.string.loading4))
                removeMissingSongs(context, filesThatExist)
            } else {
                playlistsRepo.setMasterPlaylist(
                    RandomPlaylist(
                        context,
                        MASTER_PLAYLIST_NAME,
                        ArrayList(newSongs),
                        true,
                        maxPercent
                    )
                )
            }
        }
        executorFIFO.execute {
            SaveFile.saveFile(context)
            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = context.resources.getString(R.string.action_loaded)
            context.sendBroadcast(intent)
        }
    }

    private fun addNewSongs(context: Context, newSongs: List<Song>) {
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        val i = newSongs.size
        for ((j, songID) in newSongs.withIndex()) {
            playlistsRepo.getMasterPlaylist().add(context, songID)
            _loadingProgress.postValue(j.toDouble() / i.toDouble())
        }
    }

    private fun removeMissingSongs(context: Context, filesThatExist: List<Long>) {
        val mediaPlayerManager = MediaPlayerManager.getInstance(context)
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        val i = filesThatExist.size
        val ids = playlistsRepo.getMasterPlaylist().getSongIDs()
        for ((j, songID) in ids.withIndex()) {
            if (!filesThatExist.contains(songID)) {
                playlistsRepo.getSong(songID)?.let {
                    playlistsRepo.getMasterPlaylist().remove(context, it)
                    playlistsRepo.removeSongFromDB(it)
                }
                mediaPlayerManager.removeMediaPlayerWUri(songID)
            }
            _loadingProgress.postValue(j.toDouble() / i.toDouble())
        }
    }

    companion object {
        const val SONG_DATABASE_NAME: String = "SONG_DATABASE_NAME"
        private val MEDIA_DATA_LOCK: Any = Any()
        private const val MASTER_PLAYLIST_NAME: String = "MASTER_PLAYLIST_NAME"
        private var INSTANCE: MediaDatasource? = null

        fun getInstance(context: Context, maxPercent: Double): MediaDatasource {
            synchronized(MEDIA_DATA_LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = MediaDatasource(context, maxPercent)
                }
                return INSTANCE!!
            }
        }
    }
}