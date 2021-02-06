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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.RecyclerViewSongListBinding;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.R;

import java.util.ArrayList;
import java.util.List;

public class FragmentSelectSongs extends Fragment
        implements RecyclerViewAdapterSelectSongs.ListenerCallbackSelectSongs {

    public static final String NAME = "FragmentSelectSongs";

    private RecyclerViewSongListBinding binding;

    private ViewModelActivityMain viewModelActivityMain;
    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private BroadcastReceiver broadcastReceiver;

    private RecyclerView recyclerViewSongList;
    private RecyclerViewAdapterSelectSongs recyclerViewAdapter;

    private View.OnClickListener onClickListenerFABFragmentSelectSongs;
    private SearchView.OnQueryTextListener onQueryTextListenerSearch;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        createViewModels();
        binding = RecyclerViewSongListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void createViewModels() {
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
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.select_songs));
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
                        List<Song> songs = activityMain.getAllSongs();
                        List<Song> sifted = new ArrayList<>();
                        if (!newText.equals("")) {
                            for (Song song : songs) {
                                if (song.title.toLowerCase().contains(newText.toLowerCase())) {
                                    sifted.add(song);
                                }
                            }
                            recyclerViewAdapter.updateList(sifted);
                        } else {
                            recyclerViewAdapter.updateList(songs);
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
        recyclerViewSongList = binding.recyclerViewSongList;
        recyclerViewSongList.setLayoutManager(new LinearLayoutManager(recyclerViewSongList.getContext()));
        for (Song song : viewModelUserPickedSongs.getUserPickedSongs()) {
            song.setSelected(true);
        }
        recyclerViewAdapter = new RecyclerViewAdapterSelectSongs(
                this, activityMain.getAllSongs());
        recyclerViewSongList.setAdapter(recyclerViewAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpToolbar();
        updateFAB();
    }

    private void updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_white_24dp);
        viewModelActivityMain.setFABText(R.string.fab_done);
        viewModelActivityMain.showFab(true);
        onClickListenerFABFragmentSelectSongs = (view) -> {
            NavController navController = NavHostFragment.findNavController(this);
            if (navController.getCurrentDestination().getId() == R.id.FragmentSelectSongs) {
                navController.popBackStack();
            }
        };
        viewModelActivityMain.setFabOnClickListener(onClickListenerFABFragmentSelectSongs);
    }

    @Override
    public List<Song> getUserPickedSongs() {
        return viewModelUserPickedSongs.getUserPickedSongs();
    }

    @Override
    public void removeUserPickedSong(Song song) {
        viewModelUserPickedSongs.removeUserPickedSong(song);
    }

    @Override
    public void addUserPickedSong(Song song) {
        viewModelUserPickedSongs.addUserPickedSong(song);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        viewModelActivityMain.setFabOnClickListener(null);
        onClickListenerFABFragmentSelectSongs = null;
        recyclerViewSongList.setAdapter(null);
        recyclerViewAdapter = null;
        recyclerViewSongList = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) itemSearch.getActionView();
            searchView.setOnQueryTextListener(null);
            searchView.onActionViewCollapsed();
        }
        onQueryTextListenerSearch = null;
        viewModelUserPickedSongs = null;
        viewModelActivityMain = null;
        binding = null;
    }

}