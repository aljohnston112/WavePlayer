package com.example.waveplayer.fragments;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.constraintlayout.widget.ConstraintLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.R;

import java.util.List;
import java.util.function.Consumer;

public class RecyclerViewAdapterSelectSongs extends RecyclerView.Adapter<RecyclerViewAdapterSelectSongs.ViewHolder> {

    public interface ListenerCallbackSelectSongs {
        List<Song> getUserPickedSongs();

        void removeUserPickedSong(Song song);

        void addUserPickedSong(Song song);
    }

    private ListenerCallbackSelectSongs listenerCallbackSelectSongs;

    private List<Song> allSongs;

    private View.OnClickListener onClickListener;

    public RecyclerViewAdapterSelectSongs(ListenerCallbackSelectSongs listenerCallbackSelectSongs,
                                          List<Song> allSongs) {
        this.listenerCallbackSelectSongs = listenerCallbackSelectSongs;
        this.allSongs = allSongs;
    }

    public void updateList(List<Song> songs) {
        this.allSongs = songs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerViewAdapterSelectSongs.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ConstraintLayout linearLayout = holder.songView.findViewById(R.id.constraint_layout_song_name);
        List<Song> userPickedSongs = listenerCallbackSelectSongs.getUserPickedSongs();
        if (userPickedSongs.contains(allSongs.get(position))) {
            allSongs.get(position).setSelected(true);
            // TODO use color resources
            holder.textViewSongName.setBackgroundColor(Color.parseColor("#575757"));
            linearLayout.setBackgroundColor(Color.parseColor("#575757"));
        } else {
            allSongs.get(position).setSelected(false);
            holder.textViewSongName.setBackgroundColor(Color.parseColor("#000000"));
            linearLayout.setBackgroundColor(Color.parseColor("#000000"));
        }
        holder.song = allSongs.get(position);
        holder.textViewSongName.setText(allSongs.get(position).title);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerViewAdapterSelectSongs.ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.songView.setOnClickListener(null);
        onClickListener = null;
        holder.song = null;
        listenerCallbackSelectSongs = null;
    }

    @Override
    public int getItemCount() {
        return allSongs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View songView;
        public final TextView textViewSongName;
        public Song song;

        public ViewHolder(final View view) {
            super(view);
            // TODO invisible or gone?
            view.findViewById(R.id.song_handle).setVisibility(View.GONE);
            songView = view;
            textViewSongName = view.findViewById(R.id.text_view_songs_name);
            final ConstraintLayout constraintLayout = view.findViewById(R.id.constraint_layout_song_name);
            onClickListener = v -> {
                if (song.isSelected()) {
                    song.setSelected(false);
                    // TOTO color resources
                    textViewSongName.setBackgroundColor(Color.parseColor("#000000"));
                    constraintLayout.setBackgroundColor(Color.parseColor("#000000"));
                    listenerCallbackSelectSongs.removeUserPickedSong(song);
                } else {
                    song.setSelected(true);
                    textViewSongName.setBackgroundColor(Color.parseColor("#575757"));
                    constraintLayout.setBackgroundColor(Color.parseColor("#575757"));
                    listenerCallbackSelectSongs.addUserPickedSong(song);
                }
            };
            view.setOnClickListener(onClickListener);
        }
    }

}