package com.example.waveplayer.activity_main

import android.app.*
import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.waveplayer.R
import com.example.waveplayer.random_playlist.RandomPlaylist
import com.example.waveplayer.random_playlist.Song
import java.util.*

class DialogFragmentAddToPlaylist : DialogFragment() {
    private var onMultiChoiceClickListener: OnMultiChoiceClickListener? = null

    // TODO add a cancel button
    private var onClickListenerAddButton: DialogInterface.OnClickListener? = null
    private var onClickListenerNewPlaylistButton: DialogInterface.OnClickListener? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.add_to_playlist)
        val bundle = arguments
        if (bundle != null) {
            val selectedPlaylistIndices: MutableList<Int?> = ArrayList()
            setUpChoices(builder, selectedPlaylistIndices)
            setUpButtons(builder, bundle, selectedPlaylistIndices)
            return builder.create()
        }
        return null
    }

    private fun setUpChoices(builder: AlertDialog.Builder?, selectedPlaylistIndices: MutableList<Int?>?) {
        val activityMain = requireActivity() as ActivityMain
        onMultiChoiceClickListener = OnMultiChoiceClickListener { dialog: DialogInterface?, which: Int, isChecked: Boolean ->
            if (isChecked) {
                selectedPlaylistIndices.add(which)
            } else {
                selectedPlaylistIndices.remove(Integer.valueOf(which))
            }
        }
        builder.setMultiChoiceItems(getPlaylistTitles(activityMain.playlists),
                null, onMultiChoiceClickListener)
    }

    // TODO put in MediaData at some point...
    private fun getPlaylistTitles(randomPlaylists: MutableList<RandomPlaylist?>?): Array<String?>? {
        val titles: MutableList<String?> = ArrayList(randomPlaylists.size)
        for (randomPlaylist in randomPlaylists) {
            titles.add(randomPlaylist.getName())
        }
        val titlesArray = arrayOfNulls<String?>(titles.size)
        var i = 0
        for (title in titles) {
            titlesArray[i++] = title
        }
        return titlesArray
    }

    private fun setUpButtons(builder: AlertDialog.Builder?, bundle: Bundle?,
                             selectedPlaylistIndices: MutableList<Int?>?) {
        val activityMain = requireActivity() as ActivityMain
        // These are here to prevent code duplication
        val song = bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG) as Song?
        val randomPlaylist = bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST) as RandomPlaylist?
        onClickListenerAddButton = DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
            if (song != null) {
                for (index in selectedPlaylistIndices) {
                    activityMain.playlists[index].add(song)
                }
            }
            if (randomPlaylist != null) {
                for (randomPlaylistSong in randomPlaylist.songs) {
                    for (index in selectedPlaylistIndices) {
                        activityMain.playlists[index].add(randomPlaylistSong)
                    }
                }
            }
        }
        builder.setPositiveButton(R.string.add, onClickListenerAddButton)
        onClickListenerNewPlaylistButton = DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
            // UserPickedPlaylist need to be null for FragmentEditPlaylist to make a new playlist
            activityMain.userPickedPlaylist = null
            activityMain.clearUserPickedSongs()
            if (song != null) {
                activityMain.addUserPickedSong(song)
            }
            if (randomPlaylist != null) {
                for (songInPlaylist in randomPlaylist.songs) {
                    activityMain.addUserPickedSong(songInPlaylist)
                }
            }
            activityMain.navigateTo(R.id.fragmentEditPlaylist)
        }
        builder.setNeutralButton(R.string.new_playlist, onClickListenerNewPlaylistButton)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onMultiChoiceClickListener = null
        onClickListenerAddButton = null
        onClickListenerNewPlaylistButton = null
    }

    companion object {
        // TODO get rid of bundles... probably not
        val BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST: String? = "ADD_TO_PLAYLIST_PLAYLIST"
        val BUNDLE_KEY_ADD_TO_PLAYLIST_SONG: String? = "ADD_TO_PLAYLIST_SONG"
    }
}