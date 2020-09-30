package com.example.waveplayer;

import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import androidx.fragment.app.Fragment;

import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FragmentPlaylists extends Fragment {

    RecyclerView recyclerView;

    public FragmentPlaylists() {
    }

    public static FragmentPlaylists newInstance(int columnCount) {
        return new FragmentPlaylists();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.playlists));

        activityMain.setFabImage(getResources().getDrawable(R.drawable.ic_add_black_24dp));
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FragmentPlaylists.this)
                        .navigate(FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist());
            }
        });

        recyclerView = activityMain.findViewById(R.id.recycler_view_playlist_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(new RecyclerViewAdapterPlaylists(activityMain.playlists, this));
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getGroupId()) {
            case RecyclerViewAdapterPlaylists.MENU_EDIT_PLAYLIST_GROUP_ID:
                RecyclerViewAdapterPlaylists.ViewHolder viewHolder =
                        (RecyclerViewAdapterPlaylists.ViewHolder) recyclerView.getChildViewHolder(
                                recyclerView.getChildAt(item.getItemId()));
                RandomPlaylist randomPlaylist = viewHolder.randomPlaylist;
                // TODO
                return true;
            case RecyclerViewAdapterPlaylists.MENU_DELETE_PLAYLIST_GROUP_ID:
                //TODO
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}