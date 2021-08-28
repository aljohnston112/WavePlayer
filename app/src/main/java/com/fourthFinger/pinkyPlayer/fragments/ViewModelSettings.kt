package com.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.ViewModel
import com.fourthFinger.pinkyPlayer.random_playlist.Settings
import com.fourthFinger.pinkyPlayer.random_playlist.SettingsRepo
import kotlin.math.roundToInt

class ViewModelSettings: ViewModel() {

    private val settingsRepo = SettingsRepo.getInstance()

    fun getNSongs(): String {
        return (1.0/settingsRepo.getMaxPercent()).roundToInt().toString()
    }

    fun getPercentChangeUp(): String {
        return (100.0/settingsRepo.getPercentChangeUp()).roundToInt().toString()
    }

    fun getPercentChangeDown(): String {
        return (100.0/settingsRepo.getPercentChangeDown()).roundToInt().toString()
    }

    fun setSettings(
        nSongs: Int,
        percentChangeUp: Int,
        percentChangeDown: Int
    ) {
        settingsRepo.setSettings(
            // TODO add lower prob
            Settings(
                1.0 / nSongs.toDouble(),
                percentChangeUp.toDouble() / 100.0,
                percentChangeDown.toDouble() / 100.0,
                0.5
            )
        )

    }

}