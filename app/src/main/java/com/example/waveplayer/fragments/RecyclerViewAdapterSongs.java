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

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.fragments.fragment_playlist.FragmentPlaylistDirections;
import com.example.waveplayer.fragments.fragment_songs.FragmentSongsDirections;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.R;
import com.example.waveplayer.fragments.fragment_playlist.FragmentPlaylist;
import com.example.waveplayer.fragments.fragment_songs.FragmentSongs;

import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    private final Fragment fragment;

    private List<AudioUri> audioUris;
    public List<AudioUri> getAudioUris(){
        return audioUris;
    }

    private OnCreateContextMenuListenerSongs onCreateContextMenuListenerSongs;

    private View.OnClickListener onClickListenerHandle;

    private View.OnClickListener onClickListenerViewHolder;

    public RecyclerViewAdapterSongs(Fragment fragment, List<AudioUri> items) {
        this.fragment = fragment;
        audioUris = items;
    }

    public void updateList(List<AudioUri> audioURISES) {
        this.audioUris = audioURISES;
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
        holder.audioURI = audioUris.get(position);
        holder.textViewSongName.setText(audioUris.get(position).title);
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
                        if (holder.audioURI.equals(activityMain.getCurrentSong())) {
                            activityMain.seekTo(0);
                        }
                        if (fragment instanceof FragmentSongs) {
                            activityMain.setCurrentPlaylistToMaster();
                            action = FragmentSongsDirections.actionFragmentSongsToFragmentSong();
                        } else if (fragment instanceof FragmentPlaylist) {
                            activityMain.setCurrentPlaylist(activityMain.getUserPickedPlaylist());
                            action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong();
                        }
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
        return audioUris.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final View songView;
        public final TextView textViewSongName;
        public AudioUri audioURI;
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