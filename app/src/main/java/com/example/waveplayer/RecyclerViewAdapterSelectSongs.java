package com.example.waveplayer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapterSelectSongs extends RecyclerView.Adapter<RecyclerViewAdapterSelectSongs.ViewHolder>{

    private final Fragment fragment;

    private final List<AudioURI> allSongs;

    private final List<AudioURI> selectedSongs;

    public RecyclerViewAdapterSelectSongs(Fragment fragment, List<AudioURI> allSongs, List<AudioURI> selectedSongs) {
        this.allSongs = allSongs;
        this.fragment = fragment;
        this.selectedSongs = selectedSongs;
        for(AudioURI audioURI : allSongs){
            if(selectedSongs.contains(audioURI)){
                audioURI.setSelected(true);
            }
        }
    }

    @NonNull
    @Override
    public RecyclerViewAdapterSelectSongs.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewAdapterSelectSongs.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_songs, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ConstraintLayout linearLayout = holder.songView.findViewById(R.id.linear_layout_song_name);
        if(selectedSongs != null && allSongs != null) {
            if (selectedSongs.contains(allSongs.get(position))) {
                allSongs.get(position).setSelected(true);
                holder.textViewSongName.setBackgroundColor(Color.parseColor("#000057"));
                linearLayout.setBackgroundColor(Color.parseColor("#000057"));
            } else {
                allSongs.get(position).setSelected(false);
                holder.textViewSongName.setBackgroundColor(Color.parseColor("#000000"));
                linearLayout.setBackgroundColor(Color.parseColor("#000000"));
            }
        }
        if(allSongs != null){
            holder.audioURI = allSongs.get(position);
            holder.textViewSongName.setText(allSongs.get(position).title);
        }

    }


    @Override
    public int getItemCount() {
        return allSongs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View songView;
        public final TextView textViewSongName;
        public AudioURI audioURI;

        public ViewHolder(final View view) {
            super(view);
            if(fragment instanceof FragmentSelectSongs){
                view.findViewById(R.id.handle).setVisibility(View.INVISIBLE);
            }
            songView = view;
            textViewSongName = view.findViewById(R.id.text_view_songs_name);
            final ConstraintLayout linearLayout = view.findViewById(R.id.linear_layout_song_name);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        if(audioURI.isSelected()){
                            audioURI.setSelected(false);
                            textViewSongName.setBackgroundColor(Color.parseColor("#000000"));
                            linearLayout.setBackgroundColor(Color.parseColor("#000000"));
                            selectedSongs.remove(audioURI);
                        } else{
                            audioURI.setSelected(true);
                            textViewSongName.setBackgroundColor(Color.parseColor("#000057"));
                            linearLayout.setBackgroundColor(Color.parseColor("#000057"));
                            selectedSongs.add(audioURI);
                        }
                    }
            });
        }

    }

}