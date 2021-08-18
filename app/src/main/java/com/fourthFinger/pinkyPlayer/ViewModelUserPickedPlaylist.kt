package com.fourthFinger.pinkyPlayer

import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist

class ViewModelUserPickedPlaylist : ViewModel() {

    @GuardedBy("this")
    @Volatile
    private var userPickedPlaylist: RandomPlaylist? = null
    @Synchronized
    fun getUserPickedPlaylist(): RandomPlaylist? {
        return userPickedPlaylist
    }

    @Synchronized
    fun setUserPickedPlaylist(userPickedPlaylist: RandomPlaylist?) {
        this.userPickedPlaylist = userPickedPlaylist
    }

}