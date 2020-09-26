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

/**
 * {@link RecyclerView.Adapter} that can display a {@link RandomPlaylist}.
 */
public class RecyclerViewAdapterPlaylists extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    private final List<RandomPlaylist> randomPlaylists;

    private final Fragment fragment;

    public static final int MENU_DELETE_PLAYLIST_GROUP_ID = 3357909;
    public static final int MENU_EDIT_PLAYLIST_GROUP_ID = 3357910;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_playlists, parent, false);
        return new ViewHolder(view);
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
        public final View mView;
        public final TextView textViewPlaylistName;
        public RandomPlaylist randomPlaylist;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            textViewPlaylistName = (TextView) view.findViewById(R.id.text_view_playlist_name);
            view.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    // get position
                    int pos = getAdapterPosition();

                    NavDirections action = null;
                    // check if item still exists
                    if(pos != RecyclerView.NO_POSITION){
                       if(fragment instanceof FragmentPlaylists){
                            action = FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist(toString());
                        }
                        if(action != null) {
                            NavHostFragment.findNavController(fragment)
                                    .navigate(action);
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
            contextMenu.add(MENU_DELETE_PLAYLIST_GROUP_ID, getAdapterPosition(), 0, "Add to playlist");
            //groupId, itemId, order, title
        }

    }
}