package io.fourth_finger.playlist_data_source

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PlaylistsRepo {

    private val playlistDataSource = PlaylistDataSource()

    private val _playlistList = MutableLiveData<PlaylistList>()
    val playlists = _playlistList as LiveData<PlaylistList>

    fun loadPlaylists(
        context: Context,
        maxPercent: Double
    ) {
        _playlistList.postValue(
            playlistDataSource.loadPlaylists(
                context,
                maxPercent
            )
        )
    }

    fun getPlaylist(playlistName: String): RandomPlaylist? {
        var out: RandomPlaylist? = null
        for (randomPlaylist in playlists.value!!.playlists) {
            if (randomPlaylist.name == playlistName) {
                out = randomPlaylist
            }
            break
        }
        return out
    }

    fun getMasterPlaylist(): RandomPlaylist {
        return playlists.value!!.masterPlaylist
    }

    fun playlistExists(playlistName: String): Boolean {
        var playlistIndex = -1
        for ((i, randomPlaylist) in playlists.value!!.playlists.withIndex()) {
            if (randomPlaylist.name == playlistName) {
                playlistIndex = i
            }
        }
        return playlistIndex != -1
    }

    fun getPlaylistTitles(): Array<String> {
        val randomPlaylists = playlists.value!!.playlists
        val titles: MutableList<String> = ArrayList(randomPlaylists.size)
        for (randomPlaylist in randomPlaylists) {
            titles.add(randomPlaylist.name)
        }
        val titlesArray = Array(titles.size) { "" }
        var i = 0
        for (title in titles) {
            titlesArray[i++] = title
        }
        return titlesArray
    }

    fun addPlaylist(
        context: Context,
        randomPlaylist: RandomPlaylist
    ) {
        updatePlaylistList(context) {
            it.withAppendedPlaylist(randomPlaylist)
        }
    }

    fun addPlaylist(
        context: Context,
        position: Int,
        randomPlaylist: RandomPlaylist
    ) {
        updatePlaylistList(context) {
            it.withAppendedPlaylist(
                position,
                randomPlaylist
            )
        }
    }

    fun removePlaylist(
        context: Context,
        randomPlaylist: RandomPlaylist
    ) {
        updatePlaylistList(context) {
            it.withRemovedPlaylist(randomPlaylist)
        }
    }

    fun setMaxPercent(
        context: Context,
        maxPercent: Double
    ) {
        updatePlaylistList(context) {
            it.withMaxPercent(maxPercent)
        }
    }

    fun setName(
        context: Context,
        randomPlaylist: RandomPlaylist,
        name: String
    ) {
        updatePlaylistList(context) {
            randomPlaylist.name = name
            it
        }
    }

    fun resetProbabilities(
        context: Context,
        randomPlaylist: RandomPlaylist
    ) {
        updatePlaylistList(context) {
            randomPlaylist.resetProbabilities()
            it
        }
    }

    fun lowerProbabilities(
        context: Context,
        randomPlaylist: RandomPlaylist,
        probabilityFloor: Double
    ) {
        updatePlaylistList(context) {
            randomPlaylist.lowerProbabilities(probabilityFloor)
            it
        }
    }

    fun swapSongPositions(
        context: Context,
        randomPlaylist: RandomPlaylist,
        oldPosition: Int,
        newPosition: Int
    ) {
        updatePlaylistList(context) {
            randomPlaylist.swapSongPositions(
                oldPosition,
                newPosition
            )
            it
        }
    }

    fun switchSongPositions(
        context: Context,
        randomPlaylist: RandomPlaylist,
        oldPosition: Int,
        newPosition: Int
    ) {
        updatePlaylistList(context) {
            randomPlaylist.switchSongPositions(
                oldPosition,
                newPosition
            )
            it
        }
    }

    fun bad(
        context: Context,
        randomPlaylist: RandomPlaylist,
        song: Song,
        percentChangeDown: Double
    ) {
        updatePlaylistList(context) {
            randomPlaylist.bad(
                song,
                percentChangeDown
            )
            it
        }
    }

    fun good(
        context: Context,
        randomPlaylist: RandomPlaylist,
        song: Song,
        percentChangeUp: Double
    ) {
        updatePlaylistList(context) {
            randomPlaylist.good(
                song,
                percentChangeUp
            )
            it
        }
    }

    fun removeSong(
        context: Context,
        playlist: RandomPlaylist,
        song: Song
    ) {
        updatePlaylistList(context) {
            playlist.remove(song)
            it
        }
    }

    fun addSong(
        context: Context,
        playlist: RandomPlaylist,
        song: Song
    ) {
        updatePlaylistList(context) {
            playlist.add(song)
            it
        }
    }

    fun addSong(
        context: Context,
        playlist: RandomPlaylist,
        song: Song,
        probability: Double
    ) {
        updatePlaylistList(context) {
            playlist.add(
                song,
                probability
            )
            it
        }
    }

    fun addToMasterPlaylist(
        context: Context,
        song: Song
    ) {
        updatePlaylistList(
            context
        ) {
            it.masterPlaylist.add(song)
            it
        }
    }

    fun removeFromMasterPlaylist(
        context: Context,
        song: Song
    ) {
        updatePlaylistList(
            context
        ) {
            it.masterPlaylist.remove(song)
            it
        }
    }


    private fun updatePlaylistList(
        context: Context,
        updateFunction: (PlaylistList) -> PlaylistList
    ) {
        playlists.value?.let { playlistList ->
            val newPlaylistList = updateFunction(playlistList)
            playlistDataSource.savePlaylists(
                context,
                newPlaylistList
            )
            _playlistList.postValue(
                newPlaylistList
            )
        }
    }

    fun cleanup() {
        playlistDataSource.cleanUp()
    }

}