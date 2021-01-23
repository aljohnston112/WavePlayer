package com.example.waveplayer.media_controller;

import android.util.Log;

import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.io.Serializable;

public class Settings implements Serializable {

    public final double maxPercent;

    public final double percentChangeUp;

    public final double percentChangeDown;

    public Settings(double maxPercent, double percentChangeUp, double percentChangeDown){
        this.maxPercent = maxPercent;
        this.percentChangeUp = percentChangeUp;
        this.percentChangeDown = percentChangeDown;
    }

}
