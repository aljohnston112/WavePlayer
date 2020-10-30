package com.example.waveplayer;

import android.view.View;

import androidx.navigation.fragment.NavHostFragment;

public class OnClickListenerFABFragmentPlaylists implements View.OnClickListener {

    final FragmentPlaylists fragmentPlaylists;

    OnClickListenerFABFragmentPlaylists(FragmentPlaylists fragmentPlaylists){
        this.fragmentPlaylists = fragmentPlaylists;
    }

        @Override
        public void onClick(View view) {
            ActivityMain activityMain = (ActivityMain) fragmentPlaylists.getActivity();
            // userPickedPlaylist is null when user is making a new playlist
            activityMain.setUserPickedPlaylist(null);
            activityMain.clearUserPickedSongs();
            NavHostFragment.findNavController(fragmentPlaylists).navigate(
                    FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist());
        }

}
