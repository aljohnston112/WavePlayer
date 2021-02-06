package com.example.waveplayer.fragments;

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
import com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.databinding.RecyclerViewPlaylistListBinding;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.Song;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;

public class FragmentPlaylists extends Fragment
        implements RecyclerViewAdapterPlaylists.ListenerCallbackPlaylists {

    private RecyclerViewPlaylistListBinding binding;

    private ViewModelActivityMain viewModelActivityMain;
    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;
    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private BroadcastReceiver broadcastReceiver;

    private RecyclerView recyclerViewPlaylists;
    private RecyclerViewAdapterPlaylists recyclerViewAdapterPlaylists;

    private View.OnClickListener onClickListenerFAB;

    private SearchView.OnQueryTextListener onQueryTextListenerSearch;

    private ItemTouchHelper itemTouchHelper;
    private ItemTouchHelper.Callback itemTouchHelperCallback;
    private View.OnClickListener undoListenerPlaylistRemoved;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setUpViewModels();
        binding = RecyclerViewPlaylistListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void setUpViewModels() {
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        viewModelUserPickedSongs =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedSongs.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(requireView());
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.playlists));
        setUpBroadcastReceiver();
        setUpRecyclerView();
    }

    private void setUpBroadcastReceiver() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        intentFilter.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu));
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_on_create_options_menu))) {
                        setUpToolbar();
                    } else if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_service_connected))) {
                        setUpRecyclerView();
                    }
                }
            }
        };

        activityMain.registerReceiver(broadcastReceiver, intentFilter);
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
                    onQueryTextListenerSearch = new SearchView.OnQueryTextListener() {

                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            return false;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            // TODO fix bug where you can reorder songs when sifted
                            recyclerViewPlaylists = binding.recyclerViewPlaylistList;
                            recyclerViewAdapterPlaylists =
                                    (RecyclerViewAdapterPlaylists) recyclerViewPlaylists.getAdapter();
                            List<RandomPlaylist> playlists = activityMain.getPlaylists();
                            List<RandomPlaylist> sifted = new ArrayList<>();
                            if (!newText.equals("")) {
                                for (RandomPlaylist randomPlaylist : playlists) {
                                    if (randomPlaylist.getName().toLowerCase().contains(newText.toLowerCase())) {
                                        sifted.add(randomPlaylist);
                                    }
                                }
                                recyclerViewAdapterPlaylists.updateList(sifted);
                            } else {
                                recyclerViewAdapterPlaylists.updateList(playlists);
                            }
                            return true;
                        }
                    };
                    SearchView searchView = (SearchView) (itemSearch.getActionView());
                    searchView.setOnQueryTextListener(onQueryTextListenerSearch);
                }
            }
        }
    }

    private void setUpRecyclerView() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        RecyclerView recyclerView = binding.recyclerViewPlaylistList;
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        RecyclerViewAdapterPlaylists recyclerViewAdapter =
                new RecyclerViewAdapterPlaylists(this, activityMain.getPlaylists());
        recyclerView.setAdapter(recyclerViewAdapter);
        itemTouchHelperCallback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                    Collections.swap(activityMain.getPlaylists(),
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    recyclerViewAdapter.notifyItemMoved(
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    activityMain.saveFile();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getAdapterPosition();
                    List<RandomPlaylist> randomPlaylists = activityMain.getPlaylists();
                    RandomPlaylist randomPlaylist = randomPlaylists.get(position);
                    activityMain.removePlaylist(randomPlaylist);
                    recyclerViewAdapter.notifyItemRemoved(position);
                    activityMain.saveFile();
                    Snackbar snackbar = Snackbar.make(binding.recyclerViewPlaylistList,
                            R.string.playlist_deleted, BaseTransientBottomBar.LENGTH_LONG);
                    undoListenerPlaylistRemoved = v -> {
                        activityMain.addPlaylist(position, randomPlaylist);
                        recyclerViewAdapter.notifyItemInserted(position);
                        activityMain.saveFile();
                    };
                    snackbar.setAction(R.string.undo, undoListenerPlaylistRemoved);
                    snackbar.show();
                }
        };
        itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
        setUpToolbar();
    }

    private void updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_add_black_24dp);
        viewModelActivityMain.setFABText(R.string.fab_new);
        viewModelActivityMain.showFab(true);
        onClickListenerFAB = (View.OnClickListener) view -> {
            // userPickedPlaylist is null when user is making a new playlist
            viewModelUserPickedPlaylist.setUserPickedPlaylist(null);
            viewModelUserPickedSongs.clearUserPickedSongs();
            NavHostFragment.findNavController(this).navigate(
                    FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist());
        };
        viewModelActivityMain.setFabOnClickListener(onClickListenerFAB);
    }

    @Override
    public boolean onMenuItemClickAddToPlaylist(RandomPlaylist randomPlaylist) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, randomPlaylist);
        bundle.putSerializable(BUNDLE_KEY_IS_SONG, false);
        DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist = new DialogFragmentAddToPlaylist();
        dialogFragmentAddToPlaylist.setArguments(bundle);
        dialogFragmentAddToPlaylist.show(getParentFragmentManager(), getTag());
        return true;
    }

    @Override
    public boolean onMenuItemClickAddToQueue(RandomPlaylist randomPlaylist) {
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        for (Song song : randomPlaylist.getSongs()) {
            activityMain.addToQueue(song.id);
        }
        // TODO stop MasterPlaylist from continuing after queue is done
        // shuffle is off and looping is on or something like that?
        activityMain.setCurrentPlaylistToMaster();
        if (!activityMain.isSongInProgress()) {
            activityMain.showSongPane();
            // TODO goToFrontOfQueue() is dumb
            activityMain.goToFrontOfQueue();
            activityMain.playNext();
        }
        return true;
    }

    @Override
    public void onClickViewHolder(RandomPlaylist randomPlaylist) {
        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist);
        NavHostFragment.findNavController(this).navigate(
                FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist());
    }

    @Override
    public void onDestroyView() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        super.onDestroyView();
        itemTouchHelper.attachToRecyclerView(null);
        itemTouchHelperCallback = null;
        itemTouchHelper = null;
        undoListenerPlaylistRemoved = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        if (toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu != null) {
                MenuItem itemSearch = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) (itemSearch.getActionView());
                searchView.setOnQueryTextListener(null);
                searchView.onActionViewCollapsed();
            }
        }
        onQueryTextListenerSearch = null;
        viewModelActivityMain.setFabOnClickListener(null);
        onClickListenerFAB = null;
        recyclerViewPlaylists.setAdapter(null);
        recyclerViewAdapterPlaylists = null;
        recyclerViewPlaylists = null;
        activityMain.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        viewModelUserPickedPlaylist = null;
        viewModelUserPickedSongs = null;
        viewModelActivityMain = null;
        binding = null;
    }

}