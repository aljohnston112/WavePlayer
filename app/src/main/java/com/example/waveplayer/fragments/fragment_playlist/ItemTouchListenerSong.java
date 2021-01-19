package com.example.waveplayer.fragments.fragment_playlist;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.fragments.RecyclerViewAdapterSongs;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;

class ItemTouchListenerSong extends ItemTouchHelper.Callback {

    private final ActivityMain activityMain;

    private final RandomPlaylist userPickedPlaylist;

    public ItemTouchListenerSong(ActivityMain activityMain, RandomPlaylist userPickedPlaylist) {
        this.activityMain = activityMain;
        this.userPickedPlaylist = userPickedPlaylist;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        RecyclerViewAdapterSongs recyclerViewAdapterSongsList =
                (RecyclerViewAdapterSongs) recyclerView.getAdapter();
        if (recyclerViewAdapterSongsList != null) {
            Collections.swap(recyclerViewAdapterSongsList.getAudioUris(),
                    viewHolder.getAdapterPosition(), target.getAdapterPosition());
            swapSongPositions(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            recyclerViewAdapterSongsList.notifyItemMoved(
                    viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        RecyclerView recyclerView = activityMain.findViewById(R.id.recycler_view_song_list);
        RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                (RecyclerViewAdapterSongs) recyclerView.getAdapter();
        AudioUri audioURI = recyclerViewAdapterSongs.getAudioUris().get(position);
        double probability = userPickedPlaylist.getProbability(audioURI);
        if (userPickedPlaylist.size() == 1) {
            activityMain.removePlaylist(userPickedPlaylist);
            activityMain.setUserPickedPlaylist(null);
        } else {
            userPickedPlaylist.remove(audioURI);
        }
        recyclerViewAdapterSongs.getAudioUris().remove(position);
        recyclerViewAdapterSongs.notifyItemRemoved(position);
        activityMain.saveFile();
        Snackbar snackbar = Snackbar.make(
                activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                R.string.song_removed, BaseTransientBottomBar.LENGTH_LONG);
        UndoListenerSongRemoved undoListenerSongRemoved = new UndoListenerSongRemoved(
                activityMain, userPickedPlaylist,
                recyclerViewAdapterSongs,
                audioURI, probability, position);
        snackbar.setAction(R.string.undo, undoListenerSongRemoved);
        snackbar.show();
    }

    private void swapSongPositions(int oldPosition, int newPosition) {
        userPickedPlaylist.swapSongPositions(oldPosition, newPosition);
        activityMain.saveFile();
    }

}