package com.example.waveplayer.fragments.fragment_select_songs;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.R;

import java.util.List;

public class FragmentSelectSongs extends Fragment {

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private OnClickListenerFABFragmentSelectSongs onClickListenerFABFragmentSelectSongs;

    private RecyclerView recyclerViewSongList;

    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    // TODO search functionality

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelUserPickedSongs =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedSongs.class);
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.hideKeyboard(view);
        activityMain.setActionBarTitle(getResources().getString(R.string.select_songs));
        updateFAB();
        setUpRecyclerView();
        setUpBroadcastReceiverServiceConnected();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void updateFAB() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setFabImage(R.drawable.ic_check_white_24dp);
        activityMain.setFABText(R.string.fab_done);
        activityMain.showFab(true);
        onClickListenerFABFragmentSelectSongs = new OnClickListenerFABFragmentSelectSongs(this);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentSelectSongs);
    }

    private void setUpRecyclerView() {
        View view = getView();
        recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
        recyclerViewSongList.setLayoutManager(
                new LinearLayoutManager(recyclerViewSongList.getContext()));
        for (Song song : viewModelUserPickedSongs.getUserPickedSongs()) {
            song.setSelected(true);
        }
        RecyclerViewAdapterSelectSongs recyclerViewAdapterSelectSongs =
                new RecyclerViewAdapterSelectSongs(this);
        recyclerViewSongList.setAdapter(recyclerViewAdapterSelectSongs);
    }

    private void setUpBroadcastReceiverServiceConnected() {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        recyclerViewSongList.setAdapter(null);
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        onClickListenerFABFragmentSelectSongs = null;
        viewModelUserPickedSongs = null;
    }

    public List<Song> getUserPickedSongs() {
        return viewModelUserPickedSongs.getUserPickedSongs();
    }

    public void removeUserPickedSong(Song song) {
        viewModelUserPickedSongs.removeUserPickedSong(song);
    }

    public void addUserPickedSong(Song song) {
        viewModelUserPickedSongs.addUserPickedSong(song);
    }
}