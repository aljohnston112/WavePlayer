package com.example.waveplayer;

import android.view.View;

public class UndoListenerPlaylistRemoved implements View.OnClickListener {

    ActivityMain activityMain;
    RecyclerViewAdapterPlaylists recyclerViewAdapter;
    RandomPlaylist randomPlaylist;
    int position;
    boolean isDirectoryPlaylist;
    long uriID;

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
