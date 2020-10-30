package com.example.waveplayer;

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
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FragmentSongs extends Fragment {

    public static final String NAME = "FragmentSongs";

    ActivityMain activityMain;

    View view;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    private OnQueryTextListenerSearch onQueryTextListenerSearch;

    boolean setUp = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.recycler_view_song_list, container, false);
        activityMain = ((ActivityMain) getActivity());
        if (activityMain != null) {
            activityMain.setUserPickedPlaylistToMasterPlaylist();
        }
        hideKeyBoard();
        updateMainContent();
        setUpRecyclerView();
        setUpBroadcastReceiverServiceConnected();
        setUpBroadcastReceiverOnOptionsMenuCreated();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void updateMainContent() {
        activityMain.setActionBarTitle(getResources().getString(R.string.songs));
        activityMain.showFab(false);
        setUpToolbar();
    }

    private void setUpToolbar() {
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            /*
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);

             */
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            if (itemSearch != null) {
                onQueryTextListenerSearch = new OnQueryTextListenerSearch(activityMain, NAME);
                SearchView searchView = (SearchView) itemSearch.getActionView();
                searchView.setOnQueryTextListener(onQueryTextListenerSearch);
            }
        }
    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager)
                activityMain.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setUpBroadcastReceiverServiceConnected() {
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
        if (!setUp) {
            RecyclerView recyclerViewSongs = view.findViewById(R.id.recycler_view_song_list);
            RecyclerViewAdapterSongs recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(
                    this, new ArrayList<>(activityMain.getAllSongs()));
            recyclerViewSongs.setLayoutManager(
                    new LinearLayoutManager(recyclerViewSongs.getContext()));
            recyclerViewSongs.setAdapter(recyclerViewAdapterSongs);
            setUp = true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideKeyBoard();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            /*
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);

             */
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(itemSearch);
            searchView.setOnQueryTextListener(null);
            searchView.onActionViewCollapsed();
        }
        onQueryTextListenerSearch = null;
        setUp = false;
        view = null;
        activityMain = null;
    }

}