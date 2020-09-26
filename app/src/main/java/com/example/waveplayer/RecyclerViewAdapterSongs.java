package com.example.waveplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link AudioURI}.
 */
public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private final List<AudioURI> audioURIS;

    private final Fragment fragment;

    public static final int MENU_ADD_TO_PLAYLIST_GROUP_ID = 3357908;

    public RecyclerViewAdapterSongs(ArrayList<AudioURI> items, Fragment fragment) {
        audioURIS = items;
        this.fragment = fragment;
    }

    public void addSongs(List<AudioURI> items){
        this.audioURIS.addAll(items);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_songs, parent, false);
        return new ViewHolder(view);
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

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener{

        public final View mView;
        public final TextView textViewSongName;
        public AudioURI audioURI;

        public ViewHolder(View view) {

            super(view);
            mView = view;
            textViewSongName = (TextView) view.findViewById(R.id.text_view_song_name);
            view.setOnCreateContextMenuListener(this);
            view.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    // get position
                    int pos = getAdapterPosition();

                    NavDirections action = null;
                    // check if item still exists
                    if(pos != RecyclerView.NO_POSITION){
                        if(fragment instanceof FragmentSongs) {
                            action = FragmentSongsDirections.actionFragmentSongsToFragmentSong(audioURI.title);
                        } else if(fragment instanceof FragmentPlaylist){
                          action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong(audioURI.title);
                        }
                        if(action != null) {
                            NavHostFragment.findNavController(fragment)
                                    .navigate(action);
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
            contextMenu.add(MENU_ADD_TO_PLAYLIST_GROUP_ID, getAdapterPosition(), 0, "Add to playlist");
            //groupId, itemId, order, title
        }


    }

}