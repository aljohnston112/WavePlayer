package com.example.waveplayer.fragments.fragment_select_songs;

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
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.fragments.OnQueryTextListenerSearch;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.R;

import java.util.List;

public class FragmentSelectSongs extends Fragment {

    public static final String NAME = "FragmentSelectSongs";

    private ViewModelActivityMain viewModelActivityMain;

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private OnClickListenerFABFragmentSelectSongs onClickListenerFABFragmentSelectSongs;

    private RecyclerView recyclerViewSongList;

    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private OnQueryTextListenerSearch onQueryTextListenerSearch;

    private BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    // TODO search functionality

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
        viewModelUserPickedSongs =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedSongs.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(view);
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.select_songs));
        updateFAB();
        setUpRecyclerView();
        setUpToolbar();
        setUpBroadcastReceiverServiceConnected();
        setUpBroadcastReceiverOnOptionsMenuCreated();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void setUpToolbar() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            if (itemSearch != null) {
                onQueryTextListenerSearch = new OnQueryTextListenerSearch(activityMain, NAME);
                SearchView searchView = (SearchView) itemSearch.getActionView();
                searchView.setOnQueryTextListener(onQueryTextListenerSearch);
            }
        }
    }

    private void setUpBroadcastReceiverOnOptionsMenuCreated() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu));
        broadcastReceiverOptionsMenuCreated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpToolbar();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOptionsMenuCreated, filterComplete);
    }

    private void updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_white_24dp);
        viewModelActivityMain.setFABText(R.string.fab_done);
        viewModelActivityMain.showFab(true);
        onClickListenerFABFragmentSelectSongs = new OnClickListenerFABFragmentSelectSongs(this);
        viewModelActivityMain.setFabOnClickListener(onClickListenerFABFragmentSelectSongs);
    }

    private void setUpRecyclerView() {
        View view = getView();
        recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
        recyclerViewSongList.setLayoutManager(
                new LinearLayoutManager(recyclerViewSongList.getContext()));
        for (Song song : viewModelUserPickedSongs.getUserPickedSongs()) {
            song.setSelected(true);
        }
        RecyclerViewAdapterSelectSongs recyclerViewAdapterSelectSongs =
                new RecyclerViewAdapterSelectSongs(this);
        recyclerViewSongList.setAdapter(recyclerViewAdapterSelectSongs);
    }

    private void setUpBroadcastReceiverServiceConnected() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpRecyclerView();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        recyclerViewSongList.setAdapter(null);
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        onClickListenerFABFragmentSelectSongs = null;
        viewModelUserPickedSongs = null;
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
        viewModelActivityMain = null;
    }

    public List<Song> getUserPickedSongs() {
        return viewModelUserPickedSongs.getUserPickedSongs();
    }

    public void removeUserPickedSong(Song song) {
        viewModelUserPickedSongs.removeUserPickedSong(song);
    }

    public void addUserPickedSong(Song song) {
        viewModelUserPickedSongs.addUserPickedSong(song);
    }
}