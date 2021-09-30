package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.random_playlist.*

class ViewModelFragmentQueue: ViewModel() {

    fun songClicked(context: Context, fragment: Fragment, pos: Int) {
        val mediaPlayerSession = MediaPlayerSession.getInstance(context)
        val mediaSession: MediaSession = MediaSession.getInstance(context)
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        val songQueue = SongQueue.getInstance()
        val song: Song = songQueue.songClicked(pos)
        synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
            // TODO pass this in?
            if (song == mediaPlayerSession.currentAudioUri.value?.id?.let {
                    playlistsRepo.getSong(it)
                }
            ) {
                mediaSession.seekTo(context, 0)
            }
            mediaSession.playNext(context)
        }
        NavUtil.navigateTo(fragment, R.id.fragmentSong)
    }
}