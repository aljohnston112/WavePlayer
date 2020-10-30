package com.example.waveplayer;

import android.view.View;

import java.util.ArrayList;

public class UndoListenerSongRemoved implements View.OnClickListener {

    ActivityMain activityMain;

    RandomPlaylist userPickedPlaylist;

    RecyclerViewAdapterSongs recyclerViewAdapter;

    AudioUri audioURI;

    double probability;

    int position;

    UndoListenerSongRemoved( RandomPlaylist userPickedPlaylist,
                             RecyclerViewAdapterSongs recyclerViewAdapter, AudioUri audioURI,
                            double probability, int position) {
        this.userPickedPlaylist = userPickedPlaylist;
        this.recyclerViewAdapter = recyclerViewAdapter;
        this.audioURI = audioURI;
        this.probability = probability;
        this.position = position;
    }

    @Override
    public void onClick(View v) {
        userPickedPlaylist.add(audioURI, probability);
        switchSongPosition(userPickedPlaylist,
                userPickedPlaylist.size() - 1, position);
        recyclerViewAdapter.updateList(new ArrayList<>(userPickedPlaylist.getAudioUris()));
        recyclerViewAdapter.notifyDataSetChanged();
        activityMain.saveFile();
    }

    private void switchSongPosition(RandomPlaylist userPickedPlaylist, int oldPosition, int newPosition) {
       userPickedPlaylist.switchSongPositions(oldPosition, newPosition);
    }

}
