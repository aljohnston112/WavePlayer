package com.example.waveplayer.activity_main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ActivityMain;

public class OnDestinationChangedListenerPanes
        implements NavController.OnDestinationChangedListener {

    private final ActivityMain activityMain;

    public OnDestinationChangedListenerPanes(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onDestinationChanged(@NonNull NavController controller,
                                     @NonNull final NavDestination destination,
                                     @Nullable Bundle arguments) {
        if (destination.getId() != R.id.fragmentSong) {
            if (activityMain.songInProgress()) {
                activityMain.fragmentSongVisible(false);
                activityMain.showSongPane();
            }
            activityMain.updateUI();
        } else {
            activityMain.fragmentSongVisible(true);
            activityMain.hideSongPane();
            activityMain.updateUI();
        }

    }

}