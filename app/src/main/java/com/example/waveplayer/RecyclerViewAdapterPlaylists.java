package com.example.waveplayer;

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
 * {@link RecyclerView.Adapter} that can display a {@link AudioURI}.
 */
public class RecyclerViewAdapterPlaylists extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    private final List<AudioURI> audioURISES;

    private final Fragment fragment;

    public RecyclerViewAdapterPlaylists(List<AudioURI> items, Fragment fragment) {
        audioURISES = items;
        this.fragment = fragment;
    }

    public void addPlaylists(List<AudioURI> items){
        this.audioURISES.addAll(items);
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
        holder.audioURI = audioURISES.get(position);
        holder.textViewPlaylistName.setText(audioURISES.get(position).title);
    }

    @Override
    public int getItemCount() {
        return audioURISES.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView textViewPlaylistName;
        public AudioURI audioURI;

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
    }
}