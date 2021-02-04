package com.example.waveplayer.fragments;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist;
import com.example.waveplayer.fragments.FragmentPlaylistsDirections;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.Song;

import java.util.List;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;

public class RecyclerViewAdapterPlaylists extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    private final FragmentPlaylists fragmentPlaylists;

    public List<RandomPlaylist> randomPlaylists;

    private View.OnCreateContextMenuListener onCreateContextMenuListenerPlaylists;

    private View.OnClickListener onClickListenerHandle;

    private View.OnClickListener onClickListenerViewHolder;

    public RecyclerViewAdapterPlaylists(FragmentPlaylists fragmentPlaylists, List<RandomPlaylist> items) {
        this.fragmentPlaylists = fragmentPlaylists;
        randomPlaylists = items;
    }

    public void updateList(List<RandomPlaylist> randomPlaylists) {
        this.randomPlaylists = randomPlaylists;
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.randomPlaylist = randomPlaylists.get(position);
        holder.textViewPlaylistName.setText(randomPlaylists.get(position).getName());
        holder.handle.setOnCreateContextMenuListener(null);
        onCreateContextMenuListenerPlaylists =
                new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        final MenuItem itemAddToPlaylist = menu.add(R.string.add_to_playlist);
                        itemAddToPlaylist.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                contextMenuAddToPlaylist();
                                return true;
                            }
                        });
                        final MenuItem itemAddToQueue = menu.add(R.string.add_to_queue);
                        itemAddToQueue.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                contextMenuAddToQueue();
                                return true;
                            }
                        });
                    }

                    private void contextMenuAddToPlaylist() {
                        ActivityMain activityMain = ((ActivityMain) fragmentPlaylists.requireActivity());
                        if (activityMain != null) {
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, holder.randomPlaylist);
                            bundle.putSerializable(BUNDLE_KEY_IS_SONG, false);
                            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist
                                    = new DialogFragmentAddToPlaylist();
                            dialogFragmentAddToPlaylist.setArguments(bundle);
                            dialogFragmentAddToPlaylist.show(
                                    fragmentPlaylists.getParentFragmentManager(), fragmentPlaylists.getTag());
                        }
                    }

                    private void contextMenuAddToQueue() {
                        ActivityMain activityMain = ((ActivityMain) fragmentPlaylists.requireActivity());
                        for (Song song : holder.randomPlaylist.getSongs()) {
                            activityMain.addToQueue(song.id);
                        }
                        activityMain.setShuffling(false);
                        activityMain.setLooping(false);
                        activityMain.setLoopingOne(false);
                        activityMain.showSongPane();
                        // TODO stop MasterPlaylist from continuing after queue is done
                        activityMain.setCurrentPlaylistToMaster();
                        if (!activityMain.isPlaying()) {
                            activityMain.goToFrontOfQueue();
                            activityMain.playNext();
                        }
                    }
                };
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerPlaylists);
        onClickListenerHandle = v -> holder.handle.performLongClick();
        holder.handle.setOnClickListener(null);
        holder.handle.setOnClickListener(onClickListenerHandle);
        onClickListenerViewHolder = v -> {
            int position1 = holder.getAdapterPosition();
            if (position1 != RecyclerView.NO_POSITION) {
                ActivityMain activityMain = ((ActivityMain) fragmentPlaylists.getActivity());
                if (activityMain != null) {
                    fragmentPlaylists.setUserPickedPlaylist(holder.randomPlaylist);
                }
                NavHostFragment.findNavController(fragmentPlaylists).navigate(
                        FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist());
            }
        };
        holder.playlistView.setOnClickListener(null);
        holder.playlistView.setOnClickListener(onClickListenerViewHolder);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        onCreateContextMenuListenerPlaylists = null;
        holder.handle.setOnCreateContextMenuListener(null);
        onClickListenerHandle = null;
        holder.handle.setOnClickListener(null);
        onClickListenerViewHolder = null;
        holder.playlistView.setOnClickListener(null);
        holder.randomPlaylist = null;
    }

    @Override
    public int getItemCount() {
        return randomPlaylists.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final public View playlistView;
        final public TextView textViewPlaylistName;
        public RandomPlaylist randomPlaylist;
        final ImageView handle;

        public ViewHolder(View view) {
            super(view);
            playlistView = view;
            textViewPlaylistName = view.findViewById(R.id.text_view_playlist_name);
            if (randomPlaylist != null) {
                textViewPlaylistName.setText(randomPlaylist.getName());
            }
            handle = view.findViewById(R.id.playlist_handle);
        }

        @Override
        @NonNull
        public String toString() {
            return randomPlaylist.getName();
        }

    }

}