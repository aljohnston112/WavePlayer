package com.example.waveplayer.media_controller

import java.io.Serializable

class Settings(val maxPercent: Double, val percentChangeUp: Double, val percentChangeDown: Double, val lowerProb: Double) : Serializable