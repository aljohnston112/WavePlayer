package com.example.waveplayer;

import android.view.LayoutInflater;
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

    private final Fragment fragment;

    public List<RandomPlaylist> randomPlaylists;

    OnCreateContextMenuListenerPlaylists onCreateContextMenuListenerPlaylists;

    View.OnClickListener onClickListenerHandle;

    View.OnClickListener onClickListenerViewHolder;

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
        holder.handle.setOnCreateContextMenuListener(null);
        onCreateContextMenuListenerPlaylists =
                new OnCreateContextMenuListenerPlaylists(fragment, holder.randomPlaylist);
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerPlaylists);
        onClickListenerHandle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.handle.performLongClick();
            }
        };
        holder.handle.setOnClickListener(null);
        holder.handle.setOnClickListener(onClickListenerHandle);
        onClickListenerViewHolder = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
                    if (activityMain != null) {
                        activityMain.serviceMain.userPickedPlaylist = holder.randomPlaylist;
                    }
                    NavHostFragment.findNavController(fragment).navigate(
                            FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist());
                }
            }
        };
        holder.playlistView.setOnClickListener(null);
        holder.playlistView.setOnClickListener(onClickListenerViewHolder);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        onCreateContextMenuListenerPlaylists = null;
        holder.handle.setOnCreateContextMenuListener(null);
        onClickListenerHandle = null;
        holder.handle.setOnClickListener(null);
        onClickListenerViewHolder = null;
        holder.playlistView.setOnClickListener(null);
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
        final ImageView handle;

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