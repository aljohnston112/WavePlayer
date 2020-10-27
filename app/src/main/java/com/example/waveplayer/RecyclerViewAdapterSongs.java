package com.example.waveplayer;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    final private Fragment fragment;
    public List<AudioURI> audioURIS;

    OnCreateContextMenuListenerSongs onCreateContextMenuListenerSongs;

    View.OnClickListener onClickListenerHandle;

    View.OnClickListener onClickListenerViewHolder;

    public RecyclerViewAdapterSongs(Fragment fragment, List<AudioURI> items) {
        this.fragment = fragment;
        audioURIS = items;
    }

    public void updateList(List<AudioURI> audioURIS) {
        this.audioURIS = audioURIS;
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
        holder.audioURI = audioURIS.get(position);
        holder.textViewSongName.setText(audioURIS.get(position).title);
        onCreateContextMenuListenerSongs =
                new OnCreateContextMenuListenerSongs(fragment, holder.audioURI);
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
                int position = holder.getAdapterPosition();
                NavDirections action = null;
                if (position != RecyclerView.NO_POSITION) {
                    ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
                    if (activityMain != null) {
                        if (holder.audioURI.equals(activityMain.serviceMain.currentSong)) {
                            MediaPlayerWURI mediaPlayerWURI =
                                    activityMain.serviceMain.uriMediaPlayerWURIHashMap.get(
                                            activityMain.serviceMain.currentSong.getUri());
                            if(mediaPlayerWURI != null) {
                                mediaPlayerWURI.seekTo(0);
                            }
                        } else {
                            activityMain.serviceMain.stopAndPreparePrevious();
                        }
                        if (fragment instanceof FragmentSongs) {
                            activityMain.serviceMain.currentPlaylist =
                                    activityMain.serviceMain.masterPlaylist;
                            action = FragmentSongsDirections.actionFragmentSongsToFragmentSong();
                        } else if (fragment instanceof FragmentPlaylist) {
                            activityMain.serviceMain.currentPlaylist =
                                    activityMain.serviceMain.userPickedPlaylist;
                            action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong();
                        }
                        activityMain.serviceMain.currentPlaylistArray =
                                new ArrayList<>(activityMain.serviceMain.currentPlaylist
                                        .getProbFun().getProbMap().keySet());
                        activityMain.serviceMain.clearSongQueue();
                        activityMain.addToQueueAndPlay(holder.audioURI);
                    }
                    if (action != null) {
                        NavHostFragment.findNavController(fragment).navigate(action);
                    }
                }
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
        holder.audioURI = null;
    }

    @Override
    public int getItemCount() {
        return audioURIS.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View songView;
        public final TextView textViewSongName;
        public AudioURI audioURI;
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
            return audioURI.title;
        }

    }

}