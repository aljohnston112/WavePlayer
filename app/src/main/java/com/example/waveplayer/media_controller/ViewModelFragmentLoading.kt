package com.example.waveplayer.media_controller

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class ViewModelFragmentLoading constructor() : ViewModel() {
    private val mediaData: MediaData? = MediaData.Companion.getInstance()
    fun getLoadingProgress(): LiveData<Double?>? {
        return mediaData.getLoadingProgress()
    }

    fun getLoadingText(): LiveData<String?>? {
        return mediaData.getLoadingText()
    }
}