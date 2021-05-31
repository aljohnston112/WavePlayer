package com.example.waveplayer2.media_controller

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.example.waveplayer2.R
import com.example.waveplayer2.random_playlist.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService

class MediaData(context: Context) {

    private val _loadingText: MutableLiveData<String> = MutableLiveData()
    val loadingText = _loadingText as LiveData<String>

    private val _loadingProgress: MutableLiveData<Double> = MutableLiveData(0.0)
    val loadingProgress = _loadingProgress as LiveData<Double>

    private val songIDToMediaPlayerWUriHashMap: HashMap<Long, MediaPlayerWUri> = HashMap()
    private val playlists: MutableList<RandomPlaylist> = mutableListOf()
    private var songDAO: SongDAO
    private var masterPlaylist: RandomPlaylist? = null
    private var settings = Settings(0.1, 0.1, 0.5, 0.0)


    fun getMediaPlayerWUri(songID: Long): MediaPlayerWUri? {
        return songIDToMediaPlayerWUriHashMap[songID]
    }

    fun addMediaPlayerWUri(mediaPlayerWURI: MediaPlayerWUri) {
        songIDToMediaPlayerWUriHashMap[mediaPlayerWURI.id] = mediaPlayerWURI
    }

    fun releaseMediaPlayers() {
        // TODO
        synchronized(this) {
            for (mediaPlayerWURI in songIDToMediaPlayerWUriHashMap.values) {
                mediaPlayerWURI.release()
            }
            songIDToMediaPlayerWUriHashMap.clear()
        }
    }

    fun getPlaylists(): List<RandomPlaylist> {
        return playlists
    }

    fun addPlaylist(randomPlaylist: RandomPlaylist) {
        playlists.add(randomPlaylist)
    }

    fun addPlaylist(position: Int, randomPlaylist: RandomPlaylist) {
        playlists.add(position, randomPlaylist)
    }

    fun removePlaylist(randomPlaylist: RandomPlaylist) {
        playlists.remove(randomPlaylist)
    }

    fun getPlaylist(playlistName: String): RandomPlaylist? {
        var out: RandomPlaylist? = null
        for (randomPlaylist in playlists) {
            if (randomPlaylist.getName() == playlistName) {
                out = randomPlaylist
            }
            break
        }
        return out
    }

    fun getAllSongs(): List<Song>? {
        return masterPlaylist?.getSongs()
    }

    fun setMasterPlaylist(masterPlaylist: RandomPlaylist) {
        this.masterPlaylist = masterPlaylist
    }

    fun getMasterPlaylist(): RandomPlaylist? {
        return masterPlaylist
    }

    fun getSettings(): Settings {
        return settings
    }

    fun setSettings(settings: Settings) {
        this.settings = settings
    }

    fun getMaxPercent(): Double {
        return settings.maxPercent
    }

    fun setMaxPercent(maxPercent: Double) {
        masterPlaylist?.setMaxPercent(maxPercent)
        for (randomPlaylist in playlists) {
            randomPlaylist.setMaxPercent(maxPercent)
        }
        settings = Settings(
                maxPercent, settings.percentChangeUp, settings.percentChangeDown, settings.lowerProb)
    }

    fun setPercentChangeUp(percentChangeUp: Double) {
        settings = Settings(
                settings.maxPercent, percentChangeUp, settings.percentChangeDown, settings.lowerProb)
    }

    fun getPercentChangeUp(): Double {
        return settings.percentChangeUp
    }

    fun setPercentChangeDown(percentChangeDown: Double) {
        settings = Settings(
                settings.maxPercent, settings.percentChangeUp, percentChangeDown, settings.lowerProb)
    }

    fun getPercentChangeDown(): Double {
        return settings.percentChangeDown
    }

    fun setLowerProb(lowerProb: Double) {
        settings = Settings(
                settings.maxPercent, settings.percentChangeUp, settings.percentChangeDown, lowerProb)
    }

    fun getLowerProb(): Double {
        return settings.lowerProb
    }

    fun loadData(context: Context) {
         SaveFile.loadSaveFile(context, this)
         getSongsFromMediaStore(context)
    }

    private fun getSongsFromMediaStore(context: Context) {
        val resources = context.resources
        val newSongs = mutableListOf<Song>()
        val filesThatExist = mutableListOf<Long>()
        val executorServiceFIFO: ExecutorService = ServiceMain.executorServiceFIFO
        executorServiceFIFO.execute {
            val projection = arrayOf(
                    MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.ARTIST_ID,
                    MediaStore.Audio.Media.TITLE)
            val selection = MediaStore.Audio.Media.IS_MUSIC + " != ?"
            val selectionArgs = arrayOf("0")
            val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
            context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, selectionArgs, sortOrder).use { cursor ->
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
                        if ((masterPlaylist?.contains(Song(id, title)) != true)) {
                            val audioURI = AudioUri(displayName, artist, title, id)
                            try {
                                context.openFileOutput(id.toString(), Context.MODE_PRIVATE).use { fos ->
                                    ObjectOutputStream(fos).use { objectOutputStream ->
                                        objectOutputStream.writeObject(audioURI)
                                    }
                                }
                            } catch (e: FileNotFoundException) {
                                e.printStackTrace()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
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
        executorServiceFIFO.execute {
            val i = newSongs.size
            _loadingText.postValue(context.getResources().getString(R.string.loading2))
            for ((k, song) in newSongs.withIndex()) {
                songDAO.insertAll(song)
                _loadingProgress.postValue(k.toDouble() / i.toDouble())
            }
        }
        executorServiceFIFO.execute {
            if (masterPlaylist != null) {
                _loadingText.postValue(resources.getString(R.string.loading3))
                addNewSongs(filesThatExist)
                _loadingText.postValue(resources.getString(R.string.loading4))
                removeMissingSongs(filesThatExist)
            } else {
                masterPlaylist = RandomPlaylist(
                        MASTER_PLAYLIST_NAME,
                        ArrayList(newSongs),
                        settings.maxPercent,
                        true
                )
            }
        }
        executorServiceFIFO.execute {
            if (settings.lowerProb == 0.0) {
                setLowerProb(2.0 / (masterPlaylist?.size()?:2).toDouble())
            }
            SaveFile.saveFile(context)
            val intent = Intent()
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.action = context.resources.getString(R.string.broadcast_receiver_action_loaded)
            context.sendBroadcast(intent)
        }
    }

    private fun removeMissingSongs(filesThatExist: List<Long>) {
        val i = filesThatExist.size
        val ids = masterPlaylist?.getSongIDs()
        if(ids != null) {
            for((j, songID) in ids.withIndex()) {
                if (!filesThatExist.contains(songID)) {
                    songDAO.getSong(songID)?.let {
                        masterPlaylist?.remove(it)
                        songDAO.delete(it) }
                    songIDToMediaPlayerWUriHashMap.remove(songID)
                }
                _loadingProgress.postValue(j.toDouble() / i.toDouble())
            }
        }
    }

    private fun addNewSongs(filesThatExist: List<Long>) {
        val i = filesThatExist.size
        for ((j, songID) in filesThatExist.withIndex()) {
            if (masterPlaylist?.contains(songID) == false) {
                songDAO.getSong(songID)?.let { masterPlaylist?.add(it) }
            }
            _loadingProgress.postValue(j.toDouble() / i.toDouble())
        }
    }

    fun getSong(songID: Long): Song? {
        var song: Song? = null
        try {
            song = ServiceMain.executorServicePool.submit(Callable { songDAO.getSong(songID) }).get()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return song
    }

    init {
        val songDatabase = Room.databaseBuilder(context, SongDatabase::class.java, SONG_DATABASE_NAME).build()
        songDAO = songDatabase.songDAO()
    }

    companion object {
        const val SONG_DATABASE_NAME: String = "SONG_DATABASE_NAME"
        private val MEDIA_DATA_LOCK: Any = Any()
        private const val MASTER_PLAYLIST_NAME: String = "MASTER_PLAYLIST_NAME"
        private var INSTANCE: MediaData? = null

        /** Returns a singleton instance of this class.
         * loadData(Context context) must be called for methods on the singleton to function properly.
         * @return A singleton instance of this class that may or may not be loaded with data.
         */
        fun getInstance(context: Context): MediaData {
            synchronized(MEDIA_DATA_LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = MediaData(context)
                }
                return INSTANCE!!
            }
        }
    }
}