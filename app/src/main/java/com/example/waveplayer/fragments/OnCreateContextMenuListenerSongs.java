package com.example.waveplayer.fragments;

import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.Song;

public class OnCreateContextMenuListenerSongs implements View.OnCreateContextMenuListener {

    private final RecyclerViewAdapterSongs.OnCreateContextMenuListenerSongsCallback
            onCreateContextMenuListenerSongsCallback;

    private final Song song;

    OnCreateContextMenuListenerSongs(RecyclerViewAdapterSongs.OnCreateContextMenuListenerSongsCallback
                                             onCreateContextMenuListenerSongsCallback, Song song) {
        this.onCreateContextMenuListenerSongsCallback = onCreateContextMenuListenerSongsCallback;
        this.song = song;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        final MenuItem item = menu.add(R.string.add_to_playlist);
        item.setOnMenuItemClickListener(
                menuItem -> onCreateContextMenuListenerSongsCallback.onMenuItemClickAddToPlaylist(song));

        final MenuItem anotherItem = menu.add(R.string.add_to_queue);
        anotherItem.setOnMenuItemClickListener(
                menuItem2 -> onCreateContextMenuListenerSongsCallback.onMenuItemClickAddToQueue(song));
    }

}