package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.fourthFinger.pinkyPlayer.media_controller.SaveFile
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import java.util.*

class ViewModelPlaylists(application: Application) : AndroidViewModel(application) {

    private val playlistsRepo = PlaylistsRepo.getInstance(application)

    fun getPlaylists(): List<RandomPlaylist> {
        return playlistsRepo.getPlaylists()
    }

    fun getAllSongs(): List<Song>? {
        return playlistsRepo.getAllSongs()
    }

    fun notifyPlaylistMoved(context: Context, fromPosition: Int, toPosition: Int) {
        // TODO make sure changes are persistent across app restarts
        Collections.swap(
            getPlaylists(),
            fromPosition,
            toPosition
        )
        SaveFile.saveFile(context)
    }

    private var playlist: RandomPlaylist? = null

    fun notifyPlaylistRemoved(context: Context, position: Int) {
        playlist = getPlaylists()[position]
        playlist?.let { playlistsRepo.removePlaylist(context, it) }
    }

    fun notifyPlaylistInserted(context: Context, position: Int) {
        playlist?.let { playlistsRepo.addPlaylist(context, position, it) }
        playlist = null
    }

    fun filterPlaylists(newText: String): List<RandomPlaylist> {
        val sifted: MutableList<RandomPlaylist> = ArrayList<RandomPlaylist>()
        for (randomPlaylist in getPlaylists()) {
            if (randomPlaylist.getName().lowercase(Locale.ROOT)
                    .contains(newText.lowercase(Locale.ROOT))
            ) {
                sifted.add(randomPlaylist)
            }
        }
        return sifted
    }

    fun filterAllSongs(newText: String): List<Song> {
        val sifted: MutableList<Song> = ArrayList<Song>()
        for (song in getAllSongs() ?: listOf()) {
            if (song.title.lowercase(Locale.ROOT)
                    .contains(newText.lowercase(Locale.ROOT))
            ) {
                sifted.add(song)
            }
        }
        return sifted
    }

}