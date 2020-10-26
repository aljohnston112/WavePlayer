package com.example.waveplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;

public class FragmentPlaylists extends Fragment {

    public static final String NAME = "FragmentPlaylists";

    ActivityMain activityMain;

    View view;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    OnClickListenerFABFragmentPlaylists onClickListenerFABFragmentPlaylists;

    ItemTouchHelper itemTouchHelper;
    ItemTouchListenerPlaylist itemTouchListenerPlaylist;
    UndoListenerPlaylistRemoved undoListenerPlaylistRemoved;

    RecyclerView recyclerView;

    boolean setUp = false;

    Snackbar snackbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activityMain = ((ActivityMain) getActivity());
        view = inflater.inflate(R.layout.recycler_view_playlist_list, container, false);
        onClickListenerFABFragmentPlaylists = new OnClickListenerFABFragmentPlaylists(this);
        hideKeyBoard();
        updateMainContent();
        setUpBroadcastReceiverOnServiceConnected();
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
        // TODO set up searching
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void updateMainContent() {
        activityMain.setActionBarTitle(getResources().getString(R.string.playlists));
        updateFAB();
        setUpRecyclerView();
    }

    private void updateFAB() {
        activityMain.setFabImage(R.drawable.ic_add_black_24dp);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentPlaylists);
    }

    private void setUpRecyclerView() {
        if (activityMain.serviceMain != null && !setUp) {
            recyclerView = view.findViewById(R.id.recycler_view_playlist_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
            RecyclerViewAdapterPlaylists recyclerViewAdapter =
                    new RecyclerViewAdapterPlaylists(
                            this, activityMain.serviceMain.playlists);
            recyclerView.setAdapter(recyclerViewAdapter);
            itemTouchListenerPlaylist = new ItemTouchListenerPlaylist();
            itemTouchHelper = new ItemTouchHelper(itemTouchListenerPlaylist);
            itemTouchHelper.attachToRecyclerView(recyclerView);
            setUp = true;
        }
    }

    class ItemTouchListenerPlaylist extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            RecyclerViewAdapterPlaylists recyclerViewAdapter =
                    (RecyclerViewAdapterPlaylists) recyclerView.getAdapter();
            if (recyclerViewAdapter != null) {
                Collections.swap(recyclerViewAdapter.randomPlaylists,
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                Collections.swap(activityMain.serviceMain.playlists,
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                recyclerViewAdapter.notifyItemMoved(
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            recyclerView = activityMain.findViewById(R.id.recycler_view_playlist_list);
            RecyclerViewAdapterPlaylists recyclerViewAdapter =
                    (RecyclerViewAdapterPlaylists) recyclerView.getAdapter();
            if (recyclerViewAdapter != null) {
                int position = viewHolder.getAdapterPosition();
                RandomPlaylist randomPlaylist = activityMain.serviceMain.playlists.get(position);
                activityMain.serviceMain.playlists.remove(position);
                boolean isDirectoryPlaylist = false;
                if (activityMain.serviceMain.directoryPlaylists.containsValue(randomPlaylist)) {
                    isDirectoryPlaylist = true;
                    activityMain.serviceMain.directoryPlaylists.remove(randomPlaylist.mediaStoreUriID);
                }
                recyclerViewAdapter.notifyItemRemoved(position);
                activityMain.serviceMain.saveFile();
                snackbar = Snackbar.make(
                        activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                        R.string.playlist_deleted, BaseTransientBottomBar.LENGTH_LONG);
                undoListenerPlaylistRemoved = new UndoListenerPlaylistRemoved(
                        recyclerViewAdapter, randomPlaylist, position,
                        isDirectoryPlaylist, randomPlaylist.mediaStoreUriID);
                snackbar.setAction(R.string.undo, undoListenerPlaylistRemoved);
                snackbar.show();
            }
        }

    }

    public class UndoListenerPlaylistRemoved implements View.OnClickListener {

        RecyclerViewAdapterPlaylists recyclerViewAdapter;
        RandomPlaylist randomPlaylist;
        int position;
        boolean isDirectoryPlaylist;
        long uriID;

        UndoListenerPlaylistRemoved(RecyclerViewAdapterPlaylists recyclerViewAdapter, RandomPlaylist randomPlaylist,
                                    int position, boolean isDirectoryPlaylist, long uriId) {
            this.recyclerViewAdapter = recyclerViewAdapter;
            this.randomPlaylist = randomPlaylist;
            this.position = position;
            this.isDirectoryPlaylist = isDirectoryPlaylist;
            this.uriID = uriId;
        }

        @Override
        public void onClick(View v) {
            activityMain.serviceMain.playlists.add(position, randomPlaylist);
            if (isDirectoryPlaylist) {
                activityMain.serviceMain.directoryPlaylists.put(uriID, randomPlaylist);
            }
            recyclerViewAdapter.notifyItemInserted(position);
            activityMain.serviceMain.saveFile();
        }
    }

    private void setUpBroadcastReceiverOnServiceConnected() {
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
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_on_create_options_menu));
        broadcastReceiverOptionsMenuCreated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
                Menu menu = toolbar.getMenu();
                menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);
            }
        };
        activityMain.registerReceiver(broadcastReceiverOptionsMenuCreated, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        setUp = false;
        onClickListenerFABFragmentPlaylists = null;
        itemTouchHelper.attachToRecyclerView(null);
        itemTouchListenerPlaylist = null;
        itemTouchHelper = null;
        if(snackbar != null) {
            snackbar.setAction(null, null);
        }
        undoListenerPlaylistRemoved = null;
        snackbar = null;
        view = null;
        activityMain = null;
    }

}