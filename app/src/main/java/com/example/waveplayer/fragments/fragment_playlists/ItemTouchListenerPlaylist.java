package com.example.waveplayer.fragments.fragment_playlists;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

public class ItemTouchListenerPlaylist extends ItemTouchHelper.Callback {

    private final ActivityMain activityMain;

    public ItemTouchListenerPlaylist(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        RecyclerViewAdapterPlaylists recyclerViewAdapter =
                (RecyclerViewAdapterPlaylists) recyclerView.getAdapter();
        if (recyclerViewAdapter != null) {
            Collections.swap(recyclerViewAdapter.randomPlaylists,
                    viewHolder.getAdapterPosition(), target.getAdapterPosition());
            Collections.swap(activityMain.getPlaylists(),
                    viewHolder.getAdapterPosition(), target.getAdapterPosition());
            recyclerViewAdapter.notifyItemMoved(
                    viewHolder.getAdapterPosition(), target.getAdapterPosition());
            activityMain.saveFile();
        }
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        RecyclerView recyclerView = activityMain.findViewById(R.id.recycler_view_playlist_list);
        RecyclerViewAdapterPlaylists recyclerViewAdapter =
                (RecyclerViewAdapterPlaylists) recyclerView.getAdapter();
        if (recyclerViewAdapter != null) {
            int position = viewHolder.getAdapterPosition();
            List<RandomPlaylist> randomPlaylists = activityMain.getPlaylists();
            RandomPlaylist randomPlaylist = randomPlaylists.get(position);
            activityMain.removePlaylist(randomPlaylist);
            recyclerViewAdapter.notifyItemRemoved(position);
            activityMain.saveFile();
            Snackbar snackbar = Snackbar.make(
                    activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                    R.string.playlist_deleted, BaseTransientBottomBar.LENGTH_LONG);
            UndoListenerPlaylistRemoved undoListenerPlaylistRemoved = new UndoListenerPlaylistRemoved(
                    activityMain, recyclerViewAdapter, randomPlaylist, position);
            snackbar.setAction(R.string.undo, undoListenerPlaylistRemoved);
            snackbar.show();
        }
    }

}