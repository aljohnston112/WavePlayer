package com.example.waveplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link WaveURI}.
 */
public class RecyclerViewAdapterPlaylists extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    private final List<WaveURI> waveURIS;

    public RecyclerViewAdapterPlaylists(List<WaveURI> items) {
        waveURIS = items;
    }

    public void addPlaylists(List<WaveURI> items){
        for(WaveURI waveURI : items){
            this.waveURIS.add(waveURI);
        }
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
        holder.waveURI = waveURIS.get(position);
        holder.textViewPlaylistName.setText(waveURIS.get(position).name);
    }

    @Override
    public int getItemCount() {
        return waveURIS.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView textViewPlaylistName;
        public WaveURI waveURI;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            textViewPlaylistName = (TextView) view.findViewById(R.id.text_view_playlist_name);
        }

        @Override
        @NonNull
        public String toString() {
            return super.toString() + " '" + textViewPlaylistName.getText() + "'";
        }
    }
}