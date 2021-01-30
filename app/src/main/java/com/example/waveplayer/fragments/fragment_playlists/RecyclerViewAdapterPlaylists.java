package com.example.waveplayer.fragments.fragment_playlists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.List;

public class RecyclerViewAdapterPlaylists extends RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder> {

    private final FragmentPlaylists fragmentPlaylists;

    public List<RandomPlaylist> randomPlaylists;

    private OnCreateContextMenuListenerPlaylists onCreateContextMenuListenerPlaylists;

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
                new OnCreateContextMenuListenerPlaylists(fragmentPlaylists, holder.randomPlaylist);
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