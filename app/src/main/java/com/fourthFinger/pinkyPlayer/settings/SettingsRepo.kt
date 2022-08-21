package com.fourthFinger.pinkyPlayer.settings

import android.content.Context

class SettingsRepo private constructor(context: Context) {

    private val settingsDataSource = SettingsDataSource.getInstance()

    var settings =
        settingsDataSource.loadSettings(context)
            ?: Settings(
                0.1,
                0.1,
                0.5,
                0.1
            )
        private set

    fun setSettings(context: Context, settings: Settings) {
        this.settings = settings
        settingsDataSource.saveSettings(context, settings)
    }

    companion object {

        private var INSTANCE: SettingsRepo? = null

        @Synchronized
        fun getInstance(context: Context): SettingsRepo {
            if (INSTANCE == null) {
                INSTANCE = SettingsRepo(context)
            }
            return INSTANCE!!
        }
    }

}