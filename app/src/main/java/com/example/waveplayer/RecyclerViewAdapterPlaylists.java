package com.example.waveplayer;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapterPlaylists extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST = "ADD_TO_PLAYLIST_PLAYLIST";
    public static final String BUNDLE_KEY_PLAYLISTS = "PLAYLISTS";

    private final Fragment fragment;

    public List<RandomPlaylist> randomPlaylists;

    public RecyclerViewAdapterPlaylists(Fragment fragment, List<RandomPlaylist> items) {
        this.fragment = fragment;
        randomPlaylists = items;
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
        holder.onCreateContextMenuListenerPlaylists = null;
        holder.onCreateContextMenuListenerPlaylists =
                new OnCreateContextMenuListenerPlaylists(fragment, holder.randomPlaylist);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.onCreateContextMenuListenerPlaylists = null;
        holder.playlistView = null;
        holder.textViewPlaylistName = null;
        holder.randomPlaylist = null;
    }

    @Override
    public int getItemCount() {
        return randomPlaylists.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public View playlistView;
        public TextView textViewPlaylistName;
        public RandomPlaylist randomPlaylist;

        OnCreateContextMenuListenerPlaylists onCreateContextMenuListenerPlaylists;

        public ViewHolder(View view) {
            super(view);
            playlistView = view;
            textViewPlaylistName = view.findViewById(R.id.text_view_playlist_name);
            if (randomPlaylist != null) {
                textViewPlaylistName.setText(randomPlaylist.getName());
            }
            final ImageView handle = view.findViewById(R.id.playlist_handle);
            handle.setOnCreateContextMenuListener(onCreateContextMenuListenerPlaylists);
            handle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handle.performLongClick();
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
                        if (activityMain != null) {
                            activityMain.serviceMain.userPickedPlaylist = randomPlaylist;
                        }
                        NavHostFragment.findNavController(fragment).navigate(
                                FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist());
                    }
                }
            });
        }

        @Override
        @NonNull
        public String toString() {
            return randomPlaylist.getName();
        }

    }

}