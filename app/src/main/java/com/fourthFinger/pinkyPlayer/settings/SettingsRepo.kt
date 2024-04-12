package com.fourthFinger.pinkyPlayer.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SettingsRepo private constructor() {

    private val settingsDataSource = SettingsDataSource.getInstance()

    private val _settings = MutableLiveData<Settings>()
    val settings = _settings as LiveData<Settings>

    fun loadSettings(context: Context): Settings {
        val loadedSettings = settingsDataSource.loadSettings(context)
            ?: Settings(
                0.1,
                0.1,
                0.5,
                0.1
            )
        _settings.postValue(loadedSettings)
        return loadedSettings
    }

    fun setSettings(
        context: Context,
        settings: Settings
    ) {
        this._settings.postValue(settings)
        settingsDataSource.saveSettings(context, settings)
    }

    companion object {

        private var INSTANCE: SettingsRepo? = null

        @Synchronized
        fun getInstance(): SettingsRepo {
            if (INSTANCE == null) {
                INSTANCE = SettingsRepo()
            }
            return INSTANCE!!
        }
    }

}