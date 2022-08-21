package com.fourthFinger.pinkyPlayer.fragments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.fourthFinger.pinkyPlayer.random_playlist.MediaDatasource
import com.fourthFinger.pinkyPlayer.settings.SettingsRepo
import kotlin.math.roundToInt

class ViewModelFragmentLoading(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepo.getInstance(application)

    private lateinit var mediaLoader: MediaDatasource

    val loadingText: LiveData<String> = mediaLoader.loadingText

    val loadingProgress: LiveData<Int> = Transformations.map(
        mediaLoader.loadingProgress
    ) { percent ->
        (percent * 100.0).roundToInt()
    }

    fun permissionGranted() {
        mediaLoader = MediaDatasource.getInstance(
            getApplication(),
            settingsRepo.settings.maxPercent
        )
    }

}