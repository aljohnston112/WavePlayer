package com.example.waveplayer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link WaveURI}.
 */
public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private final List<WaveURI> waveURIS;

    public RecyclerViewAdapterSongs(ArrayList<WaveURI> items) {
        waveURIS = items;
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
                .inflate(R.layout.fragment_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.waveURI = waveURIS.get(position);
        holder.textViewSongName.getEditText().setText(waveURIS.get(position).name);
    }

    @Override
    public int getItemCount() {
        return waveURIS.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextInputLayout textViewSongName;
        public WaveURI waveURI;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            textViewSongName = (TextInputLayout) view.findViewById(R.id.text_view_song_name);
        }

        @NonNull
        @Override
        public String toString() {
            return "";//super.toString() + " '" + textViewSongName.getText() + "'";
        }
    }
}