package com.example.waveplayer;

import android.view.View;

import androidx.navigation.fragment.NavHostFragment;

public class OnClickListenerFragmentEditPlaylistButtonSelectSongs implements View.OnClickListener {

    private FragmentEditPlaylist fragmentEditPlaylist;

    OnClickListenerFragmentEditPlaylistButtonSelectSongs(FragmentEditPlaylist fragmentEditPlaylist){
        this.fragmentEditPlaylist = fragmentEditPlaylist;
    }

    @Override
    public void onClick(View view) {
        NavHostFragment.findNavController(fragmentEditPlaylist).navigate(
                FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs());
    }

}

