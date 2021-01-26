package com.example.waveplayer.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.service_main.ServiceMain;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.R;
import com.example.waveplayer.fragments.fragment_playlist.FragmentPlaylist;
import com.example.waveplayer.fragments.fragment_playlist.FragmentPlaylistDirections;
import com.example.waveplayer.fragments.fragment_songs.FragmentSongs;
import com.example.waveplayer.fragments.fragment_songs.FragmentSongsDirections;

import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private final Fragment fragment;

    private List<Song> songs;

    public List<Song> getSongs() {
        return songs;
    }

    private OnCreateContextMenuListenerSongs onCreateContextMenuListenerSongs;

    private View.OnClickListener onClickListenerHandle;

    private View.OnClickListener onClickListenerViewHolder;

    public RecyclerViewAdapterSongs(Fragment fragment, List<Song> items) {
        this.fragment = fragment;
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
                new OnCreateContextMenuListenerSongs(fragment, holder.song);
        holder.handle.setOnCreateContextMenuListener(null);
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerSongs);
        onClickListenerHandle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.handle.performLongClick();
            }
        };
        holder.handle.setOnClickListener(null);
        holder.handle.setOnClickListener(onClickListenerHandle);
        onClickListenerViewHolder = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServiceMain.executorServiceFIFO.submit(new Runnable() {
                    @Override
                    public void run() {
                        int position = holder.getAdapterPosition();
                        NavDirections action = null;
                        if (position != RecyclerView.NO_POSITION) {
                            ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
                            if (activityMain != null) {
                                if (activityMain.getCurrentAudioUri() != null &&
                                        holder.song.equals(
                                                MediaData.getInstance(activityMain).getSong(
                                                        activityMain.getCurrentAudioUri().id))) {
                                    activityMain.seekTo(0);
                                }
                                if (fragment instanceof FragmentSongs) {
                                    activityMain.setCurrentPlaylistToMaster();
                                    action = FragmentSongsDirections.actionFragmentSongsToFragmentSong();
                                } else if (fragment instanceof FragmentPlaylist) {
                                    activityMain.setCurrentPlaylist(
                                            ((FragmentPlaylist) fragment).getUserPickedPlaylist());
                                    action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong();
                                }
                                activityMain.clearSongQueue();
                                activityMain.addToQueueAndPlay(holder.song.id);
                            }
                            if (action != null) {
                                NavHostFragment.findNavController(fragment).navigate(action);
                            }
                        }
                    }
                });
            }
        };
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