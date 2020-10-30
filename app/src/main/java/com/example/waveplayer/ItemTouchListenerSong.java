package com.example.waveplayer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;

class ItemTouchListenerSong extends ItemTouchHelper.Callback {

    private final ActivityMain activityMain;

    private final RecyclerViewAdapterSongs recyclerViewAdapterSongsList;

    private final RandomPlaylist userPickedPlaylist;

    private UndoListenerSongRemoved undoListenerSongRemoved;

    public ItemTouchListenerSong(ActivityMain activityMain,
                                 RecyclerViewAdapterSongs recyclerViewAdapterSongsList,
                                 RandomPlaylist userPickedPlaylist) {
        this.activityMain = activityMain;
        this.recyclerViewAdapterSongsList = recyclerViewAdapterSongsList;
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
        AudioUri audioURI = recyclerViewAdapterSongsList.getAudioUris().get(position);
        double probability = userPickedPlaylist.getProbability(audioURI);
        if (userPickedPlaylist.size() == 1) {
            activityMain.remove(userPickedPlaylist);
            activityMain.setUserPickedPlaylist(null);
        } else {
            userPickedPlaylist.remove(audioURI);
        }
        recyclerViewAdapterSongsList.getAudioUris().remove(position);
        recyclerViewAdapterSongsList.notifyItemRemoved(position);
        activityMain.saveFile();
        Snackbar snackbar = Snackbar.make(
                activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                R.string.song_removed, BaseTransientBottomBar.LENGTH_LONG);
        undoListenerSongRemoved = new UndoListenerSongRemoved(userPickedPlaylist,
                recyclerViewAdapterSongsList,
                audioURI, probability, position);
        snackbar.setAction(R.string.undo, undoListenerSongRemoved);
        snackbar.show();
    }

    private void swapSongPositions(int oldPosition, int newPosition) {
        userPickedPlaylist.swapSongPositions(oldPosition, newPosition);

    }

}