package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.media_controller.MediaSession

class ViewModelFragmentPaneSong: ViewModel() {

    fun clicked(context: Context, fragment: Fragment, @IdRes id: Int) {
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