package com.fourthFinger.pinkyPlayer.random_playlist

class SettingsRepo private constructor() {

    private var settings = Settings(
        0.1,
        0.1,
        0.5,
        0.1
    )

    fun getSettings(): Settings {
        return settings
    }

    fun setSettings(settings: Settings) {
        this.settings = settings
    }

    fun getMaxPercent(): Double {
        return settings.maxPercent
    }

    fun getPercentChangeUp(): Double {
        return settings.percentChangeUp
    }

    fun getPercentChangeDown(): Double {
        return settings.percentChangeDown
    }

    fun getLowerProb(): Double {
        return settings.lowerProb
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