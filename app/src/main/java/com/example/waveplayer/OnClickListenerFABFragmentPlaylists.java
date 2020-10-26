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
            fragmentPlaylists.activityMain.serviceMain.userPickedPlaylist = null;
            fragmentPlaylists.activityMain.serviceMain.userPickedSongs.clear();
            NavHostFragment.findNavController(fragmentPlaylists).navigate(
                    FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist());
        }

}
