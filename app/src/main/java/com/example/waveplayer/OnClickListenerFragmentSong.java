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
                activityMain.getCurrentPlaylist().bad(
                        activityMain.getCurrentSong(), activityMain.getPercentChangeDown());
                activityMain.saveFile();
            } else if (v.getId() == R.id.button_thumb_up) {
                activityMain.getCurrentPlaylist().good(
                        activityMain.getCurrentSong(), activityMain.getPercentChangeUp());
                activityMain.saveFile();
            } else if (v.getId() == R.id.imageButtonShuffle) {
                ImageButton imageButton = (ImageButton)v;
                if(activityMain.shuffling()){
                    activityMain.shuffling(false);
                    imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp);
                } else {
                    activityMain.shuffling(true);
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
                if(activityMain.loopingOne()){
                    activityMain.loopingOne(false);
                    imageButton.setImageResource(R.drawable.repeat_white_24dp);
                } else if(activityMain.looping()){
                    activityMain.looping(false);
                    activityMain.loopingOne(true);
                    imageButton.setImageResource(R.drawable.repeat_one_black_24dp);
                } else{
                    activityMain.looping(true);
                    activityMain.loopingOne(false);
                    imageButton.setImageResource(R.drawable.repeat_black_24dp);
                }
            }
        }
    }

}
