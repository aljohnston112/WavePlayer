package com.example.waveplayer.fragments;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.media_controller.ServiceMain;

import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    public interface ListenerCallbackSongs {
        void onClickViewHolder(Song Song);
        boolean onMenuItemClickAddToPlaylist(Song song);
        boolean onMenuItemClickAddToQueue(Song song);
    }

    private ListenerCallbackSongs listenerCallbackSongs;

    private View.OnCreateContextMenuListener onCreateContextMenuListenerSongs;
    private MenuItem.OnMenuItemClickListener onMenuItemClickListenerAddToPlaylist;
    private MenuItem.OnMenuItemClickListener onMenuItemClickListenerAddToQueue;

    private View.OnClickListener onClickListenerViewHolder;
    private View.OnClickListener onClickListenerHandle;

    private List<Song> songs;

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

        // onClickListenerHandle
        onClickListenerHandle = v -> holder.handle.performLongClick();
        holder.handle.setOnClickListener(null);
        holder.handle.setOnClickListener(onClickListenerHandle);

        // onClickListenerViewHolder
        onClickListenerViewHolder = v ->  {
            if (position != RecyclerView.NO_POSITION) {
                listenerCallbackSongs.onClickViewHolder(holder.song);
            }
        };
        holder.songView.setOnClickListener(null);
        holder.songView.setOnClickListener(onClickListenerViewHolder);

        holder.song = songs.get(position);
        holder.textViewSongName.setText(songs.get(position).title);

        // onCreateContextMenu
        onMenuItemClickListenerAddToPlaylist = menuItem ->
                listenerCallbackSongs.onMenuItemClickAddToPlaylist(holder.song);
        onMenuItemClickListenerAddToQueue = menuItem2 ->
                listenerCallbackSongs.onMenuItemClickAddToQueue(holder.song);
        onCreateContextMenuListenerSongs = (menu, v, menuInfo) -> {
                    final MenuItem menuItemAddToPlaylist = menu.add(R.string.add_to_playlist);
                    menuItemAddToPlaylist.setOnMenuItemClickListener(onMenuItemClickListenerAddToPlaylist);
                    final MenuItem menuItemAddToQueue = menu.add(R.string.add_to_queue);
                    menuItemAddToQueue.setOnMenuItemClickListener(onMenuItemClickListenerAddToQueue);
                };
        holder.handle.setOnCreateContextMenuListener(null);
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerSongs);

    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.handle.setOnCreateContextMenuListener(null);
        onCreateContextMenuListenerSongs = null;
        onMenuItemClickListenerAddToPlaylist = null;
        onMenuItemClickListenerAddToQueue = null;
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

    public List<Song> getSongs() {
        return songs;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final View songView;
        public final TextView textViewSongName;
        public final ImageView handle;
        public Song song;

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