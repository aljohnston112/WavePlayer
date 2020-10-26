package com.example.waveplayer;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.Fragment;

import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_PLAYLISTS;

public class OnCreateContextMenuListenerPlaylists implements View.OnCreateContextMenuListener {

    final Fragment fragment;
    final RandomPlaylist randomPlaylist;

    OnCreateContextMenuListenerPlaylists(Fragment fragment, RandomPlaylist randomPlaylist) {
        this.fragment = fragment;
        this.randomPlaylist = randomPlaylist;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        final MenuItem itemAddToPlaylist = menu.add(R.string.add_to_playlist);
        itemAddToPlaylist.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                contextMenuAddToPlaylist();
                return true;
            }
        });
        final MenuItem itemAddToQueue = menu.add(R.string.add_to_queue);
        itemAddToQueue.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
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
            bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, randomPlaylist);
            bundle.putSerializable(BUNDLE_KEY_PLAYLISTS, activityMain.serviceMain.playlists);
            bundle.putSerializable(BUNDLE_KEY_IS_SONG, false);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist = new DialogFragmentAddToPlaylist();
            dialogFragmentAddToPlaylist.setArguments(bundle);
            dialogFragmentAddToPlaylist.show(fragment.getParentFragmentManager(), fragment.getTag());
        }
    }

    private void contextMenuAddToQueue() {
        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
        if (activityMain != null) {
            if (activityMain.serviceMain.songInProgress()) {
                for (AudioURI audioURI : randomPlaylist.getProbFun().getProbMap().keySet()) {
                    activityMain.serviceMain.addToQueue(audioURI.getUri());
                }
            } else {
                for (AudioURI audioURI : randomPlaylist.getProbFun().getProbMap().keySet()) {
                    activityMain.serviceMain.addToQueue(audioURI.getUri());
                }
                activityMain.serviceMain.playNextInQueue();
                activityMain.showSongPane();
                activityMain.updateUI();
            }
        }
    }

}
