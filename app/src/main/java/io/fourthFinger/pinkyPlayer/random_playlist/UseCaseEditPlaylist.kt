package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import androidx.fragment.app.Fragment
import io.fourthFinger.pinkyPlayer.NavUtil
import io.fourthFinger.playlistDataSource.PlaylistsRepo
import io.fourthFinger.playlistDataSource.Song

class UseCaseEditPlaylist(
    val playlistsRepo: PlaylistsRepo,
    val mediaSession: MediaSession
) {

    private var songForUndo: Song? = null
    private var probabilityForUndo: Double? = null

    fun notifySongMoved(
        context: Context,
        fromPosition: Int,
        toPosition: Int
    ) {
        // TODO make sure changes are persistent across app restarts
        mediaSession.currentlyPlayingPlaylist.value?.let {
            playlistsRepo.swapSongPositions(
                context,
                it,
                fromPosition,
                toPosition
            )
        }
    }

    fun notifySongRemoved(fragment: Fragment, position: Int) {
        val currentPlaylist = mediaSession.currentlyPlayingPlaylist.value
        if (currentPlaylist != null) {
            val context = fragment.requireActivity().applicationContext
            songForUndo = currentPlaylist.getSongs().toList()[position]
            probabilityForUndo = songForUndo?.let { currentPlaylist.getProbability(it) }
            if (currentPlaylist.size() == 1) {
                currentPlaylist.let {
                    playlistsRepo.removePlaylist(context, it)
                    // TODO when user is listening to a playlist and then removes it, it keeps playing
                    mediaSession.setCurrentPlaylist(null)
                    NavUtil.popBackStack(fragment)
                }
            } else {
                // Remove the song; this is NOT the undo
                songForUndo?.let {
                    playlistsRepo.removeSong(
                        context,
                        currentPlaylist,
                        it
                    )
                }
            }
        }
    }

    fun notifyItemInserted(context: Context, position: Int) {
        val currentPlaylist = mediaSession.currentlyPlayingPlaylist.value
        if (currentPlaylist != null) {
            songForUndo?.let { song ->
                probabilityForUndo?.let { probability ->
                    playlistsRepo.addSong(
                        context,
                        currentPlaylist,
                        song,
                        probability
                    )
                }
            }
            playlistsRepo.switchSongPositions(
                context,
                currentPlaylist,
                currentPlaylist.size() - 1,
                position
            )
            if (currentPlaylist.size() == 1) {
                playlistsRepo.addPlaylist(context, currentPlaylist)
            }
        }
        songForUndo = null
        probabilityForUndo = null
    }

}
