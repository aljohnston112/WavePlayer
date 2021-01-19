package com.example.waveplayer.fragments.fragment_pane_song;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.R;

public class OnClickListenerSongPane implements View.OnClickListener {

    // TODO does not appear to work (resized to different dimensions)

    private final ActivityMain activityMain;

    public OnClickListenerSongPane(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onClick(View v) {
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
    }

    private void openFragmentSong() {
        FragmentManager fragmentManager = activityMain.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).navigate(R.id.fragmentSong);
        }
    }

}