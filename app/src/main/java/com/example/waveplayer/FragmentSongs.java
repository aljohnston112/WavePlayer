package com.example.waveplayer;

import android.app.Activity;
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
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FragmentSongs extends Fragment {

    public static final String NAME = "FragmentSongs";

    ActivityMain activityMain;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    private onQueryTextListenerSearch onQueryTextListenerSearch;
    
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
        hideKeyBoard(view);
        updateMainContent();
        setUpToolbar();
        setUpRecyclerView(view);
        setUpBroadcastReceiverOnCompletion(view);
        setUpBroadcastReceiverOnOptionsMenuCreated();
    }

    private void updateMainContent() {
        activityMain.setActionBarTitle(getResources().getString(R.string.songs));
        activityMain.showFab(false);
    }

    private void setUpToolbar() {
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if(menu != null) {
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            if (itemSearch != null) {
                onQueryTextListenerSearch = new onQueryTextListenerSearch(activityMain, NAME);
                SearchView searchView = (SearchView) itemSearch.getActionView();
                searchView.setOnQueryTextListener(onQueryTextListenerSearch);
            }
        }
    }

    private void hideKeyBoard(View view) {
        InputMethodManager imm = (InputMethodManager)
                activityMain.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setUpBroadcastReceiverOnCompletion(final View view) {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected =
                new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                    setUpRecyclerView(view);
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
        hideKeyBoard(getView());
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        activityMain = null;
        onQueryTextListenerSearch = null;
    }

}