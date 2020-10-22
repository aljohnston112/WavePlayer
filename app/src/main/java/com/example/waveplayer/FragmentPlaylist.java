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

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
                swapSongPositions(viewHolder.getAdapterPosition(), target.getAdapterPosition());
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
                ProbFun<AudioURI> probFun = activityMain.serviceMain.userPickedPlaylist.getProbFun();
                AudioURI audioURI = recyclerViewAdapterSongsList.audioURIS.get(position);
                double prob = probFun.getProbMap().get(audioURI);
                probFun.remove(audioURI);
                recyclerViewAdapterSongsList.audioURIS.remove(position);
                recyclerViewAdapterSongsList.notifyItemRemoved(position);
                activityMain.serviceMain.saveFile();
                Snackbar snackbar = Snackbar.make(
                        activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                        R.string.song_removed, BaseTransientBottomBar.LENGTH_LONG);
                snackbar.setAction(R.string.undo,
                        new FragmentPlaylist.UndoListenerSongRemoved(recyclerViewAdapterSongsList,
                                audioURI, prob, position));
                snackbar.show();
            }
        }
    }

    private void switchSongPosition(int oldPosition, int newPosition) {
        Map<AudioURI, Double> oldMap =
                activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap();
        ArrayList<AudioURI> keySetList = new ArrayList<>(oldMap.keySet());
        keySetList.add(newPosition, keySetList.get(oldPosition));
        keySetList.remove(oldPosition+1);
        LinkedHashMap<AudioURI, Double> swappedMap = new LinkedHashMap<>();
        for (AudioURI oldSwappedKey : keySetList) {
            swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
        }
        activityMain.serviceMain.userPickedPlaylist.getProbFun().setProbMap(swappedMap);
    }

    private void swapSongPositions(int oldPosition, int newPosition) {
        Map<AudioURI, Double> oldMap =
                activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap();
        ArrayList<AudioURI> keySetList = new ArrayList<>(oldMap.keySet());
        Collections.swap(keySetList, oldPosition, newPosition);
        LinkedHashMap<AudioURI, Double> swappedMap = new LinkedHashMap<>();
        for (AudioURI oldSwappedKey : keySetList) {
            swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
        }
        activityMain.serviceMain.userPickedPlaylist.getProbFun().setProbMap(swappedMap);
    }

    public class UndoListenerSongRemoved implements View.OnClickListener {

        RecyclerViewAdapterSongs recyclerViewAdapter;

        AudioURI audioURI;

        double probability;

        int position;

        UndoListenerSongRemoved(RecyclerViewAdapterSongs recyclerViewAdapter, AudioURI audioURI,
                                double probability, int position) {
            this.recyclerViewAdapter = recyclerViewAdapter;
            this.audioURI = audioURI;
            this.probability = probability;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            activityMain.serviceMain.userPickedPlaylist.getProbFun().add(audioURI, probability);
            switchSongPosition(
                    activityMain.serviceMain.userPickedPlaylist.getProbFun().probMap.keySet().size()-1,
                    position);
            recyclerViewAdapter.updateList(new ArrayList<>(
                    activityMain.serviceMain.userPickedPlaylist.getProbFun().probMap.keySet()));
            recyclerViewAdapter.notifyDataSetChanged();
            activityMain.serviceMain.saveFile();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        activityMain = null;
    }

}