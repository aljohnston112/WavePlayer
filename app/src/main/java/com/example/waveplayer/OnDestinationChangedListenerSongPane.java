package com.example.waveplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

public class OnDestinationChangedListenerSongPane implements NavController.OnDestinationChangedListener {

    final ActivityMain activityMain;

    public OnDestinationChangedListenerSongPane(ActivityMain activityMain){
        this.activityMain = activityMain;
    }

    @Override
    public void onDestinationChanged(@NonNull NavController controller,
                                     @NonNull final NavDestination destination,
                                     @Nullable Bundle arguments) {
        if (destination.getId() != R.id.fragmentSong) {
            if(activityMain.serviceMain.songInProgress()) {
                activityMain.serviceMain.fragmentSongVisible = false;
                activityMain.updateSongPaneUI();
                activityMain.showSongPane();
            }
        } else {
            activityMain.serviceMain.fragmentSongVisible = true;
            activityMain.hideSongPane();
        }

    }

}