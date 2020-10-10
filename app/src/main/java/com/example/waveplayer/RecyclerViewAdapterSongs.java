package com.example.waveplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapterSongs extends RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder> {

    final private Fragment fragment;
    final public List<AudioURI> audioURIS;

    public RecyclerViewAdapterSongs(Fragment fragment, List<AudioURI> items) {
        this.fragment = fragment;
        audioURIS = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_songs, parent, false));
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
            if(fragment instanceof FragmentSongs){
                view.findViewById(R.id.handle).setVisibility(View.INVISIBLE);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    NavDirections action = null;
                    if (position != RecyclerView.NO_POSITION) {
                        ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
                        if(activityMain != null) {
                            if(activityMain.serviceMain.isPlaying()){
                                activityMain.serviceMain.pauseOrPlay();
                            }
                            activityMain.serviceMain.songQueueIterator = null;
                            activityMain.serviceMain.songQueue.clear();
                            activityMain.serviceMain.songQueueIterator = activityMain.serviceMain.songQueue.listIterator();
                            activityMain.serviceMain.songQueueIterator.add(audioURI.getUri());
                            activityMain.addToQueueAndPlay(audioURI);

                            if (fragment instanceof FragmentSongs) {
                                activityMain.serviceMain.currentPlaylist = activityMain.serviceMain.masterPlaylist;
                                action = FragmentSongsDirections.actionFragmentSongsToFragmentSong();
                            } else if (fragment instanceof FragmentPlaylist) {
                                activityMain.serviceMain.currentPlaylist = activityMain.serviceMain.userPickedPlaylist;
                                action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong();
                            }
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