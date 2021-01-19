package com.example.waveplayer.fragments.fragment_playlists;

import android.view.View;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.random_playlist.RandomPlaylist;

public class UndoListenerPlaylistRemoved implements View.OnClickListener {

    private final ActivityMain activityMain;
    private final RecyclerViewAdapterPlaylists recyclerViewAdapter;
    private final RandomPlaylist randomPlaylist;
    private final int position;
    private final boolean isDirectoryPlaylist;
    private final long uriID;

    UndoListenerPlaylistRemoved(ActivityMain activityMain,
                                RecyclerViewAdapterPlaylists recyclerViewAdapter,
                                RandomPlaylist randomPlaylist,
                                int position, boolean isDirectoryPlaylist, long uriId) {
        this.activityMain = activityMain;
        this.recyclerViewAdapter = recyclerViewAdapter;
        this.randomPlaylist = randomPlaylist;
        this.position = position;
        this.isDirectoryPlaylist = isDirectoryPlaylist;
        this.uriID = uriId;
    }

    @Override
    public void onClick(View v) {
        if (isDirectoryPlaylist) {
            activityMain.addDirectoryPlaylist(uriID, randomPlaylist);
        } else {
            activityMain.addPlaylist(position, randomPlaylist);
        }
        recyclerViewAdapter.notifyItemInserted(position);
        activityMain.saveFile();
    }

}