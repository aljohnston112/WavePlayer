package com.example.waveplayer.media_controller;

import java.io.Serializable;

public class Settings implements Serializable {

    public final double maxPercent;

    public final double percentChangeUp;

    public final double percentChangeDown;

    public final double lowerProb;

    public Settings(double maxPercent, double percentChangeUp, double percentChangeDown, double lowerProb){
        this.maxPercent = maxPercent;
        this.percentChangeUp = percentChangeUp;
        this.percentChangeDown = percentChangeDown;
        this.lowerProb = lowerProb;
    }

}
