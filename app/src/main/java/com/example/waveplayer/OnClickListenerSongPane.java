package com.example.waveplayer;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

public class OnClickListenerSongPane implements View.OnClickListener {

    ActivityMain activityMain;

    public OnClickListenerSongPane(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onClick(View v) {
        synchronized (activityMain.lock) {
            switch (v.getId()) {
                case R.id.imageButtonSongPaneNext:
                    activityMain.playNext();
                    break;
                case R.id.imageButtonSongPanePlayPause:
                    activityMain.pauseOrPlay();
                    break;
                case R.id.imageButtonSongPanePrev:
                    activityMain.playPrevious();
                    break;
                case R.id.textViewSongPaneSongName:
                case R.id.imageViewSongPaneSongArt:
                    FragmentManager fragmentManager = activityMain.getSupportFragmentManager();
                    Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
                    if (fragment != null) {
                        NavHostFragment.findNavController(fragment).navigate(R.id.fragmentSong);
                    }
                    break;
            }
        }
    }

}