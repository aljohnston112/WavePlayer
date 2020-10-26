package com.example.waveplayer;

import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.navigation.fragment.NavHostFragment;

public class OnClickListenerFABFragmentPlaylist implements View.OnClickListener {

    final FragmentPlaylist fragmentPlaylist;

    OnClickListenerFABFragmentPlaylist(FragmentPlaylist fragmentPlaylist){
        this.fragmentPlaylist = fragmentPlaylist;
    }

        @Override
        public void onClick(View view) {
            fragmentPlaylist.activityMain.serviceMain.userPickedSongs.clear();
            NavHostFragment.findNavController(fragmentPlaylist)
                    .navigate(FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSelectSongs());
        }

}
