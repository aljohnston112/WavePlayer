package com.example.waveplayer;

import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class OnClickListenerFABFragmentSettings implements View.OnClickListener {

    final FragmentSettings fragmentSettings;

    final EditText editTextNSongs;
    final EditText editTextPercentChangeDown;
    final EditText editTextPercentChangeUp;

    OnClickListenerFABFragmentSettings(FragmentSettings fragmentSettings, EditText editTextNSongs,
                                       EditText editTextPercentChangeDown, EditText editTextPercentChangeUp) {
        this.fragmentSettings = fragmentSettings;
        this.editTextNSongs = editTextNSongs;
        this.editTextPercentChangeDown = editTextPercentChangeDown;
        this.editTextPercentChangeUp = editTextPercentChangeUp;
    }

    @Override
    public void onClick(View view) {
        int nSongs = getNSongs();
        if (nSongs == -1) {
            return;
        }
        int percentChangeUp = getPercentChangeUp();
        if (percentChangeUp == -1) {
            return;
        }
        int percentChangeDown = getPercentChangeDown();
        if (percentChangeDown == -1) {
            return;
        }
        updateSettings(nSongs, percentChangeUp, percentChangeDown);
        NavController navController = NavHostFragment.findNavController(fragmentSettings);
        navController.popBackStack();
    }

    private int getNSongs() {
        int nSongs = -1;
        try {
            nSongs = Integer.parseInt(editTextNSongs.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (nSongs < 1) {
            Toast toast = Toast.makeText(
                    fragmentSettings.getContext(), R.string.max_percent_error, Toast.LENGTH_LONG);
            toast.show();
        }
        return nSongs;
    }

    private int getPercentChangeUp() {
        int percentChangeUp = -1;
        try {
            percentChangeUp = Integer.parseInt(editTextPercentChangeUp.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (percentChangeUp < 1 || percentChangeUp > 100) {
            Toast toast = Toast.makeText(fragmentSettings.getContext(),
                    R.string.percent_change_error, Toast.LENGTH_LONG);
            toast.show();
        }
        return percentChangeUp;
    }

    private int getPercentChangeDown() {
        int percentChangeDown = -1;
        try {
            percentChangeDown = Integer.parseInt(editTextPercentChangeDown.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (percentChangeDown < 1 || percentChangeDown > 100) {
            Toast toast = Toast.makeText(fragmentSettings.getContext(),
                    R.string.percent_change_error, Toast.LENGTH_LONG);
            toast.show();
        }
        return percentChangeDown;
    }

    private void updateSettings(int nSongs, int percentChangeUp, int percentChangeDown) {
        ServiceMain.MAX_PERCENT = (1.0 / (double) nSongs);
        for (RandomPlaylist randomPlaylist : fragmentSettings.activityMain.serviceMain.playlists) {
            randomPlaylist.setMaxPercent(ServiceMain.MAX_PERCENT);
        }
        for (RandomPlaylist randomPlaylist : fragmentSettings.activityMain.serviceMain.directoryPlaylists.values()) {
            randomPlaylist.setMaxPercent(ServiceMain.MAX_PERCENT);
        }
        fragmentSettings.activityMain.serviceMain.masterPlaylist.setMaxPercent(ServiceMain.MAX_PERCENT);
        ServiceMain.PERCENT_CHANGE_UP = ((double) percentChangeUp) / 100.0;
        ServiceMain.PERCENT_CHANGE_DOWN = ((double) percentChangeDown) / 100.0;
    }

}
