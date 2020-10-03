package com.example.waveplayer;

import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

public class FragmentPlaylist extends Fragment {

    ActivityMain activityMain;

    RecyclerView recyclerView;

    public FragmentPlaylist() {
        // Required empty public constructor
    }

    public static FragmentPlaylist newInstance() {
        return new FragmentPlaylist();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO
        activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(activityMain.currentPlaylist.getName());
        activityMain.setFabImage(R.drawable.ic_add_black_24dp);
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                activityMain.userPickedSongs = null;
                NavHostFragment.findNavController(FragmentPlaylist.this)
                        .navigate(FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSelectSongs());
            }
        });

        recyclerView = activityMain.findViewById(R.id.recycler_view_song_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(new RecyclerViewAdapterSongs(new ArrayList<>(
                        activityMain.currentPlaylist.getProbFun().getProbMap().keySet()), this));
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SongItemTouchListener());
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    class SongItemTouchListener extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            Collections.swap(((RecyclerViewAdapterSongs)recyclerView.getAdapter()).audioURIS, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            ((RecyclerViewAdapterSongs)recyclerView.getAdapter()).notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            ArrayList<AudioURI> keySetList = new ArrayList<>();
            LinkedHashMap<AudioURI, Double> oldMap = activityMain.currentPlaylist.getProbFun().getProbMap();
            keySetList.addAll(oldMap.keySet());
            Collections.swap(keySetList, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            LinkedHashMap<AudioURI, Double> swappedMap = new LinkedHashMap<>();
            for(AudioURI oldSwappedKey:keySetList) {
                swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
            }
            activityMain.currentPlaylist.getProbFun().setProbMap(swappedMap);
        return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }

    }

}