package com.example.waveplayer.fragments.fragment_song;

import android.view.View;
import android.widget.ImageButton;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.R;

public class OnClickListenerFragmentSong implements View.OnClickListener {

    private final ActivityMain activityMain;

    public OnClickListenerFragmentSong(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onClick(View view) {
        synchronized (ActivityMain.lock) {
            if (view.getId() == R.id.button_thumb_down) {
                Song song = activityMain.getSong(activityMain.getCurrentAudioUri().id);
                if(song != null) {
                    activityMain.getCurrentPlaylist().globalBad(
                            song, activityMain.getPercentChangeDown());
                    activityMain.saveFile();
                }
            } else if (view.getId() == R.id.button_thumb_up) {
                Song song = activityMain.getSong(activityMain.getCurrentAudioUri().id);
                if(song != null) {
                    activityMain.getCurrentPlaylist().good(activityMain.getApplicationContext(),
                            song, activityMain.getPercentChangeUp(), true);
                    activityMain.saveFile();
                }
            } else if (view.getId() == R.id.imageButtonShuffle) {
                ImageButton imageButton = (ImageButton)view;
                if(activityMain.shuffling()){
                    activityMain.shuffling(false);
                    imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp);
                } else {
                    activityMain.shuffling(true);
                    imageButton.setImageResource(R.drawable.ic_shuffle_black_24dp);
                }
            } else if (view.getId() == R.id.imageButtonPrev) {
                activityMain.playPrevious();
            } else if (view.getId() == R.id.imageButtonPlayPause) {
                activityMain.pauseOrPlay();
            } else if (view.getId() == R.id.imageButtonNext) {
                activityMain.playNext();
            } else if (view.getId() == R.id.imageButtonRepeat) {
                ImageButton imageButton = (ImageButton)view;
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