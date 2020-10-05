package com.example.waveplayer;

import android.app.Activity;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        if (activityMain != null) {
            updateFAB(view);
            activityMain.setActionBarTitle(getResources().getString(R.string.edit_playlist));
            view.findViewById(R.id.buttonEditSongs).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NavHostFragment.findNavController(FragmentEditPlaylist.this).navigate(
                            FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs());
                }
            });
        }
    }

    private void updateFAB(View view) {
        activityMain.showFab(true);
        activityMain.setFabImage(R.drawable.ic_check_black_24dp);
        final EditText finalEditTextPlaylistName = view.findViewById(R.id.editTextPlaylistName);
        ArrayList<AudioURI> playlistSongs = new ArrayList<>();
        if (activityMain.userPickedPlaylist != null) {
            if(activityMain.userPickedSongs.isEmpty()) {
                activityMain.userPickedSongs.addAll(activityMain.userPickedPlaylist.getProbFun().getProbMap().keySet());
            }
            playlistSongs  = new ArrayList<>(activityMain.userPickedPlaylist.getProbFun().getProbMap().keySet());
            finalEditTextPlaylistName.setText(activityMain.userPickedPlaylist.getName());
        }
        final ArrayList<AudioURI> finalPlaylistSongs = playlistSongs;
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String playlistName = finalEditTextPlaylistName.getText().toString();
                int playlistIndex = indexOfPlaylistWName(playlistName);
                if (activityMain.userPickedSongs.size() == 0) {
                    Toast toast = Toast.makeText(getContext(), R.string.not_enough_songs_for_playlist, Toast.LENGTH_LONG);
                    toast.show();
                } else if (playlistName.length() == 0) {
                    Toast toast = Toast.makeText(getContext(), R.string.no_name_playlist, Toast.LENGTH_LONG);
                    toast.show();
                } else if(playlistIndex != -1 && activityMain.userPickedPlaylist == null){
                    Toast toast = Toast.makeText(getContext(), R.string.duplicate_name_playlist, Toast.LENGTH_LONG);
                    toast.show();
                } else if (activityMain.userPickedPlaylist == null) {
                    activityMain.serviceMain.playlists.add(new RandomPlaylist(
                            activityMain.userPickedSongs,
                            ServiceMain.MAX_PERCENT,
                            finalEditTextPlaylistName.getText().toString()));
                    for (AudioURI audioURI : activityMain.userPickedSongs) {
                        audioURI.setSelected(false);
                    }
                    activityMain.userPickedSongs.clear();
                    popBackStackAndHideKeyboard(view);
                }  else{
                    for(AudioURI audioURI : finalPlaylistSongs) {
                        if (!activityMain.userPickedSongs.contains(audioURI)){
                            activityMain.userPickedPlaylist.getProbFun().remove(audioURI);
                        }
                    }
                    for(AudioURI audioURI : activityMain.userPickedSongs){
                        activityMain.userPickedPlaylist.getProbFun().add(audioURI);
                        audioURI.setSelected(false);
                    }
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
                NavController navController = NavHostFragment.findNavController(FragmentEditPlaylist.this);
                navController.popBackStack();
                InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

    }

}