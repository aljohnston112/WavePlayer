package com.example.waveplayer.fragments.fragment_playlists;

import android.view.View;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.MediaData;
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
        MediaData.getInstance().addPlaylist(position, randomPlaylist);
        recyclerViewAdapter.notifyItemInserted(position);
        activityMain.saveFile();
    }

}