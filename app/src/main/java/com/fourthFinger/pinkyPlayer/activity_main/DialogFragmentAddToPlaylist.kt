package com.fourthFinger.pinkyPlayer.activity_main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.fragments.ViewModelPlaylists
import com.fourthFinger.pinkyPlayer.media_controller.MediaData
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import java.util.*

class DialogFragmentAddToPlaylist : DialogFragment() {

    // TODO add a cancel button
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.add_to_playlist)
        val bundle = arguments
        if (bundle != null) {
            val selectedPlaylistIndices: MutableList<Int> = ArrayList()
            setUpChoices(builder, selectedPlaylistIndices)
            setUpButtons(builder, bundle, selectedPlaylistIndices)
            return builder.create()
        }
        throw IllegalArgumentException("arguments must be passed to DialogFragmentAddToPlaylist")
    }

    private fun setUpChoices(builder: AlertDialog.Builder, selectedPlaylistIndices: MutableList<Int>) {
        val mediaData = MediaData.getInstance(requireActivity().applicationContext)
        builder.setMultiChoiceItems(viewModelPlaylists.getPlaylistTitles(),
                null){ _: DialogInterface?, which: Int, isChecked: Boolean ->
            if (isChecked) {
                selectedPlaylistIndices.add(which)
            } else {
                selectedPlaylistIndices.remove(Integer.valueOf(which))
            }
        }
    }

    private fun setUpButtons(builder: AlertDialog.Builder, bundle: Bundle,
                             selectedPlaylistIndices: MutableList<Int>) {
        val activityMain = requireActivity() as ActivityMain
        val mediaData = MediaData.getInstance(requireActivity().applicationContext)
        // These are here to prevent code duplication
        val song = bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG) as Song?
        val randomPlaylist = bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST) as RandomPlaylist?
        builder.setPositiveButton(R.string.add){ _: DialogInterface?, _: Int ->
            if (song != null) {
                for (index in selectedPlaylistIndices) {
                    viewModelPlaylists.getPlaylists()[index].add(song)
                }
            }
            if (randomPlaylist != null) {
                for (randomPlaylistSong in randomPlaylist.getSongs()) {
                    for (index in selectedPlaylistIndices) {
                        viewModelPlaylists.getPlaylists()[index].add(randomPlaylistSong)
                    }
                }
            }
        }
        builder.setNeutralButton(R.string.new_playlist){ _: DialogInterface?, _: Int ->
            // UserPickedPlaylist need to be null for FragmentEditPlaylist to make a new playlist
            viewModelPlaylists.setUserPickedPlaylist(null)
            viewModelPlaylists.clearUserPickedSongs()
            if (song != null) {
                viewModelPlaylists.addUserPickedSong(song)
            }
            if (randomPlaylist != null) {
                for (songInPlaylist in randomPlaylist.getSongs()) {
                    viewModelPlaylists.addUserPickedSong(songInPlaylist)
                }
            }
            activityMain.navigateTo(R.id.fragmentEditPlaylist)
        }
    }



    companion object {
        // TODO get rid of bundles... probably not
        val BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST: String = "ADD_TO_PLAYLIST_PLAYLIST"
        val BUNDLE_KEY_ADD_TO_PLAYLIST_SONG: String = "ADD_TO_PLAYLIST_SONG"
    }
}