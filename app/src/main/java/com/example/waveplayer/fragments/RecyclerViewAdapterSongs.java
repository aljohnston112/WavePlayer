package com.example.waveplayer.fragments;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.service_main.ServiceMain;

import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private List<Song> songs;

    public List<Song> getSongs() {
        return songs;
    }

    private ListenerCallbackSongs listenerCallbackSongs;

    public interface ListenerCallbackSongs {
        boolean onMenuItemClickAddToPlaylist(Song song);
        boolean onMenuItemClickAddToQueue(Song song);
        void onClickViewHolder(Song Song);
    }

    private View.OnCreateContextMenuListener onCreateContextMenuListenerSongs;

    private View.OnClickListener onClickListenerViewHolder;

    private View.OnClickListener onClickListenerHandle;

    public RecyclerViewAdapterSongs(ListenerCallbackSongs listenerCallbackSongs, List<Song> items) {
        this.listenerCallbackSongs = listenerCallbackSongs;
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

        // onCreateContextMenu
        onCreateContextMenuListenerSongs = (menu, v, menuInfo) -> {
                    final MenuItem menuItemAddToPlaylist = menu.add(R.string.add_to_playlist);
                    menuItemAddToPlaylist.setOnMenuItemClickListener(
                            menuItem -> listenerCallbackSongs.onMenuItemClickAddToPlaylist(holder.song));
                    final MenuItem menuItemAddToQueue = menu.add(R.string.add_to_queue);
                    menuItemAddToQueue.setOnMenuItemClickListener(
                            menuItem2 -> listenerCallbackSongs.onMenuItemClickAddToQueue(holder.song));
                };
        holder.handle.setOnCreateContextMenuListener(null);
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerSongs);

        // onClickListenerHandle
        onClickListenerHandle = v -> holder.handle.performLongClick();
        holder.handle.setOnClickListener(null);
        holder.handle.setOnClickListener(onClickListenerHandle);

        // onClickListenerViewHolder
        onClickListenerViewHolder = v -> ServiceMain.executorServiceFIFO.submit(() -> {
            if (position != RecyclerView.NO_POSITION) {
                listenerCallbackSongs.onClickViewHolder(holder.song);
            }
        });
        holder.songView.setOnClickListener(null);
        holder.songView.setOnClickListener(onClickListenerViewHolder);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.handle.setOnCreateContextMenuListener(null);
        onCreateContextMenuListenerSongs = null;
        holder.handle.setOnClickListener(null);
        onClickListenerHandle = null;
        holder.songView.setOnClickListener(null);
        onClickListenerViewHolder = null;
        holder.song = null;
        listenerCallbackSongs = null;
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