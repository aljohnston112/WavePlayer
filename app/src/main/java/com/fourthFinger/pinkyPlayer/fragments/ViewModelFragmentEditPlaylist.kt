package com.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

class ViewModelFragmentEditPlaylist: ViewModel() {

    fun editSongsClicked(navController: NavController){
        navController.navigate(
            FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs()
        )
    }

}