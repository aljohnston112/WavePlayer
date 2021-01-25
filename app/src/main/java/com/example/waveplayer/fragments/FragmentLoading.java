package com.example.waveplayer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ActivityMain;

public class FragmentLoading extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_loading, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateMainContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        if(activityMain != null) {
            activityMain.fragmentLoadingStarted();
        }
    }


    private void updateMainContent() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        if(activityMain != null) {
            activityMain.setActionBarTitle(getResources().getString(R.string.loading));
        }
        updateFAB();
    }

    private void updateFAB() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        if(activityMain != null) {
            activityMain.showFab(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}