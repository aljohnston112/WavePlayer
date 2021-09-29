package com.fourthFinger.pinkyPlayer.activity_main

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.fragments.ViewModelPlaylists
import com.fourthFinger.pinkyPlayer.fragments.ViewModelUserPicks
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import java.util.*

class DialogFragmentAddToPlaylist : DialogFragment() {

    // TODO add a cancel button
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>()
    private val viewModelUserPicks by activityViewModels<ViewModelUserPicks>()

    private val selectedPlaylistIndices: MutableList<Int> = ArrayList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.add_to_playlist)
        val bundle = arguments
        if (bundle != null) {
            setUpChoices(builder, selectedPlaylistIndices)
            setUpButtons(builder, bundle, selectedPlaylistIndices)
            return builder.create()
        }
        throw IllegalArgumentException("arguments must be passed to DialogFragmentAddToPlaylist")
    }

    private fun setUpChoices(
        builder: AlertDialog.Builder,
        selectedPlaylistIndices: MutableList<Int>
    ) {
        val playlistsRepo = PlaylistsRepo.getInstance(requireActivity().applicationContext)
        builder.setMultiChoiceItems(
            playlistsRepo.getPlaylistTitles(),
            null
        ) { _: DialogInterface?, which: Int, isChecked: Boolean ->
            if (isChecked) {
                selectedPlaylistIndices.add(which)
            } else {
                selectedPlaylistIndices.remove(which)
            }
        }
    }

    private fun setUpButtons(
        builder: AlertDialog.Builder,
        bundle: Bundle,
        selectedPlaylistIndices: MutableList<Int>
    ) {
        // These are here to prevent code duplication
        val song = bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG) as Song?
        val randomPlaylist = bundle.getSerializable(
            BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST
        ) as RandomPlaylist?
        builder.setPositiveButton(R.string.add) { _: DialogInterface?, _: Int ->
            if (song != null) {
                for (index in selectedPlaylistIndices) {
                    viewModelPlaylists.getPlaylists()[index].add(
                        requireActivity().applicationContext,
                        song
                    )
                }
            }
            if (randomPlaylist != null) {
                for (randomPlaylistSong in randomPlaylist.getSongs()) {
                    for (index in selectedPlaylistIndices) {
                        viewModelPlaylists.getPlaylists()[index].add(
                            requireActivity().applicationContext,
                            randomPlaylistSong
                        )
                    }
                }
            }
        }
        builder.setNeutralButton(R.string.new_playlist) { _: DialogInterface?, _: Int ->
            // UserPickedPlaylist need to be null for FragmentEditPlaylist to make a new playlist
            unselectSongs(requireActivity().applicationContext)
            val songs = mutableListOf<Song>()
            if (song != null) {
                songs.add(song)
            }
            if (randomPlaylist != null) {
                for (songInPlaylist in randomPlaylist.getSongs()) {
                    songs.add(songInPlaylist)
                }
            }
            viewModelUserPicks.startNewPlaylistWithSongs(songs)
            val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
            fragment?.let {
                NavUtil.navigateTo(
                    it,
                    R.id.fragmentEditPlaylist
                )
            }
        }
    }

    private fun unselectSongs(context: Context) {
        val songs = PlaylistsRepo.getInstance(context).getAllSongs()
        if(songs != null) {
            for (s in songs) {
                s.setSelected(false)
            }
        }
    }

    companion object {
        const val BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST: String = "ADD_TO_PLAYLIST_PLAYLIST"
        const val BUNDLE_KEY_ADD_TO_PLAYLIST_SONG: String = "ADD_TO_PLAYLIST_SONG"
        const val TAG = "DialogFragmentAddToPlaylist"
    }
}