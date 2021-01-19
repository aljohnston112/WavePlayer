package com.example.waveplayer;

import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

public class OnDestinationChangedListenerToolbar implements NavController.OnDestinationChangedListener {

    private final ActivityMain activityMain;

    public OnDestinationChangedListenerToolbar(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onDestinationChanged(@NonNull NavController controller,
                                     @NonNull final NavDestination destination,
                                     @Nullable Bundle arguments) {
        activityMain.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
                Menu menu = toolbar.getMenu();
                if (menu.size() > 0) {
                    menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(
                            destination.getId() == R.id.fragmentPlaylist ||
                                    destination.getId() == R.id.fragmentSongs);
                    menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).setVisible(
                            destination.getId() == R.id.fragmentSong ||
                                    destination.getId() == R.id.fragmentPlaylist);
                    menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(
                            destination.getId() == R.id.fragmentSongs ||
                                    destination.getId() == R.id.fragmentPlaylist ||
                                    destination.getId() == R.id.FragmentPlaylists);
                    menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).setVisible(
                            destination.getId() == R.id.fragmentSong ||
                                    destination.getId() == R.id.fragmentPlaylist);
                }
            }
        });
    }

}