package com.fourthFinger.pinkyPlayer.random_playlist

import java.io.Serializable

data class Settings(
        val maxPercent: Double,
        val percentChangeUp: Double,
        val percentChangeDown: Double,
        val lowerProb: Double
) : Serializable