package io.fourth_finger.pinky_player.activity_main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import io.fourth_finger.pinky_player.NavUtil
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.fragments.ViewModelDialogFragmentAddToPlaylist
import io.fourth_finger.pinky_player.fragments.ViewModelPlaylists
import io.fourth_finger.playlist_data_source.RandomPlaylist
import io.fourth_finger.playlist_data_source.Song

class DialogFragmentAddToPlaylist : DialogFragment() {

    // TODO add a cancel button
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists> {
        ViewModelPlaylists.Factory
    }
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain> {
        ViewModelActivityMain.Factory
    }

    private val viewModelDialogFragmentAddToPlaylist by activityViewModels<ViewModelDialogFragmentAddToPlaylist> {
        ViewModelDialogFragmentAddToPlaylist.Factory
    }

    private val selectedPlaylistIndices: MutableList<Int> = ArrayList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.add_to_playlist)
        val bundle = arguments
        if (bundle != null) {
            setUpChoices(
                builder,
                selectedPlaylistIndices
            )
            setUpButtons(
                builder,
                bundle,
                selectedPlaylistIndices
            )
            return builder.create()
        }
        throw IllegalArgumentException("arguments must be passed to DialogFragmentAddToPlaylist")
    }

    private fun setUpChoices(
        builder: AlertDialog.Builder,
        selectedPlaylistIndices: MutableList<Int>
    ) {
        viewModelPlaylists.playlists.value?.map { it.name }
        builder.setMultiChoiceItems(
            viewModelPlaylists.getPlaylistTitles(),
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
        selectedPlaylistIndices: MutableList<Int>,
    ) {
        // These are here to prevent code duplication

        @Suppress("DEPRECATION")
        val song = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG) as Song?
        } else {
            bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, Song::class.java)
        }

        @Suppress("DEPRECATION")
        val randomPlaylist = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST) as RandomPlaylist?
        } else {
            bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, RandomPlaylist::class.java)
        }

        builder.setPositiveButton(R.string.add) { _: DialogInterface?, _: Int ->
            if (song != null) {
                for (index in selectedPlaylistIndices) {
                    viewModelPlaylists.addToPlaylist(
                        requireContext(),
                        index,
                        song
                    )
                }
            }
            if (randomPlaylist != null) {
                for (randomPlaylistSong in randomPlaylist.getSongs()) {
                    for (index in selectedPlaylistIndices) {
                        viewModelPlaylists.addToPlaylist(
                            requireContext(),
                            index,
                            randomPlaylistSong
                        )
                    }
                }
            }
        }
        builder.setNeutralButton(R.string.new_playlist) { _: DialogInterface?, _: Int ->
            // currentContextPlaylist needs to be null for FragmentEditPlaylist to make a new playlist
            unselectSongs()
            viewModelActivityMain.playlistToEdit = null
            val songs = mutableListOf<Song>()
            if (song != null) {
                songs.add(song)
            }
            if (randomPlaylist != null) {
                for (songInPlaylist in randomPlaylist.getSongs()) {
                    songs.add(songInPlaylist)
                }
            }
            viewModelDialogFragmentAddToPlaylist.startNewPlaylistWithSongs(songs)
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

    private fun unselectSongs() {
        val songs = viewModelActivityMain.getAllSongs()
        for (s in songs) {
            s.setSelected(false)
        }
    }

    companion object {
        const val BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST: String = "ADD_TO_PLAYLIST_PLAYLIST"
        const val BUNDLE_KEY_ADD_TO_PLAYLIST_SONG: String = "ADD_TO_PLAYLIST_SONG"
        const val TAG = "DialogFragmentAddToPlaylist"
    }
}