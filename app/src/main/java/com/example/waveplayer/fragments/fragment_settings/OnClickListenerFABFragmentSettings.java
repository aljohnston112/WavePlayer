package com.example.waveplayer.fragments.fragment_settings;

import android.view.View;
import android.widget.EditText;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.MediaData;

public class OnClickListenerFABFragmentSettings implements View.OnClickListener {

    private final FragmentSettings fragmentSettings;

    private final EditText editTextNSongs;
    private final EditText editTextPercentChangeDown;
    private final EditText editTextPercentChangeUp;

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
            ActivityMain activityMain = (ActivityMain) fragmentSettings.getActivity();
            activityMain.showToast(R.string.max_percent_error);
            nSongs = -1;
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
            ActivityMain activityMain = (ActivityMain) fragmentSettings.getActivity();
            activityMain.showToast(R.string.percent_change_error);
            percentChangeUp = -1;
        }
        return percentChangeUp;
    }

    private int getPercentChangeDown() {
        ActivityMain activityMain = (ActivityMain) fragmentSettings.getActivity();
        int percentChangeDown = -1;
        try {
            percentChangeDown = Integer.parseInt(editTextPercentChangeDown.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (percentChangeDown < 1 || percentChangeDown > 100) {
            activityMain.showToast(R.string.percent_change_error);
            percentChangeDown = -1;
        }
        return percentChangeDown;
    }

    private void updateSettings(int nSongs, int percentChangeUp, int percentChangeDown) {
        ActivityMain activityMain = (ActivityMain) fragmentSettings.getActivity();
        double maxPercent = (1.0 / (double) nSongs);
        activityMain.setMaxPercent(maxPercent);
        activityMain.setPercentChangeUp(((double) percentChangeUp) / 100.0);
        activityMain.setPercentChangeDown(((double) percentChangeDown) / 100.0);
    }

}