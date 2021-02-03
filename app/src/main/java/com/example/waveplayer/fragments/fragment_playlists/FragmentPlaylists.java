package com.example.waveplayer.fragments.fragment_playlists;

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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.fragments.OnQueryTextListenerSearch;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

public class FragmentPlaylists extends Fragment {

    public static final String NAME = "FragmentPlaylists";

    private ViewModelActivityMain viewModelActivityMain;
    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;
    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private BroadcastReceiver broadcastReceiverOnServiceConnected;
    private BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    private View.OnClickListener onClickListenerFABFragmentPlaylists;

    private OnQueryTextListenerSearch onQueryTextListenerSearch;

    private ItemTouchHelper itemTouchHelper;
    private ItemTouchHelper.Callback itemTouchListenerPlaylist;

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
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        viewModelUserPickedSongs =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedSongs.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        updateMainContent();
        setUpBroadcastReceiverOnServiceConnected();
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void updateMainContent() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(getView());
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.playlists));
        updateFAB();
        setUpRecyclerView();
        setUpToolbar();
    }

    private void setUpToolbar() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        if (toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu != null) {
                menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);
                MenuItem itemSearch = menu.findItem(R.id.action_search);
                if (itemSearch != null) {
                    onQueryTextListenerSearch = new OnQueryTextListenerSearch(activityMain, NAME);
                    SearchView searchView = (SearchView) (itemSearch.getActionView());
                    searchView.setOnQueryTextListener(onQueryTextListenerSearch);
                }
            }
        }
    }

    private void updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_add_black_24dp);
        viewModelActivityMain.setFABText(R.string.fab_new);
        viewModelActivityMain.showFab(true);
        onClickListenerFABFragmentPlaylists = (View.OnClickListener) view -> {
            // userPickedPlaylist is null when user is making a new playlist
            setUserPickedPlaylist(null);
            clearUserPickedSongs();
            NavHostFragment.findNavController(this).navigate(
                    FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist());
        };
        viewModelActivityMain.setFabOnClickListener(onClickListenerFABFragmentPlaylists);
    }

    private void setUpRecyclerView() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_playlist_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        RecyclerViewAdapterPlaylists recyclerViewAdapter =
                new RecyclerViewAdapterPlaylists(this, activityMain.getPlaylists());
        recyclerView.setAdapter(recyclerViewAdapter);
        itemTouchListenerPlaylist = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return 0;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                RecyclerViewAdapterPlaylists recyclerViewAdapter =
                        (RecyclerViewAdapterPlaylists) recyclerView.getAdapter();
                if (recyclerViewAdapter != null) {
                    Collections.swap(activityMain.getPlaylists(),
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    recyclerViewAdapter.notifyItemMoved(
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    activityMain.saveFile();
                }
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                RecyclerView recyclerView = activityMain.findViewById(R.id.recycler_view_playlist_list);
                RecyclerViewAdapterPlaylists recyclerViewAdapter =
                        (RecyclerViewAdapterPlaylists) recyclerView.getAdapter();
                if (recyclerViewAdapter != null) {
                    int position = viewHolder.getAdapterPosition();
                    List<RandomPlaylist> randomPlaylists = activityMain.getPlaylists();
                    RandomPlaylist randomPlaylist = randomPlaylists.get(position);
                    activityMain.removePlaylist(randomPlaylist);
                    recyclerViewAdapter.notifyItemRemoved(position);
                    activityMain.saveFile();
                    Snackbar snackbar = Snackbar.make(
                            activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                            R.string.playlist_deleted, BaseTransientBottomBar.LENGTH_LONG);
                    View.OnClickListener undoListenerPlaylistRemoved = v -> {
                        activityMain.addPlaylist(position, randomPlaylist);
                        recyclerViewAdapter.notifyItemInserted(position);
                        activityMain.saveFile();
                    };
                    snackbar.setAction(R.string.undo, undoListenerPlaylistRemoved);
                    snackbar.show();
                }
            }
        };
        itemTouchHelper = new ItemTouchHelper(itemTouchListenerPlaylist);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setUpBroadcastReceiverOnServiceConnected() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpRecyclerView();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void setUpBroadcastReceiverServiceOnOptionsMenuCreated() {
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

    @Override
    public void onDestroyView() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        onQueryTextListenerSearch = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        if (toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu != null) {
                MenuItem itemSearch = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) (itemSearch.getActionView());
                searchView.onActionViewCollapsed();
            }
        }
        onClickListenerFABFragmentPlaylists = null;
        itemTouchHelper.attachToRecyclerView(null);
        itemTouchListenerPlaylist = null;
        itemTouchHelper = null;
        viewModelUserPickedPlaylist = null;
        viewModelUserPickedSongs = null;
        viewModelActivityMain = null;
    }

    public void setUserPickedPlaylist(RandomPlaylist randomPlaylist) {
        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist);
    }

    public void clearUserPickedSongs() {
        viewModelUserPickedSongs.clearUserPickedSongs();
    }
}