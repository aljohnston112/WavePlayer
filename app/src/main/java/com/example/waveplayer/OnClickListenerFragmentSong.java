package com.example.waveplayer;

import android.view.View;
import android.widget.ImageButton;

public class OnClickListenerFragmentSong implements View.OnClickListener {

    ActivityMain activityMain;

    public OnClickListenerFragmentSong(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }


    @Override
    public void onClick(View v) {
        synchronized (activityMain.lock) {
            switch (v.getId()) {
                case R.id.button_thumb_down:
                    activityMain.serviceMain.currentPlaylist.getProbFun().bad(
                            activityMain.serviceMain.getCurrentSong(), ServiceMain.PERCENT_CHANGE);
                    activityMain.serviceMain.saveFile();
                    break;
                case R.id.button_thumb_up:
                    activityMain.serviceMain.currentPlaylist.getProbFun().good(
                            activityMain.serviceMain.getCurrentSong(), ServiceMain.PERCENT_CHANGE);
                    activityMain.serviceMain.saveFile();
                    break;
                case R.id.imageButtonShuffle:
                    // TODO
                    break;
                case R.id.imageButtonPrev:
                    activityMain.playPrevious();
                    break;
                case R.id.imageButtonPlayPause:
                    activityMain.pauseOrPlay();
                    break;
                case R.id.imageButtonNext:
                    activityMain.playNext();
                    break;
                case R.id.imageButtonRepeat:
                    // TODO
            }
        }
    }

}
