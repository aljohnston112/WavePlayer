package com.example.waveplayer.fragments;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.List;

public class OnQueryTextListenerSearch implements SearchView.OnQueryTextListener {

    private final ActivityMain activityMain;

    private final String constructorFragment;

    public OnQueryTextListenerSearch(ActivityMain activityMain, String constructorFragment) {
        this.activityMain = activityMain;
        this.constructorFragment = constructorFragment;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return onQueryTextChange(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        switch (constructorFragment) {
            case FragmentSongs.NAME: {
                RecyclerView recyclerViewSongs = activityMain.findViewById(R.id.recycler_view_song_list);
                if (recyclerViewSongs != null) {
                    RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                            (RecyclerViewAdapterSongs) recyclerViewSongs.getAdapter();
                    List<Song> songs = activityMain.getAllSongs();
                    List<Song> sifted = new ArrayList<>();
                    if (!newText.equals("")) {
                        for (Song song : songs) {
                            if (song.title.toLowerCase().contains(newText.toLowerCase())) {
                                sifted.add(song);
                            }
                        }
                        recyclerViewAdapterSongs.updateList(sifted);
                    } else {
                        recyclerViewAdapterSongs.updateList(songs);
                    }
                }
                return true;
            }
        }
        return false;
    }

}