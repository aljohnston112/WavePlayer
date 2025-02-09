package io.fourth_finger.settings_repository

import java.io.Serializable

data class Settings(
        val maxPercent: Double,
        val percentChangeUp: Double,
        val percentChangeDown: Double,
        val lowerProb: Double
) : Serializable