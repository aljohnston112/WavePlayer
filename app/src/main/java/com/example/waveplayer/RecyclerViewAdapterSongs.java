package com.example.waveplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    public static final int MENU_ADD_TO_PLAYLIST_GROUP_ID = 3357908;

    private final List<AudioURI> audioURIS;
    private final Fragment fragment;

    public RecyclerViewAdapterSongs(List<AudioURI> items, Fragment fragment) {
        audioURIS = items;
        this.fragment = fragment;
    }

    public void addSongs(List<AudioURI> items) {
        this.audioURIS.addAll(items);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_songs, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.audioURI = audioURIS.get(position);
        holder.textViewSongName.setText(audioURIS.get(position).title);
    }

    @Override
    public int getItemCount() {
        return audioURIS.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        public final View songView;
        public final TextView textViewSongName;
        public AudioURI audioURI;

        public ViewHolder(View view) {

            super(view);
            songView = view;
            textViewSongName = view.findViewById(R.id.text_view_song_name);
            view.setOnCreateContextMenuListener(this);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    NavDirections action = null;
                    if (pos != RecyclerView.NO_POSITION) {
                        ((ActivityMain)fragment.getActivity()).currentSong = audioURI;
                        if (fragment instanceof FragmentSongs) {
                            action = FragmentSongsDirections.actionFragmentSongsToFragmentSong();
                        } else if (fragment instanceof FragmentPlaylist) {
                            action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong();
                        }
                        if (action != null) {
                            NavHostFragment.findNavController(fragment).navigate(action);
                        }
                    }
                }
            });

        }

        @NonNull
        @Override
        public String toString() {
            return audioURI.title;
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            //groupId, itemId, order, title
            contextMenu.add(MENU_ADD_TO_PLAYLIST_GROUP_ID, getAdapterPosition(), 0, "Add to playlist");
        }

    }

}