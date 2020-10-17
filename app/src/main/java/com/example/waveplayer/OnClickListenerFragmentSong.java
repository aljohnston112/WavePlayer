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
                ImageButton imageButton = (ImageButton)v;
                if(activityMain.serviceMain.shuffling){
                    activityMain.serviceMain.shuffling = false;
                    imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp);
                } else {
                    activityMain.serviceMain.shuffling = true;
                    imageButton.setImageResource(R.drawable.ic_shuffle_black_24dp);
                }
            } else if (v.getId() == R.id.imageButtonPrev) {
                activityMain.playPrevious();
            } else if (v.getId() == R.id.imageButtonPlayPause) {
                activityMain.pauseOrPlay();
            } else if (v.getId() == R.id.imageButtonNext) {
                activityMain.playNext();
            } else if (v.getId() == R.id.imageButtonRepeat) {
                ImageButton imageButton = (ImageButton)v;
                if(activityMain.serviceMain.loopingOne){
                    activityMain.serviceMain.loopingOne = false;
                    imageButton.setImageResource(R.drawable.repeat_white_24dp);
                } else if(activityMain.serviceMain.looping){
                    activityMain.serviceMain.looping = false;
                    activityMain.serviceMain.loopingOne = true;
                    imageButton.setImageResource(R.drawable.repeat_one_black_24dp);
                } else{
                    activityMain.serviceMain.looping = true;
                    activityMain.serviceMain.loopingOne = false;
                    imageButton.setImageResource(R.drawable.repeat_black_24dp);
                }
            }
        }
    }

}
