package com.example.waveplayer.fragments.fragment_playlists;

import android.view.View;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.random_playlist.RandomPlaylist;

public class UndoListenerPlaylistRemoved implements View.OnClickListener {

    private final ActivityMain activityMain;
    private final RecyclerViewAdapterPlaylists recyclerViewAdapter;
    private final RandomPlaylist randomPlaylist;
    private final int position;

    UndoListenerPlaylistRemoved(ActivityMain activityMain,
                                RecyclerViewAdapterPlaylists recyclerViewAdapter,
                                RandomPlaylist randomPlaylist,
                                int position) {
        this.activityMain = activityMain;
        this.recyclerViewAdapter = recyclerViewAdapter;
        this.randomPlaylist = randomPlaylist;
        this.position = position;
    }

    @Override
    public void onClick(View v) {
            activityMain.addPlaylist(position, randomPlaylist);
        recyclerViewAdapter.notifyItemInserted(position);
        activityMain.saveFile();
    }

}