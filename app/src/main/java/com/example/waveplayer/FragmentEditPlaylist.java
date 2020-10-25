package com.example.waveplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;

public class FragmentEditPlaylist extends Fragment {

    ActivityMain activityMain;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

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
        activityMain = ((ActivityMain) getActivity());
        if (activityMain != null) {
            activityMain.setActionBarTitle(getResources().getString(R.string.edit_playlist));
        }
        updateFAB(view);
        view.findViewById(R.id.buttonEditSongs).setOnClickListener(
                new OnClickListenerFragmentEditPlaylist(this));
        setUpBroadcastReceiverServiceConnected(view);
    }

    private void updateFAB(View view) {
        activityMain.showFab(true);
        activityMain.setFabImage(R.drawable.ic_check_black_24dp);
        if (activityMain.serviceMain != null) {
            final EditText finalEditTextPlaylistName = view.findViewById(R.id.editTextPlaylistName);
            ArrayList<AudioURI> playlistSongs = new ArrayList<>();
            // userPickedPlaylist is null when user is making a new playlist
            if (activityMain.serviceMain.userPickedPlaylist != null) {
                // activityMain.serviceMain.userPickedSongs.isEmpty()
                // when the user is editing a playlist
                if (activityMain.serviceMain.userPickedSongs.isEmpty()) {
                    activityMain.serviceMain.userPickedSongs.addAll(
                            activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap().keySet());
                }
                playlistSongs = new ArrayList<>(
                        activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap().keySet());
                finalEditTextPlaylistName.setText(
                        activityMain.serviceMain.userPickedPlaylist.getName());
            }
            final ArrayList<AudioURI> finalPlaylistSongs = playlistSongs;
            activityMain.setFabOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String playlistName = finalEditTextPlaylistName.getText().toString();
                    int playlistIndex = indexOfPlaylistWName(playlistName);
                    if (activityMain.serviceMain.userPickedSongs.size() == 0) {
                        Toast toast = Toast.makeText(getContext(),
                                R.string.not_enough_songs_for_playlist, Toast.LENGTH_LONG);
                        toast.show();
                    } else if (playlistName.length() == 0) {
                        Toast toast = Toast.makeText(getContext(),
                                R.string.no_name_playlist, Toast.LENGTH_LONG);
                        toast.show();
                    } else if (playlistIndex != -1 &&
                            activityMain.serviceMain.userPickedPlaylist == null) {
                        Toast toast = Toast.makeText(getContext(),
                                R.string.duplicate_name_playlist, Toast.LENGTH_LONG);
                        toast.show();
                    } else if (activityMain.serviceMain.userPickedPlaylist == null) {
                        activityMain.serviceMain.playlists.add(new RandomPlaylist(
                                activityMain.serviceMain.userPickedSongs,
                                ServiceMain.MAX_PERCENT,
                                finalEditTextPlaylistName.getText().toString(),
                                false, -1));
                        for (AudioURI audioURI : activityMain.serviceMain.userPickedSongs) {
                            audioURI.setSelected(false);
                        }
                        activityMain.serviceMain.userPickedSongs.clear();
                        activityMain.serviceMain.saveFile();
                        popBackStackAndHideKeyboard(view);
                    } else {
                        for (AudioURI audioURI : finalPlaylistSongs) {
                            if (!activityMain.serviceMain.userPickedSongs.contains(audioURI)) {
                                activityMain.serviceMain.userPickedPlaylist.getProbFun().remove(audioURI);
                            }
                        }
                        for (AudioURI audioURI : activityMain.serviceMain.userPickedSongs) {
                            activityMain.serviceMain.userPickedPlaylist.getProbFun().add(audioURI);
                            audioURI.setSelected(false);
                        }
                        activityMain.serviceMain.saveFile();
                        popBackStackAndHideKeyboard(view);
                    }
                }

                private int indexOfPlaylistWName(String playlistName) {
                    int playlistIndex = -1;
                    int i = 0;
                    for (RandomPlaylist randomPlaylist : activityMain.serviceMain.playlists) {
                        if (randomPlaylist.getName().equals(playlistName)) {
                            playlistIndex = i;
                        }
                        i++;
                    }
                    return playlistIndex;
                }

                private void popBackStackAndHideKeyboard(View view) {
                    NavController navController = NavHostFragment.findNavController(
                            FragmentEditPlaylist.this);
                    navController.popBackStack();
                    InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(
                            Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            });
        }
    }

    private void setUpBroadcastReceiverServiceConnected(final View view) {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFAB(view);
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        activityMain = null;
    }

}