package com.example.waveplayer;

import android.widget.SeekBar;

public class OnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

    ActivityMain activityMain;

    public OnSeekBarChangeListener(ActivityMain activityMain){
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
        MediaPlayerWURI mediaPlayerWURI =
                activityMain.serviceMain.songsMap.get(activityMain.serviceMain.currentSong.getUri());
        if (mediaPlayerWURI != null) {
            mediaPlayerWURI.seekTo(seekBar.getProgress());
            if(!activityMain.serviceMain.isPlaying()){
                activityMain.pauseOrPlay();
            }
        }
    }

}