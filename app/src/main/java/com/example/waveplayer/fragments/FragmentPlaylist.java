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
import androidx.fragment.app.DialogFragment;
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
import com.example.waveplayer.databinding.RecyclerViewSongListBinding;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;

public class FragmentPlaylist extends Fragment
        implements RecyclerViewAdapterSongs.ListenerCallbackSongs {

    private RecyclerViewSongListBinding binding;

    private ViewModelActivityMain viewModelActivityMain;
    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;
    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private BroadcastReceiver broadcastReceiver;

    private RecyclerView recyclerViewSongList;
    private RecyclerViewAdapterSongs recyclerViewAdapterSongs;

    private View.OnClickListener onClickListenerFAB;
    private SearchView.OnQueryTextListener onQueryTextListener;

    private ItemTouchHelper.Callback itemTouchHelperCallback;
    private ItemTouchHelper itemTouchHelper;
    private View.OnClickListener undoListenerSongRemoved;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        createViewModels();
        binding = RecyclerViewSongListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void createViewModels() {
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        viewModelUserPickedSongs =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedSongs.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(view);
        RandomPlaylist randomPlaylist = viewModelUserPickedPlaylist.getUserPickedPlaylist();
        viewModelActivityMain.setPlaylistToAddToQueue(randomPlaylist);
        viewModelActivityMain.setActionBarTitle(randomPlaylist.getName());
        setUpBroadcastReceiver(randomPlaylist);
        setUpRecyclerView(randomPlaylist);
    }

    private void setUpBroadcastReceiver(final RandomPlaylist randomPlaylist) {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu));
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
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
                        setUpRecyclerView(randomPlaylist);
                    }
                }
            }
        };
        activityMain.registerReceiver(broadcastReceiver, filterComplete);
    }

    private void setUpToolbar() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        if (toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu != null) {
                menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_LOWER_PROBS_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);
                MenuItem itemSearch = menu.findItem(R.id.action_search);
                if (itemSearch != null) {
                    onQueryTextListener = new SearchView.OnQueryTextListener() {

                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            return false;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            // TODO fix bug where you can reorder songs when sifted
                            List<Song> songs = activityMain.getUserPickedPlaylist().getSongs();
                            List<Song> sifted = new ArrayList<>();
                            if (!newText.equals("")) {
                                for (Song song : songs) {
                                    if (song.title.toLowerCase().contains(newText.toLowerCase())) {
                                        sifted.add(song);
                                    }
                                }
                                recyclerViewAdapterSongs.updateList(sifted);
                            } else {
                                recyclerViewAdapterSongs.updateList(songs);
                            }
                            return true;
                        }
                    };
                    SearchView searchView = (SearchView) (itemSearch.getActionView());
                    searchView.setOnQueryTextListener(null);
                    searchView.setOnQueryTextListener(onQueryTextListener);
                }
            }
        }
    }

    private void setUpRecyclerView(RandomPlaylist userPickedPlaylist) {
        recyclerViewSongList = binding.recyclerViewSongList;
        recyclerViewSongList.setLayoutManager(new LinearLayoutManager(recyclerViewSongList.getContext()));
        recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(
                this,
                userPickedPlaylist.getSongs());
        recyclerViewSongList.setAdapter(recyclerViewAdapterSongs);
        itemTouchHelperCallback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                ActivityMain activityMain = (ActivityMain) requireActivity();
                // TODO hopefully not needed
                /*
                    Collections.swap(recyclerViewAdapterSongs.getSongs(),
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());

                 */
                userPickedPlaylist.swapSongPositions(
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                recyclerViewAdapterSongs.notifyItemMoved(
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                activityMain.saveFile();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                ActivityMain activityMain = (ActivityMain) requireActivity();
                int position = viewHolder.getAdapterPosition();
                Song song = userPickedPlaylist.getSongs().get(position);
                double probability = userPickedPlaylist.getProbability(song);
                if (userPickedPlaylist.size() == 1) {
                    activityMain.removePlaylist(userPickedPlaylist);
                    viewModelUserPickedPlaylist.setUserPickedPlaylist(null);
                } else {
                    userPickedPlaylist.remove(song);
                }
                // TODO needed?
                /*
                recyclerViewAdapterSongs.getSongs().remove(position);
                 */
                recyclerViewAdapterSongs.notifyItemRemoved(position);
                activityMain.saveFile();
                undoListenerSongRemoved = v -> {
                    userPickedPlaylist.add(song, probability);
                    userPickedPlaylist.switchSongPositions(userPickedPlaylist.size() - 1, position);
                    // TODO needed?
                /*
                    recyclerViewAdapterSongs.updateList(userPickedPlaylist.getSongs());
                 */
                    recyclerViewAdapterSongs.notifyDataSetChanged();
                    activityMain.saveFile();
                };
                Snackbar snackbar = Snackbar.make(
                        binding.recyclerViewSongList,
                        R.string.song_removed, BaseTransientBottomBar.LENGTH_LONG);

                snackbar.setAction(R.string.undo, undoListenerSongRemoved);
                snackbar.show();
            }
        };
        itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewSongList);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
        setUpToolbar();
    }

    private void updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_add_black_24dp);
        viewModelActivityMain.setFABText(R.string.fab_edit);
        viewModelActivityMain.showFab(true);
        viewModelActivityMain.setFabOnClickListener(null);
        onClickListenerFAB = view -> {
            // userPickedSongs.isEmpty() when the user is editing a playlist
            viewModelUserPickedSongs.clearUserPickedSongs();
            NavHostFragment.findNavController(this).navigate(
                    FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist());
        };
        viewModelActivityMain.setFabOnClickListener(onClickListenerFAB);
    }

    @Override
    public boolean onMenuItemClickAddToPlaylist(Song song) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song);
        DialogFragment dialogFragment = new DialogFragmentAddToPlaylist();
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getParentFragmentManager(), getTag());
        return true;
    }

    @Override
    public boolean onMenuItemClickAddToQueue(Song song) {
        // TODO fix how music continues after queue is depleted
        // shuffle is off and looping is on or something like that?
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
        synchronized (ActivityMain.MUSIC_CONTROL_LOCK) {
            if (activityMain.getCurrentAudioUri() != null &&
                    song.equals(activityMain.getSong(
                            activityMain.getCurrentAudioUri().id))) {
                activityMain.seekTo(0);
            }
            activityMain.setCurrentPlaylist(viewModelUserPickedPlaylist.getUserPickedPlaylist());
            activityMain.clearSongQueue();
            activityMain.addToQueue(song.id);
            activityMain.playNext();
        }
        NavDirections action = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong();
        NavHostFragment.findNavController(this).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        itemTouchHelper.attachToRecyclerView(null);
        itemTouchHelperCallback = null;
        itemTouchHelper = null;
        undoListenerSongRemoved = null;
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
        onQueryTextListener = null;
        viewModelActivityMain.setFabOnClickListener(null);
        onClickListenerFAB = null;
        recyclerViewSongList.setAdapter(null);
        recyclerViewAdapterSongs = null;
        recyclerViewSongList = null;
        viewModelActivityMain.setPlaylistToAddToQueue(null);
        viewModelUserPickedPlaylist = null;
        viewModelUserPickedSongs = null;
        viewModelActivityMain = null;
        binding = null;
    }

}