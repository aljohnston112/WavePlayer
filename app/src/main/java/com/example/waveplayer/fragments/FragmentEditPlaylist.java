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
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentEditPlaylistBinding;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.List;

public class FragmentEditPlaylist extends Fragment {

    private FragmentEditPlaylistBinding binding;

    private ViewModelActivityMain viewModelActivityMain;
    private ViewModelUserPickedSongs viewModelUserPickedSongs;
    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private BroadcastReceiver broadcastReceiverOnServiceConnected;

    private View.OnClickListener onClickListenerFAB;
    private View.OnClickListener onClickListenerButtonSelectSongs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        createViewModels();
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.edit_playlist));
        binding = FragmentEditPlaylistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO don't think this is needed
        //  updateFAB();
        onClickListenerButtonSelectSongs = button ->
                NavHostFragment.findNavController(this).navigate(
                        FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs());
        binding.buttonEditSongs.setOnClickListener(onClickListenerButtonSelectSongs);
        setUpBroadcastReceiverServiceConnected();
    }

    private void createViewModels() {
        viewModelUserPickedSongs =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedSongs.class);
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
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
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_service_connected))) {
                        // TODO is this needed if onResume has it?
                        //  updateFAB();
                    }
                }
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_black_24dp);
        viewModelActivityMain.setFABText(R.string.fab_save);
        viewModelActivityMain.showFab(true);
        EditText finalEditTextPlaylistName = binding.editTextPlaylistName;
        RandomPlaylist userPickedPlaylist = viewModelUserPickedPlaylist.getUserPickedPlaylist();
        List<Song> userPickedSongs = viewModelUserPickedSongs.getUserPickedSongs();
        // userPickedPlaylist is null when user is making a new playlist
        if (userPickedPlaylist != null) {
            // userPickedSongs.isEmpty() when the user is editing a playlist
            // TODO if user is editing a playlist, unselects all the songs and returns here, ERROR
            if (userPickedSongs.isEmpty()) {
                userPickedSongs.addAll(
                        viewModelUserPickedPlaylist.getUserPickedPlaylist().getSongs());
                finalEditTextPlaylistName.setText(userPickedPlaylist.getName());
            }
        }
        onClickListenerFAB = view -> {
            ActivityMain activityMain = (ActivityMain) requireActivity();
            int playlistIndex = indexOfPlaylistWName(finalEditTextPlaylistName.getText().toString());
            if (userPickedSongs.size() == 0) {
                activityMain.showToast(R.string.not_enough_songs_for_playlist);
            } else if (finalEditTextPlaylistName.length() == 0) {
                activityMain.showToast(R.string.no_name_playlist);
            } else if (playlistIndex != -1 && userPickedPlaylist == null) {
                activityMain.showToast(R.string.duplicate_name_playlist);
            } else if (userPickedPlaylist == null) {
                // userPickedPlaylist is null when user is making a new playlist
                activityMain.addPlaylist(new RandomPlaylist(finalEditTextPlaylistName.getText().toString(),
                        userPickedSongs, activityMain.getMaxPercent(), false));
                for (Song song : userPickedSongs) {
                    song.setSelected(false);
                }
                viewModelUserPickedSongs.clearUserPickedSongs();
                activityMain.saveFile();
                activityMain.hideKeyboard(view);
                activityMain.popBackStack(this);
            } else {
                // userPickedPlaylist is not null when the user is editing a playlist
                ArrayList<String> playlistNames = new ArrayList<>();
                for (RandomPlaylist randomPlaylist : activityMain.getPlaylists()) {
                    playlistNames.add(randomPlaylist.getName());
                }
                if (userPickedPlaylist.getName().equals(
                        finalEditTextPlaylistName.getText().toString()) ||
                        !playlistNames.contains(finalEditTextPlaylistName.getText().toString())) {
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
                    activityMain.hideKeyboard(view);
                    activityMain.popBackStack(this);
                } else {
                    activityMain.showToast(R.string.duplicate_name_playlist);
                }
            }
        };
        viewModelActivityMain.setFabOnClickListener(onClickListenerFAB);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        binding.buttonEditSongs.setOnClickListener(null);
        onClickListenerButtonSelectSongs = null;
        viewModelActivityMain.setFabOnClickListener(null);
        onClickListenerFAB = null;
        viewModelUserPickedSongs = null;
        viewModelUserPickedPlaylist = null;
        viewModelActivityMain = null;
        binding = null;
    }

}