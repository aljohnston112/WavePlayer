package com.example.waveplayer;

import android.graphics.PorterDuff;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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
            Toast toast = Toast.makeText(fragmentSettings.getContext(),
                    R.string.percent_change_error, Toast.LENGTH_LONG);
            toast.show();
            percentChangeUp = -1;
        }
        return percentChangeUp;
    }

    private int getPercentChangeDown() {
        ActivityMain activityMain = fragmentSettings.activityMain;
        int percentChangeDown = -1;
        try {
            percentChangeDown = Integer.parseInt(editTextPercentChangeDown.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (percentChangeDown < 1 || percentChangeDown > 100) {
            Toast toast = Toast.makeText(fragmentSettings.getContext(),
                    R.string.percent_change_error, Toast.LENGTH_LONG);
            toast.getView().getBackground().setColorFilter(
                    activityMain.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
            TextView text = toast.getView().findViewById(android.R.id.message);
            text.setTextSize(16);
            toast.show();
            percentChangeDown = -1;
        }
        return percentChangeDown;
    }

    private void updateSettings(int nSongs, int percentChangeUp, int percentChangeDown) {
        double maxPercent = (1.0 / (double) nSongs);
        ActivityMain activityMain = (ActivityMain) fragmentSettings.getActivity();
        activityMain.setMaxPercent(maxPercent);
        activityMain.setPercentChangeUp(((double) percentChangeUp) / 100.0);
        activityMain.setPercentChangeDown(((double) percentChangeDown) / 100.0);
    }

}
