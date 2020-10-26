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

    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_SONG = "ADD_TO_PLAYLIST_SONG";
    public static final String BUNDLE_KEY_PLAYLISTS = "PLAYLISTS";

    final private Fragment fragment;
    public List<AudioURI> audioURIS;

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
            final ImageView handle = view.findViewById(R.id.song_handle);
            handle.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    final MenuItem item = menu.add(R.string.add_to_playlist);
                    item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (fragment instanceof FragmentSongs ||
                            fragment instanceof FragmentPlaylist) {
                                contextMenuAddToPlaylist(true);
                            } else if(fragment instanceof FragmentPlaylists){
                                contextMenuAddToPlaylist(false);
                            }
                            return true;
                        }
                    });
                    final MenuItem anotherItem = menu.add(R.string.add_to_queue);
                    anotherItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            contextMenuAddToQueue();
                            return true;
                        }
                    });
                }
            });
            handle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handle.performLongClick();
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
                            activityMain.addToQueueAndPlay(audioURI);
                        }
                        if (action != null) {
                            NavHostFragment.findNavController(fragment).navigate(action);
                        }
                    }
                }
            });

        }

        private void contextMenuAddToPlaylist(boolean isSong) {
            ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
            if(activityMain != null) {
                Bundle bundle = new Bundle();
                bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, audioURI);
                bundle.putSerializable(BUNDLE_KEY_PLAYLISTS, activityMain.serviceMain.playlists);
                DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                        new DialogFragmentAddToPlaylist(isSong);
                dialogFragmentAddToPlaylist.setArguments(bundle);
                dialogFragmentAddToPlaylist.show(fragment.getParentFragmentManager(), fragment.getTag());
            }
        }

        private void contextMenuAddToQueue() {
            ActivityMain activityMain = ((ActivityMain) fragment.getActivity());
            if(activityMain != null) {
                if (activityMain.serviceMain.songInProgress()) {
                    ((ActivityMain) fragment.getActivity()).serviceMain.addToQueue(audioURI.getUri());
                } else {
                    ((ActivityMain) fragment.getActivity()).serviceMain.addToQueueAndPlay(audioURI);
                    ((ActivityMain) fragment.getActivity()).showSongPane();
                    ((ActivityMain) fragment.getActivity()).updateUI();
                }
            }
        }

        @NonNull
        @Override
        public String toString() {
            return audioURI.title;
        }

    }

}