package com.example.waveplayer.fragments.fragment_playlist;

import android.view.View;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.fragments.RecyclerViewAdapterSongs;

import java.util.ArrayList;

public class UndoListenerSongRemoved implements View.OnClickListener {

    private final ActivityMain activityMain;

    private final RandomPlaylist userPickedPlaylist;

    private final RecyclerViewAdapterSongs recyclerViewAdapter;

    private final Song song;

    private final double probability;

    private final int position;

    UndoListenerSongRemoved(ActivityMain activityMain, RandomPlaylist userPickedPlaylist,
                             RecyclerViewAdapterSongs recyclerViewAdapter, Song song,
                            double probability, int position) {
        this.activityMain = activityMain;
        this.userPickedPlaylist = userPickedPlaylist;
        this.recyclerViewAdapter = recyclerViewAdapter;
        this.song = song;
        this.probability = probability;
        this.position = position;
    }

    @Override
    public void onClick(View v) {
        userPickedPlaylist.add(song, probability);
        switchSongPosition(userPickedPlaylist,
                userPickedPlaylist.size() - 1, position);
        recyclerViewAdapter.updateList(new ArrayList<>(userPickedPlaylist.getSongs()));
        recyclerViewAdapter.notifyDataSetChanged();
        activityMain.saveFile();
    }

    private void switchSongPosition(RandomPlaylist userPickedPlaylist, int oldPosition, int newPosition) {
       userPickedPlaylist.switchSongPositions(oldPosition, newPosition);
    }

}