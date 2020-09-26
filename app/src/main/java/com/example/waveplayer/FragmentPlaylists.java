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
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
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
        ArrayList arrayList = new ArrayList<AudioURI>();
        if(getArguments() != null) {
            arrayList = FragmentPlaylistsArgs.fromBundle(getArguments()).getListPlaylists();
        }
        recyclerView = (RecyclerView)
                inflater.inflate(R.layout.fragment_playlist_list, container, false);
        Context context = recyclerView.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new RecyclerViewAdapterPlaylists(arrayList, this));
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