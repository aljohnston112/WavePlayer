package com.example.waveplayer.fragments;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.DialogFragmentAddToPlaylist;
import com.example.waveplayer.R;
import com.example.waveplayer.Song;

import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_PLAYLISTS;

public class OnCreateContextMenuListenerSongs implements View.OnCreateContextMenuListener {

    private final Fragment fragment;
    private final Song song;

    OnCreateContextMenuListenerSongs(Fragment fragment, Song song) {
        this.fragment = fragment;
        this.song = song;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        final MenuItem item = menu.add(R.string.add_to_playlist);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                contextMenuAddToPlaylist();
                return true;
            }
        });
        final MenuItem anotherItem = menu.add(R.string.add_to_queue);
        anotherItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                contextMenuAddToQueue();
                return true;
            }
        });
    }

    private void contextMenuAddToPlaylist() {
        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
        if (activityMain != null) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song);
            bundle.putSerializable(BUNDLE_KEY_PLAYLISTS, activityMain.getPlaylists());
            bundle.putBoolean(BUNDLE_KEY_IS_SONG, true);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                    new DialogFragmentAddToPlaylist();
            dialogFragmentAddToPlaylist.setArguments(bundle);
            dialogFragmentAddToPlaylist.show(fragment.getParentFragmentManager(), fragment.getTag());
        }
    }

    private void contextMenuAddToQueue() {
        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
        if (activityMain != null) {
            if (activityMain.songInProgress()) {
                ((ActivityMain) fragment.getActivity()).addToQueue(song.id);
            } else {
                activityMain.showSongPane();
                activityMain.addToQueueAndPlay(song.id);
            }
        }
    }

}