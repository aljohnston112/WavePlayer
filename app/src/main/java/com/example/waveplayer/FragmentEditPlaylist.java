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

    public FragmentEditPlaylist() {
        // Required empty public constructor
    }

    public static FragmentEditPlaylist newInstance() {
        FragmentEditPlaylist fragment = new FragmentEditPlaylist();
        return fragment;
    }

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
        final ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.edit_playlist));

        RandomPlaylist randomPlaylist = activityMain.currentPlaylist;
        if (randomPlaylist != null) {
            activityMain.songsToHighlight = new ArrayList<>(randomPlaylist.getProbFun().getProbMap().keySet());
        } else {
            activityMain.songsToHighlight = new ArrayList<>();
        }

        view.findViewById(R.id.buttonChangeSongs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FragmentEditPlaylist.this).navigate(
                        FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs());
            }
        });

        final ArrayList<AudioURI> finalUserPickedSongs = activityMain.userPickedSongs;
        final EditText finalEditTextPlaylistName = view.findViewById(R.id.editTextPlaylistName);
        activityMain.setFabImage(getResources().getDrawable(R.drawable.ic_check_black_24dp));
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finalUserPickedSongs != null  && finalUserPickedSongs.size() != 0
                        && finalEditTextPlaylistName.length() != 0) {
                    activityMain.playlists.add(new RandomPlaylist(finalUserPickedSongs, ActivityMain.MAX_PERCENT, finalEditTextPlaylistName.getText().toString()));
                    for(AudioURI audioURI : finalUserPickedSongs){
                        audioURI.setChecked(false);
                    }
                    activityMain.userPickedSongs = new ArrayList<>();
                    NavController navController = NavHostFragment.findNavController(FragmentEditPlaylist.this);
                    navController.popBackStack();
                    InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                } else if(finalUserPickedSongs == null || finalUserPickedSongs.size() == 0){
                    Toast toast = Toast.makeText(getContext(), R.string.not_enough_songs_for_playlist, Toast.LENGTH_LONG);
                    toast.show();
                } else if(finalEditTextPlaylistName.length() == 0){
                    Toast toast = Toast.makeText(getContext(), R.string.no_name_playlist, Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

}