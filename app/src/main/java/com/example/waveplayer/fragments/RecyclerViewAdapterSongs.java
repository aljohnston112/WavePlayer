package com.example.waveplayer.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.service_main.ServiceMain;

import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private final OnCreateContextMenuListenerSongsCallback onCreateContextMenuListenerSongsCallback;

    private List<Song> songs;

    public List<Song> getSongs() {
        return songs;
    }

    private OnCreateContextMenuListenerSongs onCreateContextMenuListenerSongs;

    public interface OnCreateContextMenuListenerSongsCallback {
        boolean onMenuItemClickAddToPlaylist(Song song);
        boolean onMenuItemClickAddToQueue(Song song);
    }

    private View.OnClickListener onClickListenerViewHolder;

    private final OnClickListenerViewHolderCallback onClickListenerViewHolderCallback;

    public interface OnClickListenerViewHolderCallback {
        void onClick(Song Song);
    }

    private View.OnClickListener onClickListenerHandle;

    public RecyclerViewAdapterSongs(
            OnCreateContextMenuListenerSongsCallback onCreateContextMenuListenerSongsCallback,
            OnClickListenerViewHolderCallback onClickListenerViewHolderCallback,
            List<Song> items) {
        this.onCreateContextMenuListenerSongsCallback = onCreateContextMenuListenerSongsCallback;
        this.onClickListenerViewHolderCallback = onClickListenerViewHolderCallback;
        songs = items;
    }

    public void updateList(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.song = songs.get(position);
        holder.textViewSongName.setText(songs.get(position).title);
        onCreateContextMenuListenerSongs =
                new OnCreateContextMenuListenerSongs(onCreateContextMenuListenerSongsCallback, holder.song);
        holder.handle.setOnCreateContextMenuListener(null);
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerSongs);
        onClickListenerHandle = v -> holder.handle.performLongClick();
        holder.handle.setOnClickListener(null);
        holder.handle.setOnClickListener(onClickListenerHandle);
        onClickListenerViewHolder = v -> ServiceMain.executorServiceFIFO.submit(() -> {
            if (position != RecyclerView.NO_POSITION) {
                onClickListenerViewHolderCallback.onClick(holder.song);
            }
        });
        holder.songView.setOnClickListener(null);
        holder.songView.setOnClickListener(onClickListenerViewHolder);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        onCreateContextMenuListenerSongs = null;
        holder.handle.setOnCreateContextMenuListener(null);
        onClickListenerHandle = null;
        holder.handle.setOnClickListener(null);
        onClickListenerViewHolder = null;
        holder.songView.setOnClickListener(null);
        holder.song = null;
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final View songView;
        public final TextView textViewSongName;
        public Song song;
        final ImageView handle;

        public ViewHolder(View view) {
            super(view);
            songView = view;
            textViewSongName = view.findViewById(R.id.text_view_songs_name);
            handle = view.findViewById(R.id.song_handle);
        }

        @NonNull
        @Override
        public String toString() {
            return song.title;
        }

    }

}