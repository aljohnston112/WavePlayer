package com.example.waveplayer2.activity_main

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.waveplayer2.random_playlist.RandomPlaylist

class ViewModelActivityMain : ViewModel() {
    private val _showFab: MutableLiveData<Boolean> = MutableLiveData(false)
    val showFab = _showFab as LiveData<Boolean>
    fun showFab(showFAB: Boolean) {
        this._showFab.value = (showFAB)
    }
    
    private val _actionBarTitle: MutableLiveData<String> = MutableLiveData()
    val actionBarTitle = _actionBarTitle as LiveData<String>
    fun setActionBarTitle(actionBarTitle: String) {
        this._actionBarTitle.value = (actionBarTitle)
    }

    private val _fabText: MutableLiveData<Int> = MutableLiveData()
    val fabText = _fabText as LiveData<Int>
    fun setFABText(fabText: Int) {
        this._fabText.value = (fabText)
    }

    private val _fabImage: MutableLiveData<Int> = MutableLiveData()
    val fabImage = _fabImage as LiveData<Int>
    fun setFabImage(fabImage: Int) {
        this._fabImage.value = (fabImage)
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
    
}