package com.example.waveplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapterPlaylists extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    private final Fragment fragment;

    public final List<RandomPlaylist> randomPlaylists;

    public RecyclerViewAdapterPlaylists(Fragment fragment, List<RandomPlaylist> items) {
        this.fragment = fragment;
        randomPlaylists = items;
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
    }

    @Override
    public int getItemCount() {
        return randomPlaylists.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View playlistView;
        public final TextView textViewPlaylistName;
        public RandomPlaylist randomPlaylist;

        public ViewHolder(View view) {
            super(view);
            playlistView = view;
            textViewPlaylistName = view.findViewById(R.id.text_view_playlist_name);
            if (randomPlaylist != null) {
                textViewPlaylistName.setText(randomPlaylist.getName());
            }
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