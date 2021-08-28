package com.fourthFinger.pinkyPlayer.activity_main

import android.app.Application
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fourthFinger.pinkyPlayer.fragments.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song

class ViewModelActivityMain(application: Application) : AndroidViewModel(application) {

    private val playlistsRepo = PlaylistsRepo.getInstance(application)

    private val _actionBarTitle: MutableLiveData<String> = MutableLiveData()
    val actionBarTitle = _actionBarTitle as LiveData<String>
    fun setActionBarTitle(actionBarTitle: String) {
        this._actionBarTitle.value = (actionBarTitle)
    }

    private val _showFab: MutableLiveData<Boolean> = MutableLiveData(false)
    val showFab = _showFab as LiveData<Boolean>
    fun showFab(showFAB: Boolean) {
        this._showFab.value = (showFAB)
    }

    private val _fabText: MutableLiveData<Int> = MutableLiveData()
    val fabText = _fabText as LiveData<Int>
    fun setFABText(@StringRes fabText: Int) {
        this._fabText.value = (fabText)
    }

    private val _fabImageID: MutableLiveData<Int> = MutableLiveData()
    val fabImageID = _fabImageID as LiveData<Int>
    fun setFabImage(@DrawableRes fabImageID: Int) {
        this._fabImageID.value = (fabImageID)
    }

    private val _fabOnClickListener: MutableLiveData<View.OnClickListener> = MutableLiveData()
    val fabOnClickListener =_fabOnClickListener as LiveData<View.OnClickListener>
    fun setFabOnClickListener(fabOnClickListener: View.OnClickListener?) {
        this._fabOnClickListener.value = (fabOnClickListener)
    }

    private val _playlistToAddToQueue = MutableLiveData<RandomPlaylist?>()
    val playlistToAddToQueue = _playlistToAddToQueue as LiveData<RandomPlaylist?>
    fun setPlaylistToAddToQueue(playlistToAddToQueue: RandomPlaylist?) {
        _playlistToAddToQueue.value = playlistToAddToQueue
    }

    private val _songToAddToQueue = MutableLiveData<Long?>()
    val songToAddToQueue = _songToAddToQueue as LiveData<Long?>
    fun setSongToAddToQueue(songToAddToQueue: Long?) {
        _songToAddToQueue.value = songToAddToQueue
    }

    fun getSongToAddToQueue(): Song? {
        return songToAddToQueue.value?.let { playlistsRepo.getSong(it) }
    }

}