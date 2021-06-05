package com.example.pinky_player.media_controller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class ViewModelFragmentLoading(application: Application) : AndroidViewModel(application) {

    private val mediaData: MediaData = MediaData.getInstance(application.applicationContext)

    fun getLoadingProgress(): LiveData<Double> {
        return mediaData.loadingProgress
    }

    fun getLoadingText(): LiveData<String> {
        return mediaData.loadingText
    }

}