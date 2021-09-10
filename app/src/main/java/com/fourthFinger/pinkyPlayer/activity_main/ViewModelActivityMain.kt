package com.fourthFinger.pinkyPlayer.activity_main

import android.app.Application
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fourthFinger.pinkyPlayer.fragments.PlaylistsRepo

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

    private val _fabOnClickListener: MutableLiveData<OnClickListener> = MutableLiveData()
    val fabOnClickListener =_fabOnClickListener as LiveData<OnClickListener>
    fun setFabOnClickListener(fabOnClickListener: OnClickListener?) {
        this._fabOnClickListener.value = (fabOnClickListener)
    }

    private val _songPaneVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val songPaneVisible = _songPaneVisible as LiveData<Boolean>
    fun songPaneVisible(visibility: Int) {
        this._songPaneVisible.value = visibility == VISIBLE
    }

}