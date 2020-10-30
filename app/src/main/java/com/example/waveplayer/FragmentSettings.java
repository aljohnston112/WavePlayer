package com.example.waveplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentSettings extends Fragment {

    ActivityMain activityMain;

    View view;

    OnClickListenerFABFragmentSettings onClickListenerFABFragmentSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activityMain = ((ActivityMain) getActivity());
        view = inflater.inflate(R.layout.fragment_settings, container, false);
        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateMainContent();
        loadSettings();
    }

    private void updateMainContent() {
        activityMain.setActionBarTitle(getResources().getString(R.string.settings));
        updateFAB();
    }

    private void updateFAB() {
        final EditText editTextNSongs = activityMain.findViewById(R.id.editTextNSongs);
        final EditText editTextPercentChangeUp = activityMain.findViewById(R.id.editTextPercentChangeUp);
        final EditText editTextPercentChangeDown = activityMain.findViewById(R.id.editTextPercentChangeDown);
        activityMain.setFabImage(R.drawable.ic_check_black_24dp);
        activityMain.setFABText(R.string.fab_save);
        activityMain.showFab(true);
        onClickListenerFABFragmentSettings = new OnClickListenerFABFragmentSettings(
                this, editTextNSongs, editTextPercentChangeDown, editTextPercentChangeUp);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentSettings);
    }

    private void loadSettings() {
        EditText editTextNSongs = activityMain.findViewById(R.id.editTextNSongs);
        EditText editTextPercentChangeUp = activityMain.findViewById(R.id.editTextPercentChangeUp);
        EditText editTextPercentChangeDown = activityMain.findViewById(R.id.editTextPercentChangeDown);
        editTextNSongs.setText(
                String.valueOf((int) Math.round(1.0 / activityMain.getMaxPercent())));
        editTextPercentChangeUp.setText(
                String.valueOf((int) Math.round(activityMain.getPercentChangeUp() * 100.0)));
        editTextPercentChangeDown.setText(
                String.valueOf((int) Math.round(activityMain.getPercentChangeDown() * 100.0)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.setFabOnClickListener(null);
        onClickListenerFABFragmentSettings = null;
        view = null;
        activityMain = null;
    }

}