package com.example.waveplayer;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

public class FragmentSong extends Fragment {

    public FragmentSong() {
        // Required empty public constructor
    }

    public static FragmentSong newInstance() {
        return new FragmentSong();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.actionBar.setTitle(R.string.now_playing);

        TextView textViewSongName = view.findViewById(R.id.text_view_song_name);
        textViewSongName.setText(activityMain.currentSong.title);
    }

}