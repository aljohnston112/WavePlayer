package com.fourthFinger.pinkyPlayer

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.fragments.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NavUtil {

    companion object {

        @OptIn(DelicateCoroutinesApi::class)
        fun navigateTo(fragment: Fragment, id: Int) {
            GlobalScope.launch(Dispatchers.Main) {
                val navController: NavController = NavHostFragment.findNavController(fragment)
                val d = navController.currentDestination
                if (d != null) {
                    if (d.id != id) {
                        navController.navigate(id)
                    }
                } else {
                    navController.navigate(id)
                }
            }
        }

        fun navigate(
            navController: NavController,
            action: NavDirections
        ) {
            var safe = true
            if (action == FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists() ||
                action == FragmentTitleDirections.actionFragmentTitleToFragmentPlaylist() ||
                action == FragmentTitleDirections.actionFragmentTitleToFragmentSettings() ||
                action == FragmentTitleDirections.actionFragmentTitleToFragmentSongs()
            ) {
                if (navController.currentDestination?.id != R.id.FragmentTitle) {
                    safe = false
                }
            } else if(action == FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist() ||
                action == FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist()){
                if (navController.currentDestination?.id != R.id.FragmentPlaylists) {
                    safe = false
                }
            }
            else if(action == FragmentSongsDirections.actionFragmentSongsToFragmentSong()){
                if (navController.currentDestination?.id != R.id.fragmentSongs) {
                    safe = false
                }
            }
            else if(action == FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist() ||
                action == FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong()){
                if (navController.currentDestination?.id != R.id.fragmentPlaylist) {
                    safe = false
                }
            } else if(action == FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs()){
                if (navController.currentDestination?.id != R.id.fragmentEditPlaylist) {
                    safe = false
                }
            }
            else if(action == FragmentLoadingDirections.actionFragmentLoadingToFragmentTitle()) {
                if (navController.currentDestination?.id != R.id.fragmentLoading) {
                    safe = false
                }
            }
            if (safe) {
                navController.navigate(action)
            }
        }

        fun popBackStack(fragment: Fragment) {
            NavHostFragment.findNavController(fragment).popBackStack()
        }

    }

}