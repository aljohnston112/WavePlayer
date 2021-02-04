package com.example.waveplayer.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.fragments.FragmentEditPlaylistDirections;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.List;

public class FragmentEditPlaylist extends Fragment {

    private ViewModelActivityMain viewModelActivityMain;

    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private BroadcastReceiver broadcastReceiverOnServiceConnected;

    private View.OnClickListener onClickListenerFragmentEditPlaylistButtonSelectSongs;

    private View.OnClickListener onClickListenerFABFragmentEditPlaylist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_playlist, container, false);
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
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.edit_playlist));
        updateFAB();
        onClickListenerFragmentEditPlaylistButtonSelectSongs =
                view1 -> navigateToFragmentSelectSongs();
        view.findViewById(R.id.buttonEditSongs).setOnClickListener(
                onClickListenerFragmentEditPlaylistButtonSelectSongs);
        setUpBroadcastReceiverServiceConnected();
    }

    private void navigateToFragmentSelectSongs() {
        NavHostFragment.findNavController(this).navigate(
                FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void updateFAB() {
        View view = getView();
        viewModelActivityMain.setFabImage(R.drawable.ic_check_black_24dp);
        viewModelActivityMain.setFABText(R.string.fab_save);
        viewModelActivityMain.showFab(true);
        final EditText finalEditTextPlaylistName = view.findViewById(R.id.editTextPlaylistName);
        RandomPlaylist userPickedPlaylist = viewModelUserPickedPlaylist.getUserPickedPlaylist();
        List<Song> userPickedSongs = viewModelUserPickedSongs.getUserPickedSongs();
        // userPickedPlaylist is null when user is making a new playlist
        if (userPickedPlaylist != null) {
            // userPickedSongs.isEmpty() when the user is editing a playlist
            if (userPickedSongs.isEmpty()) {
                userPickedSongs.addAll(
                        viewModelUserPickedPlaylist.getUserPickedPlaylist().getSongs());
            }
            finalEditTextPlaylistName.setText(userPickedPlaylist.getName());
        }
        onClickListenerFABFragmentEditPlaylist = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ActivityMain activityMain = (ActivityMain) requireActivity();
                int playlistIndex = indexOfPlaylistWName(finalEditTextPlaylistName.getText().toString());
                if (userPickedSongs.size() == 0) {
                    activityMain.showToast(R.string.not_enough_songs_for_playlist);
                } else if (finalEditTextPlaylistName.length() == 0) {
                    activityMain.showToast(R.string.no_name_playlist);
                } else if (playlistIndex != -1 && userPickedPlaylist == null) {
                    activityMain.showToast(R.string.duplicate_name_playlist);
                } else if (userPickedPlaylist == null) {
                    activityMain.addPlaylist(new RandomPlaylist(finalEditTextPlaylistName.getText().toString(),
                            userPickedSongs, activityMain.getMaxPercent(), false));
                    for (Song song : userPickedSongs) {
                        song.setSelected(false);
                    }
                    userPickedSongs.clear();
                    activityMain.saveFile();
                    popBackStack();
                    activityMain.hideKeyboard(view);
                } else {
                    ArrayList<String> names = new ArrayList<>();
                    for (RandomPlaylist randomPlaylist : activityMain.getPlaylists()) {
                        names.add(randomPlaylist.getName());
                    }
                    if (userPickedPlaylist.getName().equals(
                            finalEditTextPlaylistName.getText().toString()) ||
                            !names.contains(finalEditTextPlaylistName.getText().toString())) {
                        userPickedPlaylist.setName(finalEditTextPlaylistName.getText().toString());
                        for (Song song : userPickedPlaylist.getSongs()) {
                            if (!userPickedSongs.contains(song)) {
                                userPickedPlaylist.remove(song);
                            }
                        }
                        for (Song song : userPickedSongs) {
                            userPickedPlaylist.add(song);
                            song.setSelected(false);
                        }
                        activityMain.saveFile();
                        popBackStack();
                        activityMain.hideKeyboard(view);
                    } else {
                        activityMain.showToast(R.string.duplicate_name_playlist);
                    }
                }
            }

            private int indexOfPlaylistWName(String playlistName) {
                ActivityMain activityMain = (ActivityMain) requireActivity();
                int playlistIndex = -1;
                int i = 0;
                for (RandomPlaylist randomPlaylist : activityMain.getPlaylists()) {
                    if (randomPlaylist.getName().equals(playlistName)) {
                        playlistIndex = i;
                    }
                    i++;
                }
                return playlistIndex;
            }
        };
        viewModelActivityMain.setFabOnClickListener(onClickListenerFABFragmentEditPlaylist);
    }

    private void setUpBroadcastReceiverServiceConnected() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFAB();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        view.findViewById(R.id.buttonEditSongs).setOnClickListener(null);
        onClickListenerFragmentEditPlaylistButtonSelectSongs = null;
        onClickListenerFABFragmentEditPlaylist = null;
        viewModelUserPickedPlaylist = null;
        viewModelUserPickedSongs = null;
        viewModelActivityMain = null;
    }

    // TODO move?
    void popBackStack() {
        NavController navController = NavHostFragment.findNavController(
                FragmentEditPlaylist.this);
        navController.popBackStack();
    }

}