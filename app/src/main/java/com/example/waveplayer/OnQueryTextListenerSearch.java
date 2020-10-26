package com.example.waveplayer;

import android.widget.SearchView;

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
        if(activityMain.searchInProgress) {
            switch (constructorFragment) {
                case FragmentSongs.NAME: {
                    RecyclerView recyclerViewSongs = activityMain.findViewById(R.id.recycler_view_song_list);
                    if (recyclerViewSongs != null) {
                        RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                                (RecyclerViewAdapterSongs) recyclerViewSongs.getAdapter();
                        List<AudioURI> audioURIS = new ArrayList<>(
                                activityMain.serviceMain.masterPlaylist.getProbFun().getProbMap().keySet());
                        List<AudioURI> sifted = new ArrayList<>();
                        if (!newText.equals("")) {
                            for (AudioURI audioURI : audioURIS) {
                                if (audioURI.title.toLowerCase().contains(newText.toLowerCase())) {
                                    sifted.add(audioURI);
                                }
                            }
                            recyclerViewAdapterSongs.updateList(sifted);
                        } else {
                            recyclerViewAdapterSongs.updateList(audioURIS);
                        }
                    }
                    return true;
                }
                case FragmentPlaylist.NAME: {
                    RecyclerView recyclerViewSongs = activityMain.findViewById(R.id.recycler_view_song_list);
                    if (recyclerViewSongs != null) {
                        RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                                (RecyclerViewAdapterSongs) recyclerViewSongs.getAdapter();
                        List<AudioURI> audioURIS = new ArrayList<>(
                                activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap().keySet());
                        List<AudioURI> sifted = new ArrayList<>();
                        if (!newText.equals("")) {
                            for (AudioURI audioURI : audioURIS) {
                                if (audioURI.title.toLowerCase().contains(newText.toLowerCase())) {
                                    sifted.add(audioURI);
                                }
                            }
                            recyclerViewAdapterSongs.updateList(sifted);
                        } else {
                            recyclerViewAdapterSongs.updateList(audioURIS);
                        }
                    }
                    return true;
                }
                case FragmentPlaylists.NAME: {
                    RecyclerView recyclerViewPlaylists = activityMain.findViewById(R.id.recycler_view_playlist_list);
                    if (recyclerViewPlaylists != null) {
                        RecyclerViewAdapterPlaylists recyclerViewAdapterPlaylists =
                                (RecyclerViewAdapterPlaylists) recyclerViewPlaylists.getAdapter();
                        List<RandomPlaylist> playlists = activityMain.serviceMain.playlists;
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
        }
        return false;
    }
}