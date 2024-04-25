package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import androidx.room.Room
import io.fourthFinger.pinkyPlayer.ServiceMain
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

class PlaylistsRepo {

    private val playlists: MutableList<RandomPlaylist> = mutableListOf()

    fun getPlaylists(): List<RandomPlaylist> {
        return playlists
    }

    fun addPlaylist(context: Context, randomPlaylist: RandomPlaylist) {
        playlists.add(randomPlaylist)
        SaveFile.saveFile(context,  this)
    }

    fun addPlaylist(context: Context, position: Int, randomPlaylist: RandomPlaylist) {
        playlists.add(position, randomPlaylist)
        SaveFile.saveFile(context, this)
    }

    fun removePlaylist(context: Context, randomPlaylist: RandomPlaylist) {
        playlists.remove(randomPlaylist)
        SaveFile.saveFile(context, this)
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
    fun isMasterPlaylistInitialized(): Boolean {
        return ::masterPlaylist.isInitialized
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

    fun getPlaylistTitles(): Array<String> {
        val randomPlaylists = getPlaylists()
        val titles: MutableList<String> = ArrayList(randomPlaylists.size)
        for (randomPlaylist in randomPlaylists) {
            titles.add(randomPlaylist.getName())
        }
        val titlesArray = Array(titles.size) { "" }
        var i = 0
        for (title in titles) {
            titlesArray[i++] = title
        }
        return titlesArray
    }

    fun setMaxPercent(maxPercent: Double) {
        masterPlaylist.setMaxPercent(maxPercent)
        for (playlist in playlists){
            playlist.setMaxPercent(maxPercent)
        }
    }

}