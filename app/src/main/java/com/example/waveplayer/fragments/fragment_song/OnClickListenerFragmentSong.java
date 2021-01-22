package com.example.waveplayer.fragments.fragment_song;

import android.view.View;
import android.widget.ImageButton;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.R;

public class OnClickListenerFragmentSong implements View.OnClickListener {

    private final ActivityMain activityMain;

    public OnClickListenerFragmentSong(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onClick(View v) {
        synchronized (activityMain.lock) {
            if (v.getId() == R.id.button_thumb_down) {
                Long songID = activityMain.getCurrentSong().getID();
                if(songID != null) {
                    activityMain.getCurrentPlaylist().bad(
                            songID, activityMain.getPercentChangeDown());
                    activityMain.saveFile();
                }
            } else if (v.getId() == R.id.button_thumb_up) {
                Long songID = activityMain.getCurrentSong().getID();
                if(songID != null) {
                    activityMain.getCurrentPlaylist().good(
                            songID, activityMain.getPercentChangeUp(), true);
                    activityMain.saveFile();
                }
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