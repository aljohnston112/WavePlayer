package com.example.waveplayer;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class OnClickListenerFABFragmentSelectSongs implements View.OnClickListener {

    final FragmentSelectSongs fragmentSelectSongs;

    OnClickListenerFABFragmentSelectSongs(FragmentSelectSongs fragmentSelectSongs) {
        this.fragmentSelectSongs = fragmentSelectSongs;
    }

    @Override
    public void onClick(View view) {
        NavController navController =
                NavHostFragment.findNavController(fragmentSelectSongs);
        navController.popBackStack();
    }

}
