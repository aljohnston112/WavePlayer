package com.example.waveplayer.fragments.fragment_playlist;

import android.view.View;

import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.ActivityMain;

public class OnClickListenerFABFragmentPlaylist implements View.OnClickListener {

    private final FragmentPlaylist fragmentPlaylist;

    OnClickListenerFABFragmentPlaylist(FragmentPlaylist fragmentPlaylist){
        this.fragmentPlaylist = fragmentPlaylist;
    }

        @Override
        public void onClick(View view) {
            fragmentPlaylist.clearUserPickedSongs();
            NavHostFragment.findNavController(fragmentPlaylist).navigate(
                    FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist());
        }

}
