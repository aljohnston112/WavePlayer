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

import java.util.ArrayList;

public class RecyclerViewAdapterSelectSongs extends RecyclerView.Adapter<RecyclerViewAdapterSelectSongs.ViewHolder> {

    private final Fragment fragment;

    public RecyclerViewAdapterSelectSongs(Fragment fragment) {
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public RecyclerViewAdapterSelectSongs.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewAdapterSelectSongs.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ConstraintLayout linearLayout = holder.songView.findViewById(R.id.linear_layout_song_name);
        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
        if (activityMain.serviceMain != null) {
            ArrayList<AudioURI> allSongs = new ArrayList<>(activityMain.serviceMain.uriMap.values());
            ArrayList<AudioURI> userPickedSongs = activityMain.serviceMain.userPickedSongs;
            if (userPickedSongs.contains(allSongs.get(position))) {
                allSongs.get(position).setSelected(true);
                holder.textViewSongName.setBackgroundColor(Color.parseColor("#575757"));
                linearLayout.setBackgroundColor(Color.parseColor("#575757"));
            } else {
                allSongs.get(position).setSelected(false);
                holder.textViewSongName.setBackgroundColor(Color.parseColor("#000000"));
                linearLayout.setBackgroundColor(Color.parseColor("#000000"));
            }
            holder.audioURI = allSongs.get(position);
            holder.textViewSongName.setText(allSongs.get(position).title);
        }

    }


    @Override
    public int getItemCount() {
        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
        return activityMain.serviceMain.uriMap.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View songView;
        public final TextView textViewSongName;
        public AudioURI audioURI;

        public ViewHolder(final View view) {
            super(view);
            if (fragment instanceof FragmentSelectSongs) {
                view.findViewById(R.id.handle).setVisibility(View.INVISIBLE);
            }
            songView = view;
            textViewSongName = view.findViewById(R.id.text_view_songs_name);
            final ConstraintLayout linearLayout = view.findViewById(R.id.linear_layout_song_name);
            final ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (audioURI.isSelected()) {
                        audioURI.setSelected(false);
                        textViewSongName.setBackgroundColor(Color.parseColor("#000000"));
                        linearLayout.setBackgroundColor(Color.parseColor("#000000"));
                        activityMain.serviceMain.userPickedSongs.remove(audioURI);
                    } else {
                        audioURI.setSelected(true);
                        textViewSongName.setBackgroundColor(Color.parseColor("#575757"));
                        linearLayout.setBackgroundColor(Color.parseColor("#575757"));
                        activityMain.serviceMain.userPickedSongs.add(audioURI);
                    }
                }
            });
        }

    }

}