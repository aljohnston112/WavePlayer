package com.example.waveplayer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 */
public class FragmentPlaylists extends Fragment {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FragmentPlaylists() {
    }

    @SuppressWarnings("unused")
    public static FragmentPlaylists newInstance(int columnCount) {
        FragmentPlaylists fragment = new FragmentPlaylists();
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
            arrayList = FragmentPlaylistsArgs.fromBundle(getArguments()).getListPlaylists();
        }
        RecyclerView recyclerView = (RecyclerView)
                inflater.inflate(R.layout.fragment_playlist_list, container, false);
        Context context = recyclerView.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new RecyclerViewAdapterPlaylists(arrayList, this));
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}