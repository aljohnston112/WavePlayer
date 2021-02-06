package com.example.waveplayer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentSettingsBinding;

public class FragmentSettings extends Fragment {

    private FragmentSettingsBinding binding;

    private ViewModelActivityMain viewModelActivityMain;

    private View.OnClickListener onClickListenerFAB;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.settings));
        updateFAB();
        loadSettings();
    }

    private void updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_black_24dp);
        viewModelActivityMain.setFABText(R.string.fab_save);
        onClickListenerFAB = (view) -> {
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
                NavController navController = NavHostFragment.findNavController(this);
                if(navController.getCurrentDestination().getId() == R.id.FragmentSettings) {
                    navController.popBackStack();
                }
        };
        viewModelActivityMain.setFabOnClickListener(onClickListenerFAB);
        viewModelActivityMain.showFab(true);
    }

    private int getNSongs() {
        final EditText editTextNSongs = binding.editTextNSongs;
        int nSongs = -1;
        try {
            nSongs = Integer.parseInt(editTextNSongs.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (nSongs < 1) {
            ActivityMain activityMain = (ActivityMain) requireActivity();
            activityMain.showToast(R.string.max_percent_error);
            nSongs = -1;
        }
        return nSongs;
    }

    private int getPercentChangeUp() {
        final EditText editTextPercentChangeUp = binding.editTextPercentChangeUp;
        int percentChangeUp = -1;
        try {
            percentChangeUp = Integer.parseInt(editTextPercentChangeUp.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (percentChangeUp < 1 || percentChangeUp > 100) {
            ActivityMain activityMain = (ActivityMain) requireActivity();
            activityMain.showToast(R.string.percent_change_error);
            percentChangeUp = -1;
        }
        return percentChangeUp;
    }

    private int getPercentChangeDown() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        EditText editTextPercentChangeDown = binding.editTextPercentChangeDown;
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
        ActivityMain activityMain = (ActivityMain) requireActivity();
        double maxPercent = (1.0 / (double) nSongs);
        activityMain.setMaxPercent(maxPercent);
        activityMain.setPercentChangeUp(((double) percentChangeUp) / 100.0);
        activityMain.setPercentChangeDown(((double) percentChangeDown) / 100.0);
    }

    private void loadSettings() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        EditText editTextNSongs = binding.editTextNSongs;
        EditText editTextPercentChangeUp = binding.editTextPercentChangeUp;
        EditText editTextPercentChangeDown = binding.editTextPercentChangeDown;
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
        viewModelActivityMain.setFabOnClickListener(null);
        onClickListenerFAB = null;
        viewModelActivityMain = null;
        binding = null;
    }

}