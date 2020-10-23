package com.example.waveplayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FragmentSongs extends Fragment {

    ActivityMain activityMain;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        if(activityMain != null && activityMain.serviceMain != null) {
            activityMain.serviceMain.userPickedPlaylist = activityMain.serviceMain.masterPlaylist;
        }
        updateMainContent();
        setUpRecyclerView(view);
        setUpBroadcastReceiver(view);
        setUpSearchPane();
    }

    private void setUpSearchPane(){
        SearchView searchView = activityMain.findViewById(R.id.search_view_search);
        /*
        ImageView icon = searchView.findViewById(androidx.appcompat.);
        Drawable whiteIcon = icon.getDrawable();
        whiteIcon.setTint(Color.BLACK);
        icon.setImageDrawable(whiteIcon);
         */
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                RecyclerView recyclerViewSongs = activityMain.findViewById(R.id.recycler_view_playlist_list);
                if(recyclerViewSongs == null){
                    recyclerViewSongs = activityMain.findViewById(R.id.recycler_view_song_list);
                }
                RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                        (RecyclerViewAdapterSongs)recyclerViewSongs.getAdapter();
                if(recyclerViewAdapterSongs.audioURIS.size() ==
                        activityMain.serviceMain.masterPlaylist.getProbFun().size()) {
                    List<AudioURI> audioURIS = new ArrayList<>(
                                    activityMain.serviceMain.masterPlaylist.getProbFun().getProbMap().keySet());
                    List<AudioURI> sifted = new ArrayList<>();
                    if (!newText.equals("")) {
                        for(AudioURI audioURI : audioURIS){
                            if(audioURI.title.toLowerCase().contains(newText.toLowerCase())){
                                sifted.add(audioURI);
                            }
                        }
                        recyclerViewAdapterSongs.updateList(sifted);
                        return true;
                    } else{
                        recyclerViewAdapterSongs.updateList(audioURIS);
                        return true;
                    }
                } else {
                    List<AudioURI> audioURIS = new ArrayList<>(
                            activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap().keySet());
                    List<AudioURI> sifted = new ArrayList<>();
                    if (!newText.equals("")) {
                        for(AudioURI audioURI : audioURIS){
                            if(audioURI.title.toLowerCase().contains(newText.toLowerCase())){
                                sifted.add(audioURI);
                            }
                        }
                        recyclerViewAdapterSongs.updateList(sifted);
                        return true;
                    } else{
                        recyclerViewAdapterSongs.updateList(audioURIS);
                        return true;
                    }
                }
            }
        });
    }

    private void doneWithSearchPane(){
        // TODO
        FragmentManager fragmentManager = getParentFragmentManager();
        int count = fragmentManager.getBackStackEntryCount();
        String tag = fragmentManager.getBackStackEntryAt(count - 2).getName();
        if(tag.equals("FragmentTitle")) {
            //activityMain.serviceMain.masterPlaylist.getProbFun().getProbMap().keySet();
        } else {
            //activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap().keySet()
        }

    }

    private void setUpBroadcastReceiver(final View view) {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                    setUpRecyclerView(view);
                    activityMain.updateSongPaneUI();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void updateMainContent() {
        activityMain.setActionBarTitle(getResources().getString(R.string.songs));
        activityMain.showFab(false);
    }

    private void setUpRecyclerView(View view) {
        if(activityMain.serviceMain != null) {
            RecyclerView recyclerViewSongs = view.findViewById(R.id.recycler_view_song_list);
            RecyclerViewAdapterSongs recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(
                    this, new ArrayList<>(
                    activityMain.serviceMain.masterPlaylist.getProbFun().getProbMap().keySet()));
            recyclerViewSongs.setLayoutManager(new LinearLayoutManager(recyclerViewSongs.getContext()));
            recyclerViewSongs.setAdapter(recyclerViewAdapterSongs);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        activityMain = null;
    }

}