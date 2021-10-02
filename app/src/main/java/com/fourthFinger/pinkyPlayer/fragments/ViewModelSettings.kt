package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ToastUtil
import com.fourthFinger.pinkyPlayer.random_playlist.SaveFile
import com.fourthFinger.pinkyPlayer.random_playlist.Settings
import com.fourthFinger.pinkyPlayer.random_playlist.SettingsRepo
import kotlin.math.roundToInt

class ViewModelSettings : ViewModel() {

    private val settingsRepo = SettingsRepo.getInstance()

    fun getMaxNumberOfSongs(): String {
        return (1.0 / settingsRepo.getMaxPercent()).roundToInt().toString()
    }

    fun getPercentChangeUp(): String {
        return (settingsRepo.getPercentChangeUp()*100.0).roundToInt().toString()
    }

    fun getPercentChangeDown(): String {
        return (settingsRepo.getPercentChangeDown()*100.0).roundToInt().toString()
    }

    fun fabClicked(
        context: Context,
        navController: NavController,
        nSongs: Int,
        percentChangeUp: Int,
        percentChangeDown: Int
    ) {
        if (validateInput(context, nSongs, percentChangeUp, percentChangeDown)) {
            updateSettings(context, nSongs, percentChangeUp, percentChangeDown)
            if (navController.currentDestination?.id == R.id.FragmentSettings) {
                navController.popBackStack()
            } else {
                return
            }
        } else {
            return
        }
    }

    private fun validateInput(
        context: Context,
        nSongs: Int,
        percentChangeUp: Int,
        percentChangeDown: Int
    ): Boolean {
        return if (nSongs < 1) {
            ToastUtil.showToast(context, R.string.max_percent_error)
            false
        } else if (percentChangeUp < 1 || percentChangeUp > 100) {
            ToastUtil.showToast(context, R.string.percent_change_error)
            false
        } else if (percentChangeDown < 1 || percentChangeDown > 100) {
            ToastUtil.showToast(context, R.string.percent_change_error)
            false
        } else {
            true
        }
    }

    private fun updateSettings(
        context: Context,
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
        SaveFile.saveFile(context)
    }

}