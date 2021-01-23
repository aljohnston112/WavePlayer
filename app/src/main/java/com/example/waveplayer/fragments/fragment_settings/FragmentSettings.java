package com.example.waveplayer.fragments.fragment_settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.MediaData;

public class FragmentSettings extends Fragment {

    private OnClickListenerFABFragmentSettings onClickListenerFABFragmentSettings;

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
        updateMainContent();
        loadSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void updateMainContent() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.settings));
        updateFAB();
    }

    private void updateFAB() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        final EditText editTextNSongs = activityMain.findViewById(R.id.editTextNSongs);
        final EditText editTextPercentChangeUp = activityMain.findViewById(R.id.editTextPercentChangeUp);
        final EditText editTextPercentChangeDown = activityMain.findViewById(R.id.editTextPercentChangeDown);
        activityMain.setFabImage(R.drawable.ic_check_black_24dp);
        activityMain.setFABText(R.string.fab_save);
        onClickListenerFABFragmentSettings = new OnClickListenerFABFragmentSettings(
                this, editTextNSongs, editTextPercentChangeDown, editTextPercentChangeUp);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentSettings);
        activityMain.showFab(true);
    }

    private void loadSettings() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        EditText editTextNSongs = activityMain.findViewById(R.id.editTextNSongs);
        EditText editTextPercentChangeUp = activityMain.findViewById(R.id.editTextPercentChangeUp);
        EditText editTextPercentChangeDown = activityMain.findViewById(R.id.editTextPercentChangeDown);
        editTextNSongs.setText(
                String.valueOf((int) Math.round(1.0 / MediaData.getInstance(activityMain).getMaxPercent())));
        editTextPercentChangeUp.setText(
                String.valueOf((int) Math.round(MediaData.getInstance(activityMain).getPercentChangeUp() * 100.0)));
        editTextPercentChangeDown.setText(
                String.valueOf((int) Math.round(MediaData.getInstance(activityMain).getPercentChangeDown() * 100.0)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setFabOnClickListener(null);
        onClickListenerFABFragmentSettings = null;
    }

}