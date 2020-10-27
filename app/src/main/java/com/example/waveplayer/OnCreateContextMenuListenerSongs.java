package com.example.waveplayer;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.Fragment;

import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_PLAYLISTS;

public class OnCreateContextMenuListenerSongs implements View.OnCreateContextMenuListener {

    final Fragment fragment;
    final AudioURI audioURI;

    OnCreateContextMenuListenerSongs(Fragment fragment, AudioURI audioURI){
        this.fragment = fragment;
        this.audioURI = audioURI;
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
        if(activityMain != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(BUNDLE_KEY_IS_SONG, true);
            bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, audioURI);
            bundle.putSerializable(BUNDLE_KEY_PLAYLISTS, activityMain.serviceMain.playlists);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                    new DialogFragmentAddToPlaylist(activityMain);
            dialogFragmentAddToPlaylist.setArguments(bundle);
            dialogFragmentAddToPlaylist.show(fragment.getParentFragmentManager(), fragment.getTag());
        }
    }

    private void contextMenuAddToQueue() {
        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
        if(activityMain != null) {
            if (activityMain.serviceMain.songInProgress()) {
                ((ActivityMain) fragment.getActivity()).serviceMain.addToQueue(audioURI.getUri());
            } else {
                activityMain.serviceMain.addToQueueAndPlay(audioURI);
                activityMain.showSongPane();
                activityMain.updateUI();
            }
        }
    }

}
