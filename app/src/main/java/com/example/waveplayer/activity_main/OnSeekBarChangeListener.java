package com.example.waveplayer.activity_main;

import android.widget.SeekBar;

import com.example.waveplayer.activity_main.ActivityMain;

public class OnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

    private final ActivityMain activityMain;

    public OnSeekBarChangeListener(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        activityMain.seekTo(seekBar.getProgress());
    }

}