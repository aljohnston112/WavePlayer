package com.example.pinky_player.media_controller

import java.io.Serializable

data class Settings(
        val maxPercent: Double,
        val percentChangeUp: Double,
        val percentChangeDown: Double,
        val lowerProb: Double
) : Serializable