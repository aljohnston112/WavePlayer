package com.example.waveplayer;

import android.view.View;

public class OnClickListenerFragmentSong implements View.OnClickListener {

    ActivityMain activityMain;

    public OnClickListenerFragmentSong(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onClick(View v) {
        synchronized (activityMain.lock) {
            if (v.getId() == R.id.button_thumb_down) {
                activityMain.serviceMain.currentPlaylist.getProbFun().bad(
                        activityMain.serviceMain.getCurrentSong(), ServiceMain.PERCENT_CHANGE);
                activityMain.serviceMain.saveFile();
            } else if (v.getId() == R.id.button_thumb_up) {
                activityMain.serviceMain.currentPlaylist.getProbFun().good(
                        activityMain.serviceMain.getCurrentSong(), ServiceMain.PERCENT_CHANGE);
                activityMain.serviceMain.saveFile();
            } else if (v.getId() == R.id.imageButtonShuffle) {
                // TODO
            } else if (v.getId() == R.id.imageButtonPrev) {
                activityMain.playPrevious();
            } else if (v.getId() == R.id.imageButtonPlayPause) {
                activityMain.pauseOrPlay();
            } else if (v.getId() == R.id.imageButtonNext) {
                activityMain.playNext();
            } else if (v.getId() == R.id.imageButtonRepeat) {
                // TODO
            }
        }
    }

}