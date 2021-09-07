package com.fourthFinger.pinkyPlayer.fragments

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.NavUtil

class ViewModelFragmentEditPlaylist: ViewModel() {

    fun editSongsClicked(navController: NavController){
        NavUtil.navigate(navController, FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs())
    }

}