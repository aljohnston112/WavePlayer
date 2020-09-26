package com.example.waveplayer;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 */
public class FragmentSongs extends Fragment {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FragmentSongs() {
    }

    @SuppressWarnings("unused")
    public static FragmentSongs newInstance(int columnCount) {
        FragmentSongs fragment = new FragmentSongs();
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
        ArrayList arrayList = new ArrayList<WaveURI>();
        if(getArguments() != null) {
            arrayList = FragmentSongsArgs.fromBundle(getArguments()).getListSongs();
        }

        RecyclerView recyclerView = (RecyclerView)
                inflater.inflate(R.layout.fragment_song_list, container, false);
        // Set the adapter
            Context context = recyclerView.getContext();
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new RecyclerViewAdapterSongs(arrayList, this));
        return recyclerView;
    }

}