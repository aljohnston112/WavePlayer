package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.room.Room
import com.fourthFinger.pinkyPlayer.media_controller.MediaData
import com.fourthFinger.pinkyPlayer.media_controller.SaveFile
import com.fourthFinger.pinkyPlayer.media_controller.ServiceMain
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import com.fourthFinger.pinkyPlayer.random_playlist.SongDAO
import com.fourthFinger.pinkyPlayer.random_playlist.SongDatabase
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

    private var masterPlaylist: RandomPlaylist? = null
    fun setMasterPlaylist(masterPlaylist: RandomPlaylist) {
        this.masterPlaylist = masterPlaylist
    }
    fun getMasterPlaylist(): RandomPlaylist? {
        return masterPlaylist
    }
    fun getAllSongs(): List<Song>? {
        return masterPlaylist?.getSongs()
    }

    private var songDAO: SongDAO
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

    fun addSongToDB(song: Song) {
        songDAO.insertAll(song)
    }

    fun removeSongFromDB(it: Song) {
        songDAO.delete(it)
    }

    fun addPlaylistFromSaveFile(randomPlaylist: RandomPlaylist) {
        playlists.add(randomPlaylist)
    }

    init {
        val songDatabase = Room.databaseBuilder(context, SongDatabase::class.java,
            MediaData.SONG_DATABASE_NAME
        ).build()
        songDAO = songDatabase.songDAO()
    }

    companion object{

        private var INSTANCE: PlaylistsRepo? = null

        fun getInstance(context: Context): PlaylistsRepo {
            if(INSTANCE == null){
                INSTANCE = PlaylistsRepo(context)
            }
            return INSTANCE!!
        }
    }

}