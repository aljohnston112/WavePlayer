package com.example.waveplayer.fragments;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist;
import com.example.waveplayer.fragments.FragmentPlaylistsDirections;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.Song;

import java.util.List;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;

public class RecyclerViewAdapterPlaylists
        extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    public interface ListenerCallbackPlaylists {
        void onClickViewHolder(RandomPlaylist randomPlaylist);
        boolean onMenuItemClickAddToPlaylist(RandomPlaylist randomPlaylist);
        boolean onMenuItemClickAddToQueue(RandomPlaylist randomPlaylist);
    }

    private ListenerCallbackPlaylists listenerCallbackPlaylists;

    private List<RandomPlaylist> randomPlaylists;

    private View.OnCreateContextMenuListener onCreateContextMenuListenerPlaylists;
    private MenuItem.OnMenuItemClickListener onMenuItemClickListenerAddToPlaylist;
    private MenuItem.OnMenuItemClickListener onMenuItemClickListenerAddToQueue;

    private View.OnClickListener onClickListenerHandle;
    private View.OnClickListener onClickListenerViewHolder;

    public RecyclerViewAdapterPlaylists(ListenerCallbackPlaylists listenerCallbackPlaylists,
                                        List<RandomPlaylist> randomPlaylists) {
        this.listenerCallbackPlaylists = listenerCallbackPlaylists;
        this.randomPlaylists = randomPlaylists;
    }

    public void updateList(List<RandomPlaylist> randomPlaylists) {
        this.randomPlaylists = randomPlaylists;
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.randomPlaylist = randomPlaylists.get(position);
        holder.textViewPlaylistName.setText(randomPlaylists.get(position).getName());

        onMenuItemClickListenerAddToPlaylist = menuItem ->
                listenerCallbackPlaylists.onMenuItemClickAddToPlaylist(holder.randomPlaylist);
        onMenuItemClickListenerAddToQueue = menuItem2 ->
                listenerCallbackPlaylists.onMenuItemClickAddToQueue(holder.randomPlaylist);
        onCreateContextMenuListenerPlaylists =
                (menu, v, menuInfo) -> {
                    MenuItem itemAddToPlaylist = menu.add(R.string.add_to_playlist);
                    itemAddToPlaylist.setOnMenuItemClickListener(onMenuItemClickListenerAddToPlaylist);
                    MenuItem itemAddToQueue = menu.add(R.string.add_to_queue);
                    itemAddToQueue.setOnMenuItemClickListener(onMenuItemClickListenerAddToQueue);
                };
        holder.handle.setOnCreateContextMenuListener(null);
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerPlaylists);

        onClickListenerHandle = v -> holder.handle.performLongClick();
        holder.handle.setOnClickListener(null);
        holder.handle.setOnClickListener(onClickListenerHandle);

        onClickListenerViewHolder = v -> {
            if (position != RecyclerView.NO_POSITION) {
                listenerCallbackPlaylists.onClickViewHolder(holder.randomPlaylist);
            }
        };
        holder.playlistView.setOnClickListener(null);
        holder.playlistView.setOnClickListener(onClickListenerViewHolder);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.handle.setOnCreateContextMenuListener(null);
        onCreateContextMenuListenerPlaylists = null;
        onMenuItemClickListenerAddToPlaylist = null;
        onMenuItemClickListenerAddToQueue = null;
        holder.handle.setOnClickListener(null);
        onClickListenerHandle = null;
        holder.playlistView.setOnClickListener(null);
        onClickListenerViewHolder = null;
        holder.randomPlaylist = null;
        listenerCallbackPlaylists = null;
    }

    @Override
    public int getItemCount() {
        return randomPlaylists.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final public View playlistView;
        final public TextView textViewPlaylistName;
        final ImageView handle;
        public RandomPlaylist randomPlaylist;

        public ViewHolder(View view) {
            super(view);
            playlistView = view;
            textViewPlaylistName = view.findViewById(R.id.text_view_playlist_name);
            if (randomPlaylist != null) {
                textViewPlaylistName.setText(randomPlaylist.getName());
            }
            handle = view.findViewById(R.id.playlist_handle);
        }

        @Override
        @NonNull
        public String toString() {
            return randomPlaylist.getName();
        }

    }

}