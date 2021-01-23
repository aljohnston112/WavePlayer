package com.example.waveplayer.fragments;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.fragments.fragment_playlists.RecyclerViewAdapterPlaylists;
import com.example.waveplayer.fragments.fragment_playlist.FragmentPlaylist;
import com.example.waveplayer.fragments.fragment_playlists.FragmentPlaylists;
import com.example.waveplayer.fragments.fragment_songs.FragmentSongs;

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
            case FragmentPlaylist.NAME: {
                RecyclerView recyclerViewSongs =
                        activityMain.findViewById(R.id.recycler_view_song_list);
                if (recyclerViewSongs != null) {
                    RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                            (RecyclerViewAdapterSongs) recyclerViewSongs.getAdapter();
                    List<Song> songs = activityMain.getUserPickedPlaylist().getSongs();
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
            case FragmentPlaylists.NAME: {
                RecyclerView recyclerViewPlaylists = activityMain.findViewById(R.id.recycler_view_playlist_list);
                if (recyclerViewPlaylists != null) {
                    RecyclerViewAdapterPlaylists recyclerViewAdapterPlaylists =
                            (RecyclerViewAdapterPlaylists) recyclerViewPlaylists.getAdapter();
                    List<RandomPlaylist> playlists = MediaData.getInstance(activityMain).getPlaylists();
                    List<RandomPlaylist> sifted = new ArrayList<>();
                    if (!newText.equals("")) {
                        for (RandomPlaylist randomPlaylist : playlists) {
                            if (randomPlaylist.getName().toLowerCase().contains(newText.toLowerCase())) {
                                sifted.add(randomPlaylist);
                            }
                        }
                        recyclerViewAdapterPlaylists.updateList(sifted);
                    } else {
                        recyclerViewAdapterPlaylists.updateList(playlists);
                    }
                }
                return true;
            }
        }
        return false;
    }

}