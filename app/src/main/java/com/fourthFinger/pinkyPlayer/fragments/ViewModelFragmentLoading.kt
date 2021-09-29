package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.fourthFinger.pinkyPlayer.random_playlist.MediaLoader
import kotlin.math.roundToInt

class ViewModelFragmentLoading(application: Application) : AndroidViewModel(application) {

    private val mediaLoader: MediaLoader = MediaLoader.getInstance()

    val loadingProgress: LiveData<Int> = Transformations.map(mediaLoader.loadingProgress){ percent ->
        return@map (percent * 100.0).roundToInt()
    }

    val loadingText: LiveData<String> = mediaLoader.loadingText

    fun permissionGranted(){
        mediaLoader.loadData(getApplication())
    }

}