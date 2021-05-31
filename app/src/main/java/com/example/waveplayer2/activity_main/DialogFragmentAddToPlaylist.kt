package com.example.waveplayer2.activity_main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.waveplayer2.R
import com.example.waveplayer2.ViewModelUserPickedPlaylist
import com.example.waveplayer2.ViewModelUserPickedSongs
import com.example.waveplayer2.media_controller.MediaData
import com.example.waveplayer2.random_playlist.RandomPlaylist
import com.example.waveplayer2.random_playlist.Song
import java.util.*

class DialogFragmentAddToPlaylist : DialogFragment() {

    // TODO add a cancel button
    private val viewModelUserPickedSongs by activityViewModels<ViewModelUserPickedSongs>()
    private val viewModelUserPickedPlaylist by activityViewModels<ViewModelUserPickedPlaylist>()

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
        val mediaData = MediaData(requireActivity().applicationContext)
        builder.setMultiChoiceItems(getPlaylistTitles(mediaData.getPlaylists()),
                null){ _: DialogInterface?, which: Int, isChecked: Boolean ->
            if (isChecked) {
                selectedPlaylistIndices.add(which)
            } else {
                selectedPlaylistIndices.remove(Integer.valueOf(which))
            }
        }
    }

    // TODO put in MediaData at some point...
    private fun getPlaylistTitles(randomPlaylists: List<RandomPlaylist>): Array<String?> {
        val titles: MutableList<String> = ArrayList(randomPlaylists.size)
        for (randomPlaylist in randomPlaylists) {
            titles.add(randomPlaylist.getName())
        }
        val titlesArray = arrayOfNulls<String>(titles.size)
        var i = 0
        for (title in titles) {
            titlesArray[i++] = title
        }
        return titlesArray
    }

    private fun setUpButtons(builder: AlertDialog.Builder, bundle: Bundle,
                             selectedPlaylistIndices: MutableList<Int>) {
        val activityMain = requireActivity() as ActivityMain
        val mediaData = MediaData(requireActivity().applicationContext)
        // These are here to prevent code duplication
        val song = bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG) as Song?
        val randomPlaylist = bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST) as RandomPlaylist?
        builder.setPositiveButton(R.string.add){ _: DialogInterface?, _: Int ->
            if (song != null) {
                for (index in selectedPlaylistIndices) {
                    mediaData.getPlaylists()[index].add(song)
                }
            }
            if (randomPlaylist != null) {
                for (randomPlaylistSong in randomPlaylist.getSongs()) {
                    for (index in selectedPlaylistIndices) {
                        mediaData.getPlaylists()[index].add(randomPlaylistSong)
                    }
                }
            }
        }
        builder.setNeutralButton(R.string.new_playlist){ _: DialogInterface?, _: Int ->
            // UserPickedPlaylist need to be null for FragmentEditPlaylist to make a new playlist
            viewModelUserPickedPlaylist.setUserPickedPlaylist(null)
            viewModelUserPickedSongs.clearUserPickedSongs()
            if (song != null) {
                viewModelUserPickedSongs.addUserPickedSong(song)
            }
            if (randomPlaylist != null) {
                for (songInPlaylist in randomPlaylist.getSongs()) {
                    viewModelUserPickedSongs.addUserPickedSong(songInPlaylist)
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