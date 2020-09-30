package com.example.waveplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Iterator;

public class FragmentSettings extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(R.string.settings);
        activityMain.fab.setOnClickListener(null);
        activityMain.fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_black_24dp));
        activityMain.fab.show();
        final EditText editTextNSongs = activityMain.findViewById(R.id.editTextNSongs);
        editTextNSongs.setText(String.valueOf((int)Math.round(1.0/ActivityMain.MAX_PERCENT)));
        final EditText editTextPercentChange = activityMain.findViewById(R.id.editTextPercentChange);
        editTextPercentChange.setText(String.valueOf((int)Math.round(ActivityMain.PERCENT_CHANGE*100.0)));
        activityMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int nSongs = -1;
                try {
                    nSongs = Integer.parseInt(editTextNSongs.getText().toString());
                } catch (NumberFormatException nfe) {}
                if (nSongs < 1) {
                    Toast toast = Toast.makeText(getContext(), R.string.max_percent_error, Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                int percentChange = -1;
                try {
                    percentChange = Integer.parseInt(editTextPercentChange.getText().toString());
                } catch (NumberFormatException nfe) {}
                if (percentChange < 1 || percentChange > 100) {
                    Toast toast = Toast.makeText(getContext(), R.string.percent_change_error, Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                ActivityMain.MAX_PERCENT = (1.0 / (double)nSongs);
                Iterator<RandomPlaylist> rp = activityMain.playlists.iterator();
                while(rp.hasNext()){
                    rp.next().setMaxPercent(ActivityMain.MAX_PERCENT);
                }
                ActivityMain.PERCENT_CHANGE = ((double) percentChange) / 100.0;
                NavController navController = NavHostFragment.findNavController(FragmentSettings.this);
                navController.popBackStack();
            }
        });
    }

}