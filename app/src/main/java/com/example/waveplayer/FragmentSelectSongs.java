package com.example.waveplayer;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A fragment representing a list of Items.
 */
public class FragmentSelectSongs extends Fragment {

    RecyclerViewAdapterSelectSongs recyclerViewAdapterSelectSongs;

    RecyclerView recyclerView;

    ArrayList<AudioURI> songs;
    ArrayList<AudioURI> selectedSongs;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FragmentSelectSongs() {
    }

    @SuppressWarnings("unused")
    public static FragmentSelectSongs newInstance(int columnCount) {
        FragmentSelectSongs fragment = new FragmentSelectSongs();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(getArguments() != null) {
            selectedSongs = FragmentSelectSongsArgs.fromBundle(getArguments()).getListSelectedSongs();
            songs = FragmentSelectSongsArgs.fromBundle(getArguments()).getListSongs();
        }
        recyclerView = (RecyclerView)
                inflater.inflate(R.layout.fragment_song_list, container, false);
        FloatingActionButton fab = Objects.requireNonNull(getActivity()).findViewById(R.id.fab);
        fab.setBackground(getResources().getDrawable(R.drawable.ic_check_white_24dp));
        fab.setOnClickListener(null);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO
            }
        });
        // Set the adapter
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerViewAdapterSelectSongs = new RecyclerViewAdapterSelectSongs(songs, selectedSongs, this);
        recyclerView.setAdapter(recyclerViewAdapterSelectSongs);
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.select_songs);
    }

}