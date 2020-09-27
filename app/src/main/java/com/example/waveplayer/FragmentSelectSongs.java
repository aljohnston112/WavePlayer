package com.example.waveplayer;

import android.app.Activity;
import android.os.Binder;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FragmentSelectSongs extends Fragment {

    public FragmentSelectSongs() {
    }

    @SuppressWarnings("unused")
    public static FragmentSelectSongs newInstance(int columnCount) {
        return new FragmentSelectSongs();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.actionBar.setTitle(R.string.select_songs);

        activityMain.fab.setBackground(getResources().getDrawable(R.drawable.ic_check_white_24dp));
        activityMain.fab.setOnClickListener(null);
        activityMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.userPickedSongs = new ArrayList<>();
                int size = activityMain.songs.size();
                for(int i = 0; i < size; i++) {
                    if (activityMain.songs.get(i).isChecked()){
                        activityMain.userPickedSongs.add(activityMain.songs.get(i));
                        activityMain.songs.get(i).setChecked(false);
                    }
                }
                NavController navController = NavHostFragment.findNavController(FragmentSelectSongs.this);
                navController.popBackStack();
            }
        });

        RecyclerView recyclerView = activityMain.findViewById(R.id.recycler_view_song_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        RecyclerViewAdapterSelectSongs recyclerViewAdapterSelectSongs = new RecyclerViewAdapterSelectSongs(
                activityMain.songs, activityMain.userPickedSongs, this);
        recyclerView.setAdapter(recyclerViewAdapterSelectSongs);
    }

}