package com.example.waveplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class FragmentSettings extends Fragment {

    ActivityMain activityMain;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        updateMainContent();
        loadSettings();
    }

    private void updateMainContent() {
        activityMain.setActionBarTitle(getResources().getString(R.string.settings));
        updateFAB();
    }

    private void updateFAB() {
        final EditText editTextNSongs = activityMain.findViewById(R.id.editTextNSongs);
        final EditText editTextPercentChange = activityMain.findViewById(R.id.editTextPercentChange);
        activityMain.setFabImage(R.drawable.ic_check_black_24dp);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int nSongs = getNSongs();
                if (nSongs == -1) {
                    return;
                }
                int percentChange = getPercentChange();
                if (percentChange == -1) {
                    return;
                }
                updateSettings(nSongs, percentChange);
                NavController navController = NavHostFragment.findNavController(FragmentSettings.this);
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
                    Toast toast = Toast.makeText(getContext(), R.string.max_percent_error, Toast.LENGTH_LONG);
                    toast.show();
                }
                return nSongs;
            }

            private int getPercentChange() {
                int percentChange = -1;
                try {
                    percentChange = Integer.parseInt(editTextPercentChange.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (percentChange < 1 || percentChange > 100) {
                    Toast toast = Toast.makeText(getContext(), R.string.percent_change_error, Toast.LENGTH_LONG);
                    toast.show();
                }
                return percentChange;
            }

            private void updateSettings(int nSongs, int percentChange) {
                ServiceMain.MAX_PERCENT = (1.0 / (double) nSongs);
                for (RandomPlaylist randomPlaylist : activityMain.serviceMain.playlists) {
                    randomPlaylist.setMaxPercent(ServiceMain.MAX_PERCENT);
                }
                for (RandomPlaylist randomPlaylist : activityMain.serviceMain.directoryPlaylists.values()) {
                    randomPlaylist.setMaxPercent(ServiceMain.MAX_PERCENT);
                }
                activityMain.serviceMain.masterPlaylist.setMaxPercent(ServiceMain.MAX_PERCENT);
                ServiceMain.PERCENT_CHANGE = ((double) percentChange) / 100.0;
            }

        });
    }

    private void loadSettings() {
        EditText editTextNSongs = activityMain.findViewById(R.id.editTextNSongs);
        EditText editTextPercentChange = activityMain.findViewById(R.id.editTextPercentChange);
        editTextNSongs.setText(String.valueOf((int) Math.round(1.0 / ServiceMain.MAX_PERCENT)));
        editTextPercentChange.setText(String.valueOf((int) Math.round(ServiceMain.PERCENT_CHANGE * 100.0)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain = null;
    }

}