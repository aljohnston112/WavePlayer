package com.example.waveplayer.fragments.fragment_settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentSettingsBinding;
import com.example.waveplayer.media_controller.MediaData;

public class FragmentSettings extends Fragment {

    private ViewModelActivityMain viewModelActivityMain;

    private FragmentSettingsBinding binding;

    private OnClickListenerFABFragmentSettings onClickListenerFABFragmentSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        updateMainContent();
        loadSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void updateMainContent() {
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.settings));
        updateFAB();
    }

    private void updateFAB() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        final EditText editTextNSongs = activityMain.findViewById(R.id.edit_text_n_songs);
        final EditText editTextPercentChangeUp = activityMain.findViewById(R.id.edit_text_percent_change_up);
        final EditText editTextPercentChangeDown = activityMain.findViewById(R.id.edit_text_percent_change_down);
        viewModelActivityMain.setFabImage(R.drawable.ic_check_black_24dp);
        viewModelActivityMain.setFABText(R.string.fab_save);
        onClickListenerFABFragmentSettings = new OnClickListenerFABFragmentSettings(
                this, editTextNSongs, editTextPercentChangeDown, editTextPercentChangeUp);
        viewModelActivityMain.setFabOnClickListener(onClickListenerFABFragmentSettings);
        viewModelActivityMain.showFab(true);
    }

    private void loadSettings() {
        EditText editTextNSongs = binding.editTextNSongs;
        EditText editTextPercentChangeUp = binding.editTextPercentChangeUp;
        EditText editTextPercentChangeDown = binding.editTextPercentChangeDown;
        editTextNSongs.setText(
                String.valueOf((int) Math.round(1.0 / MediaData.getInstance().getMaxPercent())));
        editTextPercentChangeUp.setText(
                String.valueOf((int) Math.round(MediaData.getInstance().getPercentChangeUp() * 100.0)));
        editTextPercentChangeDown.setText(
                String.valueOf((int) Math.round(MediaData.getInstance().getPercentChangeDown() * 100.0)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModelActivityMain.setFabOnClickListener(null);
        onClickListenerFABFragmentSettings = null;
        binding = null;
        viewModelActivityMain = null;
    }

}