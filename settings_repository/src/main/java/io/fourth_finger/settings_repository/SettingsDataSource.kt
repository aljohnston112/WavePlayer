package io.fourth_finger.settings_repository

import android.content.Context
import io.fourth_finger.file_utility.FileUtil

private const val FILE_NAME_SETTINGS = "SETTINGS"
private const val SAVE_FILE_VERIFICATION_NUMBER = 4596834290567902435L

internal class SettingsDataSource private constructor(){

    fun saveSettings(context: Context, settings: Settings) {
        FileUtil.Companion.save(
            settings,
            context,
            FILE_NAME_SETTINGS,
            SAVE_FILE_VERIFICATION_NUMBER
        )
    }

    fun loadSettings(context: Context): Settings? {
        return FileUtil.Companion.load(
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