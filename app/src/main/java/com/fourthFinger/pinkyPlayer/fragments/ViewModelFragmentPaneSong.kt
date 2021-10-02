package com.fourthFinger.pinkyPlayer.fragments

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession

class ViewModelFragmentPaneSong: ViewModel() {

    fun clicked(fragment: Fragment, @IdRes id: Int) {
        val context = fragment.requireActivity().applicationContext
        val mediaSession = MediaSession.getInstance(context)
        if (id == R.id.imageButtonSongPaneNext) {
            mediaSession.playNext(context)
        } else if (id == R.id.imageButtonSongPanePlayPause) {
            mediaSession.pauseOrPlay(context)
        } else if (id == R.id.imageButtonSongPanePrev) {
            mediaSession.playPrevious(context)
        } else if (id == R.id.textViewSongPaneSongName ||
            id == R.id.imageViewSongPaneSongArt
        ) {
            NavUtil.navigateTo(fragment, R.id.fragmentSong)
        }
    }

}