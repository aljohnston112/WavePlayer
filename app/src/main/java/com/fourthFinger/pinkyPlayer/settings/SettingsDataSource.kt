package com.fourthFinger.pinkyPlayer.settings

import android.content.Context
import com.fourthFinger.pinkyPlayer.FileUtil

private const val FILE_NAME_SETTINGS = "SETTINGS"
private const val SAVE_FILE_VERIFICATION_NUMBER = 4596834290567902435L

class SettingsDataSource private constructor(){

    fun saveSettings(context: Context, settings: Settings) {
        FileUtil.save(
            settings,
            context,
            FILE_NAME_SETTINGS,
            SAVE_FILE_VERIFICATION_NUMBER
        )
    }

    fun loadSettings(context: Context): Settings? {
        return FileUtil.load(
            context,
            FILE_NAME_SETTINGS,
            SAVE_FILE_VERIFICATION_NUMBER
        )
    }

    companion object {

        private var INSTANCE: SettingsDataSource? = null

        @Synchronized
        fun getInstance(): SettingsDataSource {
            if (INSTANCE == null) {
                INSTANCE = SettingsDataSource()
            }
            return INSTANCE!!
        }
    }

}