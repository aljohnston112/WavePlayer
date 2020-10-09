package com.example.waveplayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    ActivityMain activityMain;

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
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        if(activityMain.serviceMain != null) {
            activityMain.serviceMain.userPickedPlaylist = activityMain.serviceMain.masterPlaylist;
        }
        updateMainContent();
        if(activityMain.serviceMain != null) {
            setUpRecyclerView(view);
        }
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction("ServiceConnected");
        activityMain.registerReceiver(new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpRecyclerView(view);
                activityMain.updateSongPaneUI();
            }
        }, filterComplete);
    }

    private void updateMainContent() {
        activityMain.setActionBarTitle(getResources().getString(R.string.songs));
        activityMain.showFab(false);
    }

    private void setUpRecyclerView(View view) {
        RecyclerView recyclerViewSongs = view.findViewById(R.id.recycler_view_song_list);
        RecyclerViewAdapterSongs recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(
                this, activityMain.serviceMain.songs);
        recyclerViewSongs.setLayoutManager(new LinearLayoutManager(recyclerViewSongs.getContext()));
        recyclerViewSongs.setAdapter(recyclerViewAdapterSongs);
    }

}