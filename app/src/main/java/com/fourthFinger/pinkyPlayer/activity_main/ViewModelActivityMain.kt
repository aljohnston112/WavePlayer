package com.fourthFinger.pinkyPlayer.activity_main

import android.view.View.OnClickListener
import android.view.View.VISIBLE
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ViewModelActivityMain : ViewModel() {

    private val _actionBarTitle: MutableLiveData<String> = MutableLiveData()
    val actionBarTitle = _actionBarTitle as LiveData<String>
    fun setActionBarTitle(actionBarTitle: String) {
        this._actionBarTitle.postValue(actionBarTitle)
    }

    private val _showFab: MutableLiveData<Boolean> = MutableLiveData(false)
    val showFab = _showFab as LiveData<Boolean>
    fun showFab(showFAB: Boolean) {
        this._showFab.postValue(showFAB)
    }

    private val _fabText: MutableLiveData<Int> = MutableLiveData()
    val fabText = _fabText as LiveData<Int>
    fun setFABText(@StringRes fabText: Int) {
        this._fabText.postValue(fabText)
    }

    private val _fabImageID: MutableLiveData<Int> = MutableLiveData()
    val fabImageID = _fabImageID as LiveData<Int>
    fun setFabImage(@DrawableRes fabImageID: Int) {
        this._fabImageID.postValue(fabImageID)
    }

    private val _fabOnClickListener: MutableLiveData<OnClickListener> = MutableLiveData()
    val fabOnClickListener =_fabOnClickListener as LiveData<OnClickListener>
    fun setFabOnClickListener(fabOnClickListener: OnClickListener?) {
        this._fabOnClickListener.postValue(fabOnClickListener)
    }

    private val _songPaneVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val songPaneVisible = _songPaneVisible as LiveData<Boolean>
    fun songPaneVisible(visibility: Int) {
        this._songPaneVisible.postValue(visibility == VISIBLE)
    }

    private val _fragmentSongVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val fragmentSongVisible = _fragmentSongVisible as LiveData<Boolean>
    fun fragmentSongVisible(visible: Boolean) {
        this._fragmentSongVisible.postValue(visible)
    }

}