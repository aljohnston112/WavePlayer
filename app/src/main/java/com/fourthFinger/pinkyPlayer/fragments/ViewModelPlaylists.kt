package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.fourthFinger.pinkyPlayer.random_playlist.*
import java.util.*

class ViewModelPlaylists(application: Application) : AndroidViewModel(application) {

    private val playlistsRepo = PlaylistsRepo.getInstance(application)

    fun getPlaylists(): List<RandomPlaylist> {
        return playlistsRepo.getPlaylists()
    }

    fun getAllSongs(): Set<Song> {
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

    private var playlistForUndo: RandomPlaylist? = null

    fun notifyPlaylistRemoved(context: Context, position: Int) {
        playlistForUndo = getPlaylists()[position]
        playlistForUndo?.let { playlistsRepo.removePlaylist(context, it) }
    }

    fun notifyPlaylistInserted(context: Context, position: Int) {
        playlistForUndo?.let { playlistsRepo.addPlaylist(context, position, it) }
        playlistForUndo = null
    }

    fun siftPlaylists(newText: String): List<RandomPlaylist> {
        val sifted: MutableList<RandomPlaylist> = mutableListOf()
        for (randomPlaylist in getPlaylists()) {
            if (randomPlaylist.getName().lowercase(Locale.ROOT)
                    .contains(newText.lowercase(Locale.ROOT))
            ) {
                sifted.add(randomPlaylist)
            }
        }
        return sifted
    }

    fun siftAllSongs(newText: String): List<Song> {
        val sifted: MutableList<Song> = mutableListOf()
        for (song in getAllSongs()) {
            if (song.title.lowercase(Locale.ROOT)
                    .contains(newText.lowercase(Locale.ROOT))
            ) {
                sifted.add(song)
            }
        }
        return sifted
    }

    fun fragmentSongsViewCreated(context: Context) {
        MediaSession.getInstance(context).setCurrentPlaylistToMaster()
    }

}