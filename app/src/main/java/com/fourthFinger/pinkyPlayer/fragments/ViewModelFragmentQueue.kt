package com.fourthFinger.pinkyPlayer.fragments

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.random_playlist.*

class ViewModelFragmentQueue: ViewModel() {

    fun songClicked(fragment: Fragment, queuePosition: Int) {
        val context = fragment.requireActivity().applicationContext
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        val mediaSession: MediaSession = MediaSession.getInstance(context)
        val mediaPlayerManager = MediaPlayerManager.getInstance(context)
        val songQueue = SongQueue.getInstance()
        val song: Song = songQueue.setIndex(queuePosition)
        synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
            if (song == mediaPlayerManager.currentAudioUri.value?.id?.let {
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