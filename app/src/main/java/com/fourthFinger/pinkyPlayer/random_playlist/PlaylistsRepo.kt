package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import androidx.room.Room
import com.fourthFinger.pinkyPlayer.ServiceMain
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

class PlaylistsRepo private constructor(context: Context) {

    private val playlists: MutableList<RandomPlaylist> = mutableListOf()
    fun getPlaylists(): List<RandomPlaylist> {
        return playlists
    }

    fun addPlaylist(context: Context, randomPlaylist: RandomPlaylist) {
        playlists.add(randomPlaylist)
        SaveFile.saveFile(context)
    }

    fun addPlaylist(context: Context, position: Int, randomPlaylist: RandomPlaylist) {
        playlists.add(position, randomPlaylist)
        SaveFile.saveFile(context)
    }

    fun removePlaylist(context: Context, randomPlaylist: RandomPlaylist) {
        playlists.remove(randomPlaylist)
        SaveFile.saveFile(context)
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

    private lateinit var masterPlaylist: RandomPlaylist
    fun setMasterPlaylist(masterPlaylist: RandomPlaylist) {
        this.masterPlaylist = masterPlaylist
    }
    fun getMasterPlaylist(): RandomPlaylist {
        return masterPlaylist
    }
    fun getAllSongs(): Set<Song> {
        return masterPlaylist.getSongs()
    }
    fun isMasterPlaylistInitialized(): Boolean {
        return ::masterPlaylist.isInitialized
    }

    private var songDAO: SongDAO
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

    fun addSongToDB(song: Song) {
        songDAO.insertAll(song)
    }

    fun removeSongFromDB(it: Song) {
        songDAO.delete(it)
    }

    fun addPlaylistsFromSaveFile(randomPlaylist: List<RandomPlaylist>) {
        for(p in randomPlaylist) {
            if(!playlists.contains(p)) {
                playlists.add(p)
            }
        }
    }

    fun playlistExists(playlistName: String): Boolean {
        var playlistIndex = -1
        for ((i, randomPlaylist) in getPlaylists().withIndex()) {
            if (randomPlaylist.getName() == playlistName) {
                playlistIndex = i
            }
        }
        return playlistIndex != -1
    }

    fun getPlaylistTitles(): Array<String?> {
        val randomPlaylists = getPlaylists()
        val titles: MutableList<String> = ArrayList(randomPlaylists.size)
        for (randomPlaylist in randomPlaylists) {
            titles.add(randomPlaylist.getName())
        }
        val titlesArray = arrayOfNulls<String>(titles.size)
        var i = 0
        for (title in titles) {
            titlesArray[i++] = title
        }
        return titlesArray
    }

    init {
        val songDatabase = Room.databaseBuilder(
            context, SongDatabase::class.java,
            MediaLoader.SONG_DATABASE_NAME
        ).build()
        songDAO = songDatabase.songDAO()
    }

    companion object {

        private var INSTANCE: PlaylistsRepo? = null

        fun getInstance(context: Context): PlaylistsRepo {
            if (INSTANCE == null) {
                INSTANCE = PlaylistsRepo(context)
            }
            return INSTANCE!!
        }
    }

}