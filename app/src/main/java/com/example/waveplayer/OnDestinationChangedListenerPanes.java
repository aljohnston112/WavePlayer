package com.example.waveplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

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
            } else {
                activityMain.updateUI();
            }
        } else {
            activityMain.fragmentSongVisible(true);
            activityMain.hideSongPane();
            activityMain.updateUI();
        }

    }

}