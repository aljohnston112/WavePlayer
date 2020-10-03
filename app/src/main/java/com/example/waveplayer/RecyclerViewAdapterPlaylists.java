package com.example.waveplayer;

import android.view.ContextMenu;
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

    public static final int MENU_DELETE_PLAYLIST_GROUP_ID = 3357909;

    private final Fragment fragment;

    private final List<RandomPlaylist> randomPlaylists;

    public RecyclerViewAdapterPlaylists(Fragment fragment, List<RandomPlaylist> items) {
        this.fragment = fragment;
        randomPlaylists = items;
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
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION){
                            ActivityMain activityMain = ((ActivityMain)fragment.getActivity());
                            if(activityMain != null) {
                                activityMain.userPickedPlaylist = randomPlaylist;
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

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                        ContextMenu.ContextMenuInfo contextMenuInfo) {
            //groupId, itemId, order, title
            contextMenu.add(MENU_DELETE_PLAYLIST_GROUP_ID, getAdapterPosition(), 0, "Delete playlist");
        }

    }
}