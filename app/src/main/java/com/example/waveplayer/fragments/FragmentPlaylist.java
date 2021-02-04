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
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.R;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.fragments.OnQueryTextListenerSearch;
import com.example.waveplayer.fragments.RecyclerViewAdapterSongs;
import com.example.waveplayer.fragments.FragmentPlaylistDirections;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;

public class FragmentPlaylist extends Fragment implements
        RecyclerViewAdapterSongs.ListenerCallbackSongs {

    public static final String NAME = "FragmentPlaylist";

    private ViewModelActivityMain viewModelActivityMain;

    private BroadcastReceiver broadcastReceiverOnServiceConnected;

    private BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    private RecyclerView recyclerViewSongList;

    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private View.OnClickListener onClickListenerFABFragmentPlaylist;
    private OnQueryTextListenerSearch onQueryTextListenerSearch;
    private ItemTouchHelper.Callback itemTouchListenerSong;
    private ItemTouchHelper itemTouchHelper;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        viewModelUserPickedSongs =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedSongs.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(view);
        setUpToolbar();
        updateFAB();
        RandomPlaylist randomPlaylist = viewModelUserPickedPlaylist.getUserPickedPlaylist();
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
        ActivityMain activityMain = (ActivityMain) requireActivity();
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        if (toolbar != null) {
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
        viewModelActivityMain.setFabImage(R.drawable.ic_add_black_24dp);
        viewModelActivityMain.setFABText(R.string.fab_edit);
        viewModelActivityMain.showFab(true);
        viewModelActivityMain.setFabOnClickListener(null);
        onClickListenerFABFragmentPlaylist = view -> {
            clearUserPickedSongs();
            NavHostFragment.findNavController(this).navigate(
                    FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist());
        };
        viewModelActivityMain.setFabOnClickListener(onClickListenerFABFragmentPlaylist);
    }

    private void setUpRecyclerView(RandomPlaylist userPickedPlaylist) {
        View view = getView();
        recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
        viewModelActivityMain.setActionBarTitle(userPickedPlaylist.getName());
        recyclerViewSongList.setLayoutManager(
                new LinearLayoutManager(recyclerViewSongList.getContext()));
        RecyclerViewAdapterSongs recyclerViewAdapterSongsList = new RecyclerViewAdapterSongs(
                this,
                new ArrayList<>(userPickedPlaylist.getSongs()));
        recyclerViewSongList.setAdapter(recyclerViewAdapterSongsList);
        itemTouchListenerSong = new ItemTouchHelper.Callback(){
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
                RecyclerViewAdapterSongs recyclerViewAdapterSongsList =
                        (RecyclerViewAdapterSongs) recyclerView.getAdapter();
                if (recyclerViewAdapterSongsList != null) {
                    Collections.swap(recyclerViewAdapterSongsList.getSongs(),
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    swapSongPositions(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    recyclerViewAdapterSongsList.notifyItemMoved(
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                ActivityMain activityMain = (ActivityMain) requireActivity();
                int position = viewHolder.getAdapterPosition();
                RecyclerView recyclerView = activityMain.findViewById(R.id.recycler_view_song_list);
                RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                        (RecyclerViewAdapterSongs) recyclerView.getAdapter();
                Song song = recyclerViewAdapterSongs.getSongs().get(position);
                double probability = userPickedPlaylist.getProbability(song);
                if (userPickedPlaylist.size() == 1) {
                    activityMain.removePlaylist(userPickedPlaylist);
                    setUserPickedPlaylist(null);
                } else {
                    userPickedPlaylist.remove(song);
                }
                recyclerViewAdapterSongs.getSongs().remove(position);
                recyclerViewAdapterSongs.notifyItemRemoved(position);
                activityMain.saveFile();
                Snackbar snackbar = Snackbar.make(
                        activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                        R.string.song_removed, BaseTransientBottomBar.LENGTH_LONG);
                View.OnClickListener undoListenerSongRemoved = v -> {
                    userPickedPlaylist.add(song, probability);
                    switchSongPosition(userPickedPlaylist,
                            userPickedPlaylist.size() - 1, position);
                    recyclerViewAdapterSongs.updateList(new ArrayList<>(userPickedPlaylist.getSongs()));
                    recyclerViewAdapterSongs.notifyDataSetChanged();
                    activityMain.saveFile();
                };

                snackbar.setAction(R.string.undo, undoListenerSongRemoved);
                snackbar.show();
            }

            private void switchSongPosition(RandomPlaylist userPickedPlaylist, int oldPosition, int newPosition) {
                userPickedPlaylist.switchSongPositions(oldPosition, newPosition);
            }

            private void swapSongPositions(int oldPosition, int newPosition) {
                ActivityMain activityMain = (ActivityMain) requireActivity();
                userPickedPlaylist.swapSongPositions(oldPosition, newPosition);
                activityMain.saveFile();
            }
        };
        itemTouchHelper = new ItemTouchHelper(itemTouchListenerSong);
        itemTouchHelper.attachToRecyclerView(recyclerViewSongList);
    }

    private void setUpBroadCastReceivers(RandomPlaylist randomPlaylist) {
        setUpBroadCastReceiverOnServiceConnected(randomPlaylist);
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
    }

    private void setUpBroadcastReceiverServiceOnOptionsMenuCreated() {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
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

    private void setUpBroadCastReceiverOnServiceConnected(final RandomPlaylist randomPlaylist) {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiver() {
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
        ActivityMain activityMain = (ActivityMain) requireActivity();
        recyclerViewSongList.setAdapter(null);
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
        itemTouchHelper.attachToRecyclerView(null);
        itemTouchListenerSong = null;
        itemTouchHelper = null;
        onClickListenerFABFragmentPlaylist = null;
        activityMain.setPlaylistToAddToQueue(null);
        viewModelUserPickedPlaylist = null;
        viewModelUserPickedSongs = null;
        viewModelActivityMain = null;
    }

    public RandomPlaylist getUserPickedPlaylist() {
        return viewModelUserPickedPlaylist.getUserPickedPlaylist();
    }

    public void setUserPickedPlaylist(RandomPlaylist randomPlaylist) {
        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist);
    }

    public void clearUserPickedSongs() {
        viewModelUserPickedSongs.clearUserPickedSongs();
    }

    @Override
    public boolean onMenuItemClickAddToPlaylist(Song song) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song);
        bundle.putBoolean(BUNDLE_KEY_IS_SONG, true);
        DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                new DialogFragmentAddToPlaylist();
        dialogFragmentAddToPlaylist.setArguments(bundle);
        dialogFragmentAddToPlaylist.show(getParentFragmentManager(), getTag());
        return true;
    }

    @Override
    public boolean onMenuItemClickAddToQueue(Song song) {
        // TODO fix how MasterPlaylist continues after queue is depleted
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.addToQueue(song.id);
        if (!activityMain.isSongInProgress()) {
            activityMain.showSongPane();
            activityMain.playNext();
        }
        return true;
    }


    @Override
    public void onClickViewHolder(Song song) {
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        if (activityMain.getCurrentAudioUri() != null &&
                song.equals(activityMain.getSong(
                        activityMain.getCurrentAudioUri().id))) {
            activityMain.seekTo(0);
        }
        activityMain.setCurrentPlaylist(getUserPickedPlaylist());
        activityMain.clearSongQueue();
        activityMain.addToQueue(song.id);
        activityMain.playNext();
        NavDirections action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong();
        if (action != null) {
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

}