package com.example.waveplayer;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;

/**
 * {@link RecyclerView.Adapter} that can display a {@link WaveURI}.
 */
public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private final List<WaveURI> waveURIS;

    private final FragmentSongs fragmentSongs;

    public RecyclerViewAdapterSongs(ArrayList<WaveURI> items, FragmentSongs fragmentSongs) {
        waveURIS = items;
        this.fragmentSongs = fragmentSongs;
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

                    // check if item still exists
                    if(pos != RecyclerView.NO_POSITION){
                        WaveURI clickedDataItem = waveURIS.get(pos);
                        FragmentSongsDirections.ActionFragmentSongsToFragmentSong action =
                                FragmentSongsDirections.actionFragmentSongsToFragmentSong(toString());
                        NavHostFragment.findNavController(fragmentSongs)
                                .navigate(action);
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