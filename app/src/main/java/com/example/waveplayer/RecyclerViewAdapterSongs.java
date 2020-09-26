package com.example.waveplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link WaveURI}.
 */
public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private final List<WaveURI> waveURIS;

    private final Fragment fragment;

    public RecyclerViewAdapterSongs(ArrayList<WaveURI> items, Fragment fragment) {
        waveURIS = items;
        this.fragment = fragment;
    }

    public void addSongs(List<WaveURI> items){
        for(WaveURI waveURI : items){
            this.waveURIS.add(waveURI);
        }
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
        holder.waveURI = waveURIS.get(position);
        holder.textViewSongName.setText(waveURIS.get(position).name);
    }

    @Override
    public int getItemCount() {
        return waveURIS.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView textViewSongName;
        public WaveURI waveURI;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            textViewSongName = (TextView) view.findViewById(R.id.text_view_song_name);
            view.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    // get position
                    int pos = getAdapterPosition();

                    NavDirections action = null;
                    // check if item still exists
                    if(pos != RecyclerView.NO_POSITION){
                        if(fragment instanceof FragmentSongs) {
                            action = FragmentSongsDirections.actionFragmentSongsToFragmentSong(toString());
                        } else if(fragment instanceof FragmentPlaylist){
                          action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong(toString());
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
            return waveURI.name;
        }
    }
}