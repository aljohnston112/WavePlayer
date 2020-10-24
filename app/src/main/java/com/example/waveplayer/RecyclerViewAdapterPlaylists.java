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

    public static final String ADD_TO_PLAYLIST_PLAYLIST = "ADD_TO_PLAYLIST_PLAYLIST";
    public static final String PLAYLISTS = "PLAYLISTS";

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
            final ImageView handle = view.findViewById(R.id.playlist_handle);
            handle.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
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
            });
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

        private void contextMenuAddToPlaylist() {
            Bundle bundle = new Bundle();
            bundle.putSerializable(ADD_TO_PLAYLIST_PLAYLIST, randomPlaylist);
            bundle.putSerializable(PLAYLISTS, ((ActivityMain) fragment.getActivity()).serviceMain.playlists);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist = new DialogFragmentAddToPlaylist();
            dialogFragmentAddToPlaylist.setArguments(bundle);
            dialogFragmentAddToPlaylist.show(fragment.getParentFragmentManager(), fragment.getTag());
        }

        private void contextMenuAddToQueue() {
            if(((ActivityMain) fragment.getActivity()).serviceMain.songInProgress()) {
                for(AudioURI audioURI : randomPlaylist.getProbFun().getProbMap().keySet()) {
                    ((ActivityMain) fragment.getActivity()).serviceMain.addToQueue(audioURI.getUri());
                }
            } else{
                for(AudioURI audioURI : randomPlaylist.getProbFun().getProbMap().keySet()) {
                    ((ActivityMain) fragment.getActivity()).serviceMain.addToQueue(audioURI.getUri());
                }
                ((ActivityMain) fragment.getActivity()).serviceMain.playNextInQueue();
                ((ActivityMain) fragment.getActivity()).showSongPane();
                ((ActivityMain) fragment.getActivity()).updateSongPaneUI();
            }
        }

        @Override
        @NonNull
        public String toString() {
            return randomPlaylist.getName();
        }

    }

}