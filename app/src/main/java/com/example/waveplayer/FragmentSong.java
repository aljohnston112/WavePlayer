package com.example.waveplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentSong#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentSong extends Fragment {

    public FragmentSong() {
        // Required empty public constructor
    }

    public static FragmentSong newInstance() {
        FragmentSong fragment = new FragmentSong();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String songTitle = null;
        if(getArguments() != null) {
            songTitle = FragmentSongArgs.fromBundle(getArguments()).getStringSongTitle();
        }
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_song, container, false);
        TextView textViewSongName = view.findViewById(R.id.text_view_song_name);
        if(songTitle!=null) {
            textViewSongName.setText(songTitle);
        }
        return view;
    }

}