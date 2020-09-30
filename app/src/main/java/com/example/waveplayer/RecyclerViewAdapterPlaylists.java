package com.example.waveplayer;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapterPlaylists extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    public static final int MENU_DELETE_PLAYLIST_GROUP_ID = 3357909;
    public static final int MENU_EDIT_PLAYLIST_GROUP_ID = 3357910;

    private final List<RandomPlaylist> randomPlaylists;

    private final Fragment fragment;

    public RecyclerViewAdapterPlaylists(List<RandomPlaylist> items, Fragment fragment) {
        randomPlaylists = items;
        this.fragment = fragment;
    }

    public void addPlaylists(List<RandomPlaylist> items){
        this.randomPlaylists.addAll(items);
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_playlists, parent, false));
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

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        public final View playlistView;
        public final TextView textViewPlaylistName;
        public RandomPlaylist randomPlaylist;

        public ViewHolder(View view) {
            super(view);
            playlistView = view;
            textViewPlaylistName = view.findViewById(R.id.text_view_playlist_name);
            if(randomPlaylist!= null){
            textViewPlaylistName.setText(randomPlaylist.getName());
            }
            view.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    NavDirections action = null;
                    if(pos != RecyclerView.NO_POSITION){
                       if(fragment instanceof FragmentPlaylists){
                            action = FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist();
                        }
                        if(action != null) {
                            ((ActivityMain)fragment.getActivity()).currentPlaylist = randomPlaylist;
                            NavHostFragment.findNavController(fragment).navigate(action);
                        }
                    }
                }
            });
        }

        @Override
        @NonNull
        public String toString() {
            return super.toString() + " '" + textViewPlaylistName.getText() + "'";
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            //groupId, itemId, order, title
            contextMenu.add(MENU_DELETE_PLAYLIST_GROUP_ID, getAdapterPosition(), 0, "Add to playlist");
        }

    }
}