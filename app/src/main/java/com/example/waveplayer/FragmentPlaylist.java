package com.example.waveplayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.Collections;
import java.util.LinkedHashMap;

public class FragmentPlaylist extends Fragment {

    ActivityMain activityMain;

    RecyclerView recyclerViewSongList;

    RecyclerViewAdapterSongs recyclerViewAdapterSongsList;

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
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO
        activityMain = ((ActivityMain) getActivity());
        if(activityMain != null) {
            updateFAB();
            if(activityMain.serviceMain != null) {
                setUpUI(view);
            }
        }
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction("ServiceConnected");
        activityMain.registerReceiver(new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpUI(view);
            }
        }, filterComplete);
    }

    private void setUpUI(View view) {
        activityMain.setActionBarTitle(activityMain.serviceMain.userPickedPlaylist.getName());
        recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
        recyclerViewSongList.setLayoutManager(new LinearLayoutManager(recyclerViewSongList.getContext()));
        recyclerViewAdapterSongsList = new RecyclerViewAdapterSongs(this, new ArrayList<>(
                activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap().keySet()));
        recyclerViewSongList.setAdapter(recyclerViewAdapterSongsList);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SongItemTouchListener());
        itemTouchHelper.attachToRecyclerView(recyclerViewSongList);
    }

    private void updateFAB() {
        activityMain.setFabImage(R.drawable.ic_add_black_24dp);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                activityMain.serviceMain.userPickedSongs.clear();
                NavHostFragment.findNavController(FragmentPlaylist.this)
                        .navigate(FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSelectSongs());
            }
        });
    }

    class SongItemTouchListener extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            Collections.swap(recyclerViewAdapterSongsList.audioURIS, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            ArrayList<AudioURI> keySetList = new ArrayList<>();
            LinkedHashMap<AudioURI, Double> oldMap = activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap();
            keySetList.addAll(oldMap.keySet());
            Collections.swap(keySetList, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            LinkedHashMap<AudioURI, Double> swappedMap = new LinkedHashMap<>();
            for(AudioURI oldSwappedKey:keySetList) {
                swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
            }
            activityMain.serviceMain.userPickedPlaylist.getProbFun().setProbMap(swappedMap);
            recyclerViewAdapterSongsList.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            activityMain.serviceMain.userPickedPlaylist.getProbFun().remove(
                    recyclerViewAdapterSongsList.audioURIS.get(position));
            recyclerViewAdapterSongsList.audioURIS.remove(position);
            recyclerViewAdapterSongsList.notifyItemRemoved(position);
        }

    }

}