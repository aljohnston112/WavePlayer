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
import com.example.waveplayer.databinding.FragmentSettingsBinding;
import com.example.waveplayer.media_controller.MediaData;

public class FragmentSettings extends Fragment {

    private FragmentSettingsBinding mBinding;

    private OnClickListenerFABFragmentSettings mOnClickListenerFABFragmentSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentSettingsBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
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
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.setActionBarTitle(getResources().getString(R.string.settings));
        updateFAB();
    }

    private void updateFAB() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        final EditText editTextNSongs = activityMain.findViewById(R.id.edit_text_n_songs);
        final EditText editTextPercentChangeUp = activityMain.findViewById(R.id.edit_text_percent_change_up);
        final EditText editTextPercentChangeDown = activityMain.findViewById(R.id.edit_text_percent_change_down);
        activityMain.setFabImage(R.drawable.ic_check_black_24dp);
        activityMain.setFABText(R.string.fab_save);
        mOnClickListenerFABFragmentSettings = new OnClickListenerFABFragmentSettings(
                this, editTextNSongs, editTextPercentChangeDown, editTextPercentChangeUp);
        activityMain.setFabOnClickListener(mOnClickListenerFABFragmentSettings);
        activityMain.showFab(true);
    }

    private void loadSettings() {
        EditText editTextNSongs = mBinding.editTextNSongs;
        EditText editTextPercentChangeUp = mBinding.editTextPercentChangeUp;
        EditText editTextPercentChangeDown = mBinding.editTextPercentChangeDown;
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
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setFabOnClickListener(null);
        mOnClickListenerFABFragmentSettings = null;
        mBinding = null;
    }

}