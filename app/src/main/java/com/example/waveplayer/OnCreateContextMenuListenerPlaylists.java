package com.example.waveplayer;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.Fragment;

public class OnCreateContextMenuListenerPlaylists implements View.OnCreateContextMenuListener {

    final Fragment fragment;
    final RandomPlaylist randomPlaylist;

    OnCreateContextMenuListenerPlaylists(Fragment fragment, RandomPlaylist randomPlaylist){
        this.fragment = fragment;
        this.randomPlaylist = randomPlaylist;
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
            bundle.putSerializable(
                    RecyclerViewAdapterPlaylists.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, randomPlaylist);
            bundle.putSerializable(
                    RecyclerViewAdapterPlaylists.BUNDLE_KEY_PLAYLISTS, activityMain.serviceMain.playlists);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist = new DialogFragmentAddToPlaylist(false);
            dialogFragmentAddToPlaylist.setArguments(bundle);
            dialogFragmentAddToPlaylist.show(fragment.getParentFragmentManager(), fragment.getTag());
        }
    }

    private void contextMenuAddToQueue() {
        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
        if(activityMain != null) {
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
