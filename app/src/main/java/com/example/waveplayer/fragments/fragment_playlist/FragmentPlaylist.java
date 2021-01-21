package com.example.waveplayer.fragments.fragment_playlist;

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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.fragments.OnQueryTextListenerSearch;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.fragments.RecyclerViewAdapterSongs;

import java.util.ArrayList;

public class FragmentPlaylist extends Fragment {

    public static final String NAME = "FragmentPlaylist";

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    private RecyclerView recyclerViewSongList;

    private OnClickListenerFABFragmentPlaylist onClickListenerFABFragmentPlaylist;
    private OnQueryTextListenerSearch onQueryTextListenerSearch;
    private ItemTouchListenerSong itemTouchListenerSong;
    private ItemTouchHelper itemTouchHelper;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.hideKeyboard(view);
        setUpToolbar();
        updateFAB();
        RandomPlaylist randomPlaylist = activityMain.getUserPickedPlaylist();
        setUpRecyclerView(randomPlaylist);
        activityMain.isSong(false);
        activityMain.setPlaylistToAddToQueue(randomPlaylist);
        setUpBroadCastReceivers(randomPlaylist);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void setUpToolbar() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        if(toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu != null) {
                menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);
                MenuItem itemSearch = menu.findItem(R.id.action_search);
                if (itemSearch != null) {
                    onQueryTextListenerSearch = new OnQueryTextListenerSearch(activityMain, NAME);
                    SearchView searchView = (SearchView) (itemSearch.getActionView());
                    searchView.setOnQueryTextListener(null);
                    searchView.setOnQueryTextListener(onQueryTextListenerSearch);
                }
            }
        }
    }

    private void updateFAB() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setFabImage(R.drawable.ic_add_black_24dp);
        activityMain.setFABText(R.string.fab_edit);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(null);
        onClickListenerFABFragmentPlaylist = new OnClickListenerFABFragmentPlaylist(this);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentPlaylist);
    }

    private void setUpRecyclerView(RandomPlaylist userPickedPlaylist) {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
        activityMain.setActionBarTitle(userPickedPlaylist.getName());
        recyclerViewSongList.setLayoutManager(
                new LinearLayoutManager(recyclerViewSongList.getContext()));
        RecyclerViewAdapterSongs recyclerViewAdapterSongsList = new RecyclerViewAdapterSongs(
                this, new ArrayList<>(userPickedPlaylist.getSongIDs()));
        recyclerViewSongList.setAdapter(recyclerViewAdapterSongsList);
        itemTouchListenerSong = new ItemTouchListenerSong(activityMain, userPickedPlaylist);
        itemTouchHelper = new ItemTouchHelper(itemTouchListenerSong);
        itemTouchHelper.attachToRecyclerView(recyclerViewSongList);
    }

    private void setUpBroadCastReceivers(RandomPlaylist randomPlaylist) {
        setUpBroadCastReceiverOnServiceConnected(randomPlaylist);
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
    }

    private void setUpBroadcastReceiverServiceOnOptionsMenuCreated() {
        final ActivityMain activityMain = ((ActivityMain) getActivity());
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

    private void setUpBroadCastReceiverOnServiceConnected(final RandomPlaylist randomPlaylist) {
        final ActivityMain activityMain = ((ActivityMain) getActivity());
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpRecyclerView(randomPlaylist);
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        recyclerViewSongList.setAdapter(null);
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        onQueryTextListenerSearch = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        if(toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu != null) {
                MenuItem itemSearch = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) (itemSearch.getActionView());
                searchView.onActionViewCollapsed();
            }
        }
        itemTouchHelper.attachToRecyclerView(null);
        itemTouchListenerSong = null;
        itemTouchHelper = null;
        onClickListenerFABFragmentPlaylist = null;
        activityMain.setPlaylistToAddToQueue(null);
    }

}