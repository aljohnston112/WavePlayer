package com.example.waveplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FragmentSongs extends Fragment {

    RecyclerView recyclerView;
    RecyclerViewAdapterSongs recyclerViewAdapterSongs;

    public FragmentSongs() {
    }

    public static FragmentSongs newInstance(int columnCount) {
        return new FragmentSongs();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.actionBar.setTitle(R.string.songs);

        recyclerView = activityMain.findViewById(R.id.recycler_view_song_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(activityMain.songs, this);
        recyclerView.setAdapter(recyclerViewAdapterSongs);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getGroupId()) {
            case RecyclerViewAdapterSongs.MENU_ADD_TO_PLAYLIST_GROUP_ID:
                RecyclerViewAdapterSongs.ViewHolder viewHolder =
                        (RecyclerViewAdapterSongs.ViewHolder)recyclerView.getChildViewHolder(
                                recyclerView.getChildAt(item.getItemId()));
                AudioURI audioURI = viewHolder.audioURI;
                // TODO
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}