package com.example.waveplayer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapterSelectSongs extends RecyclerView.Adapter<RecyclerViewAdapterSelectSongs.ViewHolder>{

    private final List<AudioURI> audioURIS;

    private final Fragment fragment;

    private final List<AudioURI> selectedSongs;

    public RecyclerViewAdapterSelectSongs(List<AudioURI> audioURIS, List<AudioURI> selectedSongs, Fragment fragment) {
        this.audioURIS = audioURIS;
        this.fragment = fragment;
        this.selectedSongs = selectedSongs;
    }

    @NonNull
    @Override
    public RecyclerViewAdapterSelectSongs.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_songs, parent, false);
        return new RecyclerViewAdapterSelectSongs.ViewHolder(view);    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if(selectedSongs.contains(audioURIS.get(position))){
            audioURIS.get(position).setChecked(true);
            holder.textViewSongName.setBackgroundColor(Color.parseColor("#000057"));
        } else{
            audioURIS.get(position).setChecked(false);
            holder.textViewSongName.setBackgroundColor(Color.parseColor("#000000"));

        }
        holder.audioURI = audioURIS.get(position);
        holder.textViewSongName.setText(audioURIS.get(position).title);
    }


    @Override
    public int getItemCount() {
        return audioURIS.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View mView;
        public final TextView textViewSongName;
        public AudioURI audioURI;

        public ViewHolder(final View view) {
            super(view);
            mView = view;
            textViewSongName = (TextView) view.findViewById(R.id.text_view_song_name);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // get position
                    int pos = getAdapterPosition();

                    NavDirections action = null;
                    // check if item still exists
                    if (pos != RecyclerView.NO_POSITION) {
                        if(audioURI.isChecked()){
                            audioURI.setChecked(false);
                            textViewSongName.setSelected(false);
                            textViewSongName.setBackgroundColor(Color.parseColor("#000000"));

                        } else{
                            audioURI.setChecked(true);
                            textViewSongName.setSelected(true);
                            textViewSongName.setBackgroundColor(Color.parseColor("#000057"));
                        }
                    }
                }
            });
        }

    }

}
