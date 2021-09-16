package com.fourthFinger.pinkyPlayer.media_controller

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import java.security.AccessControlContext
import kotlin.math.roundToInt

class ViewModelFragmentLoading(application: Application) : AndroidViewModel(application) {

    private val mediaData: MediaData = MediaData.getInstance()

    val loadingProgress: LiveData<Int> = Transformations.map(mediaData.loadingProgress){ percent ->
        return@map (percent * 100.0).roundToInt()
    }

    val loadingText: LiveData<String> = mediaData.loadingText

    fun permissionGranted(){
        mediaData.loadData(getApplication())
    }

}