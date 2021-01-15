package com.example.waveplayer;

import android.view.View;

import androidx.navigation.fragment.NavHostFragment;

public class OnClickListenerFABFragmentPlaylist implements View.OnClickListener {

    private final FragmentPlaylist fragmentPlaylist;

    OnClickListenerFABFragmentPlaylist(FragmentPlaylist fragmentPlaylist){
        this.fragmentPlaylist = fragmentPlaylist;
    }

        @Override
        public void onClick(View view) {
            ((ActivityMain)fragmentPlaylist.getActivity()).clearUserPickedSongs();
            NavHostFragment.findNavController(fragmentPlaylist).navigate(
                    FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist());
        }

}
