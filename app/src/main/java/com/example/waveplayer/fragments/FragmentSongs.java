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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.R;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.RecyclerViewSongListBinding;
import com.example.waveplayer.random_playlist.Song;

import java.util.ArrayList;
import java.util.List;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;

public class FragmentSongs extends Fragment implements
        RecyclerViewAdapterSongs.ListenerCallbackSongs {

    private RecyclerViewSongListBinding binding;

    private ViewModelActivityMain viewModelActivityMain;
    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private BroadcastReceiver broadcastReceiver;

    private RecyclerView recyclerViewSongs;
    private RecyclerViewAdapterSongs recyclerViewAdapterSongs;

    private SearchView.OnQueryTextListener onQueryTextListenerSearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        binding = RecyclerViewSongListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(view);
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.songs));
        viewModelActivityMain.showFab(false);
        viewModelUserPickedPlaylist.setUserPickedPlaylist(activityMain.getMasterPlaylist());
        setUpBroadcastReceiver();
    }

    private void setUpBroadcastReceiver() {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
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
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            if (itemSearch != null) {
                onQueryTextListenerSearch = new SearchView.OnQueryTextListener(){

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        RecyclerView recyclerViewSongs = activityMain.findViewById(R.id.recycler_view_song_list);
                        if (recyclerViewSongs != null) {
                            RecyclerViewAdapterSongs recyclerViewAdapterSongs =
                                    (RecyclerViewAdapterSongs) recyclerViewSongs.getAdapter();
                            List<Song> songs = activityMain.getAllSongs();
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
                        }
                        return true;
                    }
                };
                SearchView searchView = (SearchView) itemSearch.getActionView();
                searchView.setOnQueryTextListener(onQueryTextListenerSearch);
            }
        }
    }

    private void setUpRecyclerView() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        recyclerViewSongs = binding.recyclerViewSongList;
        List<Song> songs = activityMain.getAllSongs();
        if (songs != null) {
            recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(
                    this, new ArrayList<>(songs));
            recyclerViewSongs.setLayoutManager(new LinearLayoutManager(recyclerViewSongs.getContext()));
            recyclerViewSongs.setAdapter(recyclerViewAdapterSongs);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpToolbar();
        setUpRecyclerView();
    }

    @Override
    public boolean onMenuItemClickAddToPlaylist(Song song) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song);
        DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                new DialogFragmentAddToPlaylist();
        dialogFragmentAddToPlaylist.setArguments(bundle);
        dialogFragmentAddToPlaylist.show(getParentFragmentManager(), getTag());
        return true;
    }

    @Override
    public boolean onMenuItemClickAddToQueue(Song song) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        if (activityMain.songInProgress()) {
            activityMain.addToQueue(song.id);
        } else {
            activityMain.showSongPane();
            activityMain.addToQueue(song.id);
            if (!activityMain.isSongInProgress()) {
                activityMain.playNext();
            }
        }
        return true;
    }

    @Override
    public void onClickViewHolder(Song song) {
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        if (song.equals(activityMain.getCurrentSong())) {
            activityMain.seekTo(0);
        }
        activityMain.setCurrentPlaylistToMaster();
        activityMain.clearSongQueue();
        activityMain.addToQueue(song.id);
        activityMain.playNext();
        NavDirections action = FragmentSongsDirections.actionFragmentSongsToFragmentSong();
        if (action != null) {
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        final ActivityMain activityMain = ((ActivityMain) requireActivity());
        View view = requireView();
        activityMain.hideKeyboard(view);
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) itemSearch.getActionView();
            searchView.setOnQueryTextListener(null);
            searchView.onActionViewCollapsed();
        }
        onQueryTextListenerSearch = null;
        recyclerViewSongs.setAdapter(null);
        recyclerViewAdapterSongs = null;
        recyclerViewSongs = null;
        activityMain.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        viewModelUserPickedPlaylist = null;
        viewModelActivityMain = null;
        binding = null;
    }

}