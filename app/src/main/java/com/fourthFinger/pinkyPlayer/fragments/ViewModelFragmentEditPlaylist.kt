package com.fourthFinger.pinkyPlayer.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.ApplicationMain
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ToastUtil
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.UseCaseSongPicker
import com.fourthFinger.pinkyPlayer.settings.SettingsRepo

class ViewModelFragmentEditPlaylist(
    val songPicker: UseCaseSongPicker,
    val playlistsRepo: PlaylistsRepo,
    val settingsRepo: SettingsRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun editPlaylistFabClicked(fragment: Fragment, playlistName: String) {
        val finalPlaylist = playlistsRepo.getPlaylist(playlistName)
        val context = fragment.requireActivity().applicationContext
        if (validatePlaylistInput(context, playlistName)) {
            // userPickedPlaylist is non-null when editing a playlist.
            if (finalPlaylist != null) {
                updatePlaylist(
                    context,
                    finalPlaylist,
                    playlistName
                )
            } else {
                makeNewPlaylist(context, playlistName)
            }
            songPicker.unselectAllSongs()
            NavUtil.popBackStack(fragment)
        }
    }

    private fun validatePlaylistInput(
        context: Context,
        playlistName: String
    ): Boolean {
        val userPickedSongs = songPicker.getPickedSongs().toMutableList()
        val userPickedPlaylist = playlistsRepo.getPlaylist(playlistName)
        val makingNewPlaylist = (userPickedPlaylist == null)
        return if (userPickedSongs.size == 0) {
            ToastUtil.showToast(context, R.string.not_enough_songs_for_playlist)
            false
        } else if (playlistName.isEmpty()) {
            ToastUtil.showToast(context, R.string.no_name_playlist)
            false
        } else if (
            playlistsRepo.playlistExists(playlistName) && makingNewPlaylist) {
            ToastUtil.showToast(context, R.string.duplicate_name_playlist)
            false
        } else {
            true
        }
    }

    private fun updatePlaylist(
        context: Context,
        finalPlaylist: RandomPlaylist,
        playlistName: String
    ) {
        playlistsRepo.removePlaylist(context, finalPlaylist)
        finalPlaylist.setName(
            context,
            playlistsRepo,
            playlistName
        )
        removeMissingSongs(context, finalPlaylist)
        addNewSongs(context, finalPlaylist)
    }

    private fun removeMissingSongs(
        context: Context,
        finalPlaylist: RandomPlaylist
    ) {
        val oldPlaylistSongs = finalPlaylist.getSongs()
        val newPlaylistSongs = songPicker.getPickedSongs()
        for (song in oldPlaylistSongs) {
            if (!newPlaylistSongs.contains(song)) {
                finalPlaylist.remove(context, playlistsRepo, song)
            }
        }
    }

    private fun addNewSongs(
        context: Context,
        finalPlaylist: RandomPlaylist
    ) {
        val newPlaylistSongs = songPicker.getPickedSongs()
        for (song in newPlaylistSongs) {
            finalPlaylist.add(
                context,
                playlistsRepo,
                song
            )
            song.setSelected(false)
        }
    }

    private fun makeNewPlaylist(
        context: Context,
        playlistName: String
    ) {
        val newPlaylistSongs = songPicker.getPickedSongs()
        val finalPlaylist = RandomPlaylist(
            context,
            playlistName,
            newPlaylistSongs,
            false,
            settingsRepo.settings.value!!.maxPercent,
            playlistsRepo
        )
        playlistsRepo.addPlaylist(context, finalPlaylist)
    }

    fun selectSongsClicked(navController: NavController) {
        val oldPlaylistSongs = songPicker.getPickedSongs()
        for (song in oldPlaylistSongs) {
            song.setSelected(true)
        }
        NavUtil.navigate(
            navController,
            FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs()
        )
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val savedStateHandle = extras.createSavedStateHandle()
                return ViewModelFragmentEditPlaylist(
                    (application as ApplicationMain).songPicker,
                    application.playlistsRepo,
                    application.settingsRepo,
                    savedStateHandle
                ) as T
            }
        }
    }

}