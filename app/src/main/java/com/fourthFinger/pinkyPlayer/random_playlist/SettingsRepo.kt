package com.fourthFinger.pinkyPlayer.random_playlist

class SettingsRepo private constructor() {

    private var settings = Settings(0.1, 0.1, 0.5, 0.1)
    fun getSettings(): Settings {
        return settings
    }
    fun setSettings(settings: Settings) {
        this.settings = settings
    }

    fun getMaxPercent(): Double {
        return settings.maxPercent
    }
    fun setMaxPercent(maxPercent: Double) {
        require(!(maxPercent < 0 || maxPercent > 1.0)) {
            "maxPercent passed into the ProbFunTree constructor must be above 0 and below 1.0 "
        }
        settings = Settings(
            maxPercent,
            settings.percentChangeUp,
            settings.percentChangeDown,
            settings.lowerProb
        )
    }

    fun setPercentChangeUp(percentChangeUp: Double) {
        settings = Settings(
            settings.maxPercent,
            percentChangeUp,
            settings.percentChangeDown,
            settings.lowerProb
        )
    }
    fun getPercentChangeUp(): Double {
        return settings.percentChangeUp
    }

    fun setPercentChangeDown(percentChangeDown: Double) {
        settings = Settings(
            settings.maxPercent,
            settings.percentChangeUp,
            percentChangeDown,
            settings.lowerProb
        )
    }
    fun getPercentChangeDown(): Double {
        return settings.percentChangeDown
    }

    fun setLowerProb(lowerProb: Double) {
        settings = Settings(
            settings.maxPercent,
            settings.percentChangeUp,
            settings.percentChangeDown,
            lowerProb
        )
    }
    fun getLowerProb(): Double {
        return settings.lowerProb
    }

    companion object{

        private var INSTANCE: SettingsRepo? = null

        fun getInstance(): SettingsRepo {
            if(INSTANCE == null){
                INSTANCE = SettingsRepo()
            }
            return INSTANCE!!
        }
    }

}