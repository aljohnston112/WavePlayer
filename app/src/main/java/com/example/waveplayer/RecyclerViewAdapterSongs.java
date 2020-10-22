package com.example.waveplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    public static final String ADD_TO_PLAYLIST_SONG = "ADD_TO_PLAYLIST_SONG";
    public static final String PLAYLISTS = "PLAYLISTS";

    final private Fragment fragment;
    public List<AudioURI> audioURIS;

    public RecyclerViewAdapterSongs(Fragment fragment, List<AudioURI> items) {
        this.fragment = fragment;
        audioURIS = items;
    }

    public void updateList(List<AudioURI> audioURIS){
        this.audioURIS = audioURIS;
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
    }

    @Override
    public int getItemCount() {
        return audioURIS.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View songView;
        public final TextView textViewSongName;
        public AudioURI audioURI;

        public ViewHolder(View view) {
            super(view);
            songView = view;
            textViewSongName = view.findViewById(R.id.text_view_songs_name);
            ImageView handle = view.findViewById(R.id.handle);
            handle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(ADD_TO_PLAYLIST_SONG, audioURI);
                    bundle.putSerializable(PLAYLISTS, ((ActivityMain)fragment.getActivity()).serviceMain.playlists);
                    AddToPlaylistDialog addToPlaylistDialog = new AddToPlaylistDialog();
                    addToPlaylistDialog.setArguments(bundle);
                    addToPlaylistDialog.show(fragment.getParentFragmentManager(), fragment.getTag());
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    NavDirections action = null;
                    if (position != RecyclerView.NO_POSITION) {
                        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
                        if (activityMain != null) {
                            if (audioURI.equals(activityMain.serviceMain.currentSong)) {
                                activityMain.serviceMain.songsMap.get(
                                        activityMain.serviceMain.currentSong.getUri()).seekTo(0);
                            } else{
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
                            activityMain.serviceMain.clearQueue();
                            activityMain.addToQueueAndPlay(audioURI);
                        }
                        if (action != null) {
                            NavHostFragment.findNavController(fragment).navigate(action);
                        }
                    }
                }
            });

        }

        @NonNull
        @Override
        public String toString() {
            return audioURI.title;
        }

    }

}