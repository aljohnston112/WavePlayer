package com.example.waveplayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class FragmentPlaylist extends Fragment {

    ActivityMain activityMain;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        setUpBroadCastReceiver(view);
        updateFAB();
        setUpUI(view);
    }

    private void setUpBroadCastReceiver(final View view) {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpUI(view);
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void setUpUI(View view) {
        if (activityMain.serviceMain != null) {
            RecyclerView recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
            activityMain.setActionBarTitle(activityMain.serviceMain.userPickedPlaylist.getName());
            recyclerViewSongList.setLayoutManager(new LinearLayoutManager(recyclerViewSongList.getContext()));
            RecyclerViewAdapterSongs recyclerViewAdapterSongsList
                    = new RecyclerViewAdapterSongs(this, new ArrayList<>(
                    activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap().keySet()));
            recyclerViewSongList.setAdapter(recyclerViewAdapterSongsList);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SongItemTouchListener(recyclerViewSongList));
            itemTouchHelper.attachToRecyclerView(recyclerViewSongList);
        }
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

        RecyclerView recyclerView;

        SongItemTouchListener(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            RecyclerViewAdapterSongs recyclerViewAdapterSongsList =
                    (RecyclerViewAdapterSongs) recyclerView.getAdapter();
            if (recyclerViewAdapterSongsList != null) {
                Collections.swap(recyclerViewAdapterSongsList.audioURIS,
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                Map<AudioURI, Double> oldMap =
                        activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap();
                ArrayList<AudioURI> keySetList = new ArrayList<>(oldMap.keySet());
                Collections.swap(keySetList,
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                LinkedHashMap<AudioURI, Double> swappedMap = new LinkedHashMap<>();
                for (AudioURI oldSwappedKey : keySetList) {
                    swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
                }
                activityMain.serviceMain.userPickedPlaylist.getProbFun().setProbMap(swappedMap);
                recyclerViewAdapterSongsList.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            RecyclerViewAdapterSongs recyclerViewAdapterSongsList =
                    (RecyclerViewAdapterSongs) recyclerView.getAdapter();
            if (recyclerViewAdapterSongsList != null) {
                int position = viewHolder.getAdapterPosition();
                activityMain.serviceMain.userPickedPlaylist.getProbFun().remove(
                        recyclerViewAdapterSongsList.audioURIS.get(position));
                recyclerViewAdapterSongsList.audioURIS.remove(position);
                recyclerViewAdapterSongsList.notifyItemRemoved(position);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        activityMain = null;
    }

}