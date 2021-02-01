package com.example.waveplayer.fragments.fragment_pane_song;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;

public class OnClickListenerSongPane implements View.OnClickListener {

    // TODO does not appear to work (resized to different dimensions)

    private final OnClickCallback onClickCallback;

    public interface OnClickCallback {
        void onClick(View v);
    }

    public OnClickListenerSongPane(OnClickCallback onClickCallback) {
        this.onClickCallback = onClickCallback;
    }

    @Override
    public void onClick(View v) {
        onClickCallback.onClick(v);
    }

}