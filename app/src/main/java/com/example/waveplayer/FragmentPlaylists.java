package com.example.waveplayer;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A fragment representing a list of Items.
 */
public class FragmentPlaylists extends Fragment {

    RecyclerView recyclerView;

    ArrayList<AudioURI> arrayListSongs;
    ArrayList<RandomPlaylist> arrayListPlayList;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FragmentPlaylists() {
    }

    @SuppressWarnings("unused")
    public static FragmentPlaylists newInstance(int columnCount) {
        FragmentPlaylists fragment = new FragmentPlaylists();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        arrayListPlayList = new ArrayList<RandomPlaylist>();
        if(getArguments() != null) {
            arrayListPlayList = FragmentPlaylistsArgs.fromBundle(getArguments()).getListPlaylists();
            arrayListSongs = FragmentPlaylistsArgs.fromBundle(getArguments()).getListSongs();
        }
        recyclerView = (RecyclerView)
                inflater.inflate(R.layout.fragment_playlist_list, container, false);
        Context context = recyclerView.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new RecyclerViewAdapterPlaylists(arrayListPlayList, this));
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.playlists);
        FloatingActionButton fab = Objects.requireNonNull(getActivity()).findViewById(R.id.fab);
        fab.show();
        fab.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                FragmentPlaylistsDirections.ActionFragmentPlaylistsToFragmentEditPlaylist action =
                        FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist(
                                null, arrayListSongs);
                NavHostFragment.findNavController(FragmentPlaylists.this)
                        .navigate(action);
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getGroupId()) {
            case RecyclerViewAdapterPlaylists.MENU_EDIT_PLAYLIST_GROUP_ID:
                RecyclerViewAdapterSongs.ViewHolder viewHolder =
                        (RecyclerViewAdapterSongs.ViewHolder)recyclerView.getChildViewHolder(
                                recyclerView.getChildAt(item.getItemId()));
                AudioURI audioURI = viewHolder.audioURI;
                return true;
                case RecyclerViewAdapterPlaylists.MENU_DELETE_PLAYLIST_GROUP_ID:

                    return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}