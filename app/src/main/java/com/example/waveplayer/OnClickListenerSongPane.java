package com.example.waveplayer;

import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

public class OnClickListenerSongPane implements View.OnClickListener {

    // TODO does not appear to work

    ActivityMain activityMain;

    public OnClickListenerSongPane(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onClick(View v) {
        Log.v(ActivityMain.TAG, "OnClickListenerSongPane onClick started");
        synchronized (activityMain.lock) {
            if (v.getId() == R.id.imageButtonSongPaneNext) {
                activityMain.playNext();
            } else if (v.getId() == R.id.imageButtonSongPanePlayPause) {
                activityMain.pauseOrPlay();
            } else if (v.getId() == R.id.imageButtonSongPanePrev) {
                activityMain.playPrevious();
            } else if (v.getId() == R.id.textViewSongPaneSongName ||
                    v.getId() == R.id.imageViewSongPaneSongArt) {
                openFragmentSong();
            }
        }
        Log.v(ActivityMain.TAG, "OnClickListenerSongPane onClick ended");
    }

    private void openFragmentSong() {
        Log.v(ActivityMain.TAG, "Getting ready to open FragmentSong");
        FragmentManager fragmentManager = activityMain.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).navigate(R.id.fragmentSong);
        }
        Log.v(ActivityMain.TAG, "Done getting ready to open FragmentSong");
    }

}