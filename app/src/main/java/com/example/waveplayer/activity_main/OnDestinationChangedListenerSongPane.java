package com.example.waveplayer.activity_main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ActivityMain;

public class OnDestinationChangedListenerSongPane
        implements NavController.OnDestinationChangedListener {

    private final OnDestinationChangedCallback onDestinationChangedCallback;

    public interface OnDestinationChangedCallback {
        void onDestinationChanged(@NonNull final NavDestination destination);
    }

    public OnDestinationChangedListenerSongPane(OnDestinationChangedCallback onDestinationChangedCallback) {
        this.onDestinationChangedCallback = onDestinationChangedCallback;
    }

    @Override
    public void onDestinationChanged(@NonNull NavController controller,
                                     @NonNull final NavDestination destination,
                                     @Nullable Bundle arguments) {
        onDestinationChangedCallback.onDestinationChanged(destination);
    }

}