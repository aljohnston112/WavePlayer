package io.fourthFinger.pinkyPlayer.settings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import io.fourthFinger.pinkyPlayer.ApplicationMain
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.pinkyPlayer.ToastUtil
import io.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import kotlin.math.roundToInt

class ViewModelSettings(
    private val settingsRepo: SettingsRepo,
    private val playlistsRepo: PlaylistsRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun getMaxNumberOfSongs(): String {
        return (1.0 / settingsRepo.settings.value!!.maxPercent).roundToInt().toString()
    }

    fun getPercentChangeUp(): String {
        return (settingsRepo.settings.value!!.percentChangeUp*100.0).roundToInt().toString()
    }

    fun getPercentChangeDown(): String {
        return (settingsRepo.settings.value!!.percentChangeDown*100.0).roundToInt().toString()
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
        // TODO add lower prob UI
        val settings = Settings(
            1.0 / nSongs.toDouble(),
            percentChangeUp.toDouble() / 100.0,
            percentChangeDown.toDouble() / 100.0,
            0.5
        )
        settingsRepo.setSettings(
            context,
            settings
            )
        playlistsRepo.setMaxPercent(settings.maxPercent)
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                // Create a SavedStateHandle for this ViewModel from extras
                val savedStateHandle = extras.createSavedStateHandle()

                return ViewModelSettings(
                    (application as ApplicationMain).settingsRepo,
                    application.playlistsRepo,
                    savedStateHandle
                ) as T
            }
        }
    }


}