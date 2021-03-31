package com.example.waveplayer

import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import com.example.waveplayer.random_playlist.RandomPlaylist

class ViewModelUserPickedPlaylist constructor() : ViewModel() {
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