package com.example.waveplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FragmentSongs extends Fragment {

    ActivityMain activityMain;

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
        activityMain = ((ActivityMain) getActivity());
        updateMainContent();
        setUpRecyclerView();
    }

    private void updateMainContent() {
        activityMain.setActionBarTitle(getResources().getString(R.string.songs));
        activityMain.showFab(false);
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerViewSongs = activityMain.findViewById(R.id.recycler_view_song_list);
        RecyclerViewAdapterSongs recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(
                this, activityMain.songs);
        recyclerViewSongs.setLayoutManager(new LinearLayoutManager(recyclerViewSongs.getContext()));
        recyclerViewSongs.setAdapter(recyclerViewAdapterSongs);
    }

}