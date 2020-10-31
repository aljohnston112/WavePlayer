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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;

public class FragmentPlaylists extends Fragment {

    public static final String NAME = "FragmentPlaylists";

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;
    BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    OnClickListenerFABFragmentPlaylists onClickListenerFABFragmentPlaylists;
    private OnQueryTextListenerSearch onQueryTextListenerSearch;
    ItemTouchHelper itemTouchHelper;
    ItemTouchListenerPlaylist itemTouchListenerPlaylist;
    UndoListenerPlaylistRemoved undoListenerPlaylistRemoved;

    RecyclerView recyclerView;

    boolean setUp = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view_playlist_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideKeyBoard();
        updateMainContent();
        setUpBroadcastReceiverOnServiceConnected();
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
    }

    private void updateMainContent() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.playlists));
        updateFAB();
        setUpRecyclerView();
        setUpToolbar();
    }


    private void setUpToolbar() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
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
                SearchView searchView = (SearchView) (itemSearch.getActionView());
                searchView.setOnQueryTextListener(onQueryTextListenerSearch);
            }
        }
    }

    private void hideKeyBoard() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void updateFAB() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setFabImage(R.drawable.ic_add_black_24dp);
        activityMain.setFABText(R.string.fab_new);
        activityMain.showFab(true);
        onClickListenerFABFragmentPlaylists = new OnClickListenerFABFragmentPlaylists(this);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentPlaylists);
    }

    private void setUpRecyclerView() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        if (!setUp) {
            recyclerView = view.findViewById(R.id.recycler_view_playlist_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
            RecyclerViewAdapterPlaylists recyclerViewAdapter =
                    new RecyclerViewAdapterPlaylists(this, activityMain.getPlaylists());
            recyclerView.setAdapter(recyclerViewAdapter);
            itemTouchListenerPlaylist = new ItemTouchListenerPlaylist(activityMain);
            itemTouchHelper = new ItemTouchHelper(itemTouchListenerPlaylist);
            itemTouchHelper.attachToRecyclerView(recyclerView);
            //setUp = true;
        }
    }

    private void setUpBroadcastReceiverOnServiceConnected() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
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

    private void setUpBroadcastReceiverServiceOnOptionsMenuCreated() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_on_create_options_menu));
        broadcastReceiverOptionsMenuCreated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /*
                Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
                Menu menu = toolbar.getMenu();
                menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);

                 */
                setUpToolbar();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOptionsMenuCreated, filterComplete);
    }

    @Override
    public void onDestroyView() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        onQueryTextListenerSearch = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            /*
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);

             */
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) (itemSearch.getActionView());
            searchView.onActionViewCollapsed();
        }
        setUp = false;
        onClickListenerFABFragmentPlaylists = null;
        itemTouchHelper.attachToRecyclerView(null);
        itemTouchListenerPlaylist = null;
        itemTouchHelper = null;
        undoListenerPlaylistRemoved = null;
    }

}