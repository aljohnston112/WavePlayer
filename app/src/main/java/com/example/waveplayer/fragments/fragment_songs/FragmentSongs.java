package com.example.waveplayer.fragments.fragment_songs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.fragments.OnQueryTextListenerSearch;
import com.example.waveplayer.R;
import com.example.waveplayer.fragments.RecyclerViewAdapterSongs;

import java.util.ArrayList;
import java.util.List;

public class FragmentSongs extends Fragment {

    public static final String NAME = "FragmentSongs";

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    private OnQueryTextListenerSearch onQueryTextListenerSearch;

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
        ViewModelUserPickedPlaylist viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.hideKeyboard(view);
        updateMainContent();
        setUpRecyclerView();
        viewModelUserPickedPlaylist.setUserPickedPlaylist(MediaData.getInstance(activityMain).getMasterPlaylist());
        setUpBroadcastReceiverServiceConnected();
        setUpBroadcastReceiverOnOptionsMenuCreated();
    }

    private void updateMainContent() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.songs));
        activityMain.showFab(false);
        setUpToolbar();
    }

    private void setUpToolbar() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            if (itemSearch != null) {
                onQueryTextListenerSearch = new OnQueryTextListenerSearch(activityMain, NAME);
                SearchView searchView = (SearchView) itemSearch.getActionView();
                searchView.setOnQueryTextListener(onQueryTextListenerSearch);
            }
        }
    }

    private void setUpBroadcastReceiverServiceConnected() {
        final ActivityMain activityMain = ((ActivityMain) getActivity());
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected =
                new BroadcastReceiverOnServiceConnected() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        setUpRecyclerView();
                        activityMain.updateUI();
                    }
                };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void setUpBroadcastReceiverOnOptionsMenuCreated() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_on_create_options_menu));
        broadcastReceiverOptionsMenuCreated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpToolbar();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOptionsMenuCreated, filterComplete);
    }


    private void setUpRecyclerView() {
            ActivityMain activityMain = ((ActivityMain) getActivity());
            View view = getView();
            RecyclerView recyclerViewSongs = view.findViewById(R.id.recycler_view_song_list);
            List<Song> songs = activityMain.getAllSongs();
            if(songs != null) {
                RecyclerViewAdapterSongs recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(
                        this, new ArrayList<>(songs));
                recyclerViewSongs.setLayoutManager(
                        new LinearLayoutManager(recyclerViewSongs.getContext()));
                recyclerViewSongs.setAdapter(recyclerViewAdapterSongs);
            }
        }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        activityMain.hideKeyboard(view);
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) itemSearch.getActionView();
            searchView.setOnQueryTextListener(null);
            searchView.onActionViewCollapsed();
        }
        onQueryTextListenerSearch = null;
    }

}