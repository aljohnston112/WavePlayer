package com.example.waveplayer;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class OnQueryTextListenerSearch implements SearchView.OnQueryTextListener {

    final ActivityMain activityMain;

    final String constructorFragment;

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
        // if(activityMain.searchInProgress) {
        switch (constructorFragment) {
            case FragmentSongs.NAME: {
                RecyclerView recyclerViewSongs = activityMain.findViewById(R.id.recycler_view_song_list);
                if (recyclerViewSongs != null) {
                    RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                            (RecyclerViewAdapterSongs) recyclerViewSongs.getAdapter();
                    List<AudioUri> audioUris = activityMain.getAllSongs();
                    List<AudioUri> sifted = new ArrayList<>();
                    if (!newText.equals("")) {
                        for (AudioUri audioURI : audioUris) {
                            if (audioURI.title.toLowerCase().contains(newText.toLowerCase())) {
                                sifted.add(audioURI);
                            }
                        }
                        recyclerViewAdapterSongs.updateList(sifted);
                    } else {
                        recyclerViewAdapterSongs.updateList(audioUris);
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
                    List<AudioUri> audioUris = activityMain.getUserPickedPlaylist().getAudioUris();
                    List<AudioUri> sifted = new ArrayList<>();
                    if (!newText.equals("")) {
                        for (AudioUri audioURI : audioUris) {
                            if (audioURI.title.toLowerCase().contains(newText.toLowerCase())) {
                                sifted.add(audioURI);
                            }
                        }
                        recyclerViewAdapterSongs.updateList(sifted);
                    } else {
                        recyclerViewAdapterSongs.updateList(audioUris);
                    }
                }
                return true;
            }
            case FragmentPlaylists.NAME: {
                RecyclerView recyclerViewPlaylists = activityMain.findViewById(R.id.recycler_view_playlist_list);
                if (recyclerViewPlaylists != null) {
                    RecyclerViewAdapterPlaylists recyclerViewAdapterPlaylists =
                            (RecyclerViewAdapterPlaylists) recyclerViewPlaylists.getAdapter();
                    List<RandomPlaylist> playlists = activityMain.getPlaylists();
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
        // }
        return false;
    }
}