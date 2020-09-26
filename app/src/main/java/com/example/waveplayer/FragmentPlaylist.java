package com.example.waveplayer;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentPlaylist#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentPlaylist extends Fragment {

    public FragmentPlaylist() {
        // Required empty public constructor
    }

    public static FragmentPlaylist newInstance() {
        FragmentPlaylist fragment = new FragmentPlaylist();
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
        ArrayList arrayList = new ArrayList<AudioURI>();
        if(getArguments() != null) {
            // TODO
            //arrayList = FragmentSongsArgs.fromBundle(getArguments()).getListSongs();
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