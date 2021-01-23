package com.example.waveplayer.fragments.fragment_playlists;

import android.view.View;

import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.activity_main.ActivityMain;

public class OnClickListenerFABFragmentPlaylists implements View.OnClickListener {

    private final FragmentPlaylists fragmentPlaylists;

    OnClickListenerFABFragmentPlaylists(FragmentPlaylists fragmentPlaylists) {
        this.fragmentPlaylists = fragmentPlaylists;
    }

    @Override
    public void onClick(View view) {
        ActivityMain activityMain = (ActivityMain) fragmentPlaylists.getActivity();
        // userPickedPlaylist is null when user is making a new playlist
        fragmentPlaylists.setUserPickedPlaylist(null);
        fragmentPlaylists.clearUserPickedSongs();
        NavHostFragment.findNavController(fragmentPlaylists).navigate(
                FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist());
    }

}