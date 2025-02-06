package io.fourth_finger.playlist_data_source

class PlaylistList(
    val masterPlaylist: RandomPlaylist,
    val playlists: List<RandomPlaylist>
) {

    fun withAppendedPlaylist(randomPlaylist: RandomPlaylist): PlaylistList {
        val newPlaylists = playlists.toMutableList()
        newPlaylists.add(randomPlaylist)
        return PlaylistList(
            masterPlaylist,
            newPlaylists
        )
    }

    fun withAppendedPlaylist(
        position: Int,
        randomPlaylist: RandomPlaylist,
    ): PlaylistList {
        val newPlaylists = playlists.toMutableList()
        newPlaylists.add(position, randomPlaylist)
        return PlaylistList(
            masterPlaylist,
            newPlaylists
        )
    }

    fun withRemovedPlaylist(randomPlaylist: RandomPlaylist): PlaylistList {
        val newPlaylists = playlists.toMutableList()
        newPlaylists.remove(randomPlaylist)
        return PlaylistList(
            masterPlaylist,
            newPlaylists
        )
    }

    fun withMaxPercent(maxPercent: Double): PlaylistList {
        val newMasterPlaylist = masterPlaylist
        newMasterPlaylist.setMaxPercent(maxPercent)

        val newPlaylists = playlists
        for(playlist in newPlaylists){
            playlist.setMaxPercent(maxPercent)
        }

        return PlaylistList(
            newMasterPlaylist,
            newPlaylists
        )
    }

}