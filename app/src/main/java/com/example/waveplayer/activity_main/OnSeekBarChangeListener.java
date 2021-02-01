package com.example.waveplayer.activity_main;

import android.widget.SeekBar;

import com.example.waveplayer.activity_main.ActivityMain;

public class OnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

    private final OnSeekBarChangeCallback onSeekBarChangeCallback;

    public interface OnSeekBarChangeCallback{
        void onStopTrackingTouch(SeekBar seekBar);
    }

    public OnSeekBarChangeListener(OnSeekBarChangeCallback onSeekBarChangeCallback) {
        this.onSeekBarChangeCallback = onSeekBarChangeCallback;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        onSeekBarChangeCallback.onStopTrackingTouch(seekBar);
    }

}