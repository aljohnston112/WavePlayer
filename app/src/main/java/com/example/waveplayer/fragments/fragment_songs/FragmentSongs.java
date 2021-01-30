package com.example.waveplayer.fragments.fragment_songs;

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
import com.example.waveplayer.databinding.RecyclerViewSongListBinding;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.fragments.OnQueryTextListenerSearch;
import com.example.waveplayer.fragments.RecyclerViewAdapterSongs;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.Song;

import java.util.ArrayList;
import java.util.List;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;

public class FragmentSongs extends Fragment implements
        RecyclerViewAdapterSongs.OnCreateContextMenuListenerSongsCallback,
        RecyclerViewAdapterSongs.OnClickListenerViewHolderCallback {

    public static final String NAME = "FragmentSongs";

    private RecyclerViewSongListBinding mBinding;

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    private OnQueryTextListenerSearch onQueryTextListenerSearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = RecyclerViewSongListBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewModelUserPickedPlaylist viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(view);
        updateMainContent();
        setUpRecyclerView();
        viewModelUserPickedPlaylist.setUserPickedPlaylist(MediaData.getInstance().getMasterPlaylist());
        setUpBroadcastReceiverServiceConnected();
        setUpBroadcastReceiverOnOptionsMenuCreated();
    }

    private void updateMainContent() {
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.songs));
        activityMain.showFab(false);
        setUpToolbar();
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
                onQueryTextListenerSearch = new OnQueryTextListenerSearch(activityMain, NAME);
                SearchView searchView = (SearchView) itemSearch.getActionView();
                searchView.setOnQueryTextListener(onQueryTextListenerSearch);
            }
        }
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerViewSongs = mBinding.recyclerViewSongList;
        List<Song> songs = MediaData.getInstance().getAllSongs();
        if (songs != null) {
            RecyclerViewAdapterSongs recyclerViewAdapterSongs = new RecyclerViewAdapterSongs(
                    this, this,
                    new ArrayList<>(songs));
            recyclerViewSongs.setLayoutManager(
                    new LinearLayoutManager(recyclerViewSongs.getContext()));
            recyclerViewSongs.setAdapter(recyclerViewAdapterSongs);
        }
    }

    private void setUpBroadcastReceiverServiceConnected() {
        final ActivityMain activityMain = ((ActivityMain) getActivity());
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected =
                new BroadcastReceiverOnServiceConnected() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        setUpRecyclerView();
                    }
                };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void setUpBroadcastReceiverOnOptionsMenuCreated() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        final ActivityMain activityMain = ((ActivityMain) requireActivity());
        mBinding = null;
        View view = getView();
        activityMain.hideKeyboard(view);
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) itemSearch.getActionView();
            searchView.setOnQueryTextListener(null);
            searchView.onActionViewCollapsed();
        }
        onQueryTextListenerSearch = null;
    }

    @Override
    public boolean onMenuItemClickAddToPlaylist(Song song) {
        contextMenuAddToPlaylist(song);
        return true;
    }

    private void contextMenuAddToPlaylist(Song song) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song);
        bundle.putBoolean(BUNDLE_KEY_IS_SONG, true);
        DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                new DialogFragmentAddToPlaylist();
        dialogFragmentAddToPlaylist.setArguments(bundle);
        dialogFragmentAddToPlaylist.show(getParentFragmentManager(), getTag());
    }

    @Override
    public boolean onMenuItemClickAddToQueue(Song song) {
        contextMenuAddToQueue(song);
        return true;
    }

    private void contextMenuAddToQueue(Song song) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        if (activityMain.songInProgress()) {
            activityMain.addToQueue(song.id);
        } else {
            activityMain.showSongPane();
            activityMain.addToQueueAndPlay(song.id);
        }
    }

    @Override
    public void onClick(Song song) {
        ActivityMain activityMain = ((ActivityMain) requireActivity());
        if (activityMain.getCurrentAudioUri() != null &&
                song.equals(MediaData.getInstance().getSong(
                        activityMain.getCurrentAudioUri().id))) {
            activityMain.seekTo(0);
        }
        activityMain.setCurrentPlaylistToMaster();
        activityMain.clearSongQueue();
        activityMain.addToQueueAndPlay(song.id);
        NavDirections action = FragmentSongsDirections.actionFragmentSongsToFragmentSong();
        if (action != null) {
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

}