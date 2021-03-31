package com.example.waveplayer.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.example.waveplayer.R
import com.example.waveplayer.ViewModelUserPickedPlaylist
import com.example.waveplayer.ViewModelUserPickedSongs
import com.example.waveplayer.activity_main.ActivityMain
import com.example.waveplayer.activity_main.ViewModelActivityMain
import com.example.waveplayer.databinding.FragmentEditPlaylistBinding
import java.util.*

class FragmentEditPlaylist : Fragment() {
    private var binding: FragmentEditPlaylistBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var viewModelUserPickedSongs: ViewModelUserPickedSongs? = null
    private var viewModelUserPickedPlaylist: ViewModelUserPickedPlaylist? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var onClickListenerFAB: View.OnClickListener? = null
    private var onClickListenerButtonSelectSongs: View.OnClickListener? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        createViewModels()
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.edit_playlist))
        binding = FragmentEditPlaylistBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO don't think this is needed
        //  updateFAB();
        onClickListenerButtonSelectSongs = View.OnClickListener { button: View? ->
            NavHostFragment.findNavController(this).navigate(
                    FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs())
        }
        binding.buttonEditSongs.setOnClickListener(onClickListenerButtonSelectSongs)
        setUpBroadcastReceiver()
    }

    private fun createViewModels() {
        viewModelUserPickedSongs = ViewModelProvider(requireActivity()).get<ViewModelUserPickedSongs?>(ViewModelUserPickedSongs::class.java)
        viewModelUserPickedPlaylist = ViewModelProvider(requireActivity()).get<ViewModelUserPickedPlaylist?>(ViewModelUserPickedPlaylist::class.java)
        viewModelActivityMain = ViewModelProvider(requireActivity()).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
    }

    private fun setUpBroadcastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected))
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action: String = intent.getAction()
                if (action != null) {
                    if (action == resources.getString(
                                    R.string.broadcast_receiver_action_service_connected)) {
                        // TODO is this needed if onResume has it?
                        //  updateFAB();
                    }
                }
            }
        }
        activityMain.registerReceiver(broadcastReceiver, filterComplete)
    }

    override fun onResume() {
        super.onResume()
        updateFAB()
    }

    private fun updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_black_24dp)
        viewModelActivityMain.setFABText(R.string.fab_save)
        viewModelActivityMain.showFab(true)
        val finalEditTextPlaylistName: EditText = binding.editTextPlaylistName
        val userPickedPlaylist: RandomPlaylist = viewModelUserPickedPlaylist.getUserPickedPlaylist()
        val userPickedSongs: MutableList<Song?> = viewModelUserPickedSongs.getUserPickedSongs()
        // userPickedPlaylist is null when user is making a new playlist
        if (userPickedPlaylist != null) {
            // userPickedSongs.isEmpty() when the user is editing a playlist
            // TODO if user is editing a playlist, unselects all the songs and returns here, ERROR
            if (userPickedSongs.isEmpty()) {
                userPickedSongs.addAll(
                        viewModelUserPickedPlaylist.getUserPickedPlaylist().getSongs())
                finalEditTextPlaylistName.setText(userPickedPlaylist.getName())
            }
        }
        onClickListenerFAB = View.OnClickListener { view: View? ->
            val activityMain: ActivityMain = requireActivity() as ActivityMain
            val playlistIndex = indexOfPlaylistWName(finalEditTextPlaylistName.getText().toString())
            if (userPickedSongs.size == 0) {
                activityMain.showToast(R.string.not_enough_songs_for_playlist)
            } else if (finalEditTextPlaylistName.length() == 0) {
                activityMain.showToast(R.string.no_name_playlist)
            } else if (playlistIndex != -1 && userPickedPlaylist == null) {
                activityMain.showToast(R.string.duplicate_name_playlist)
            } else if (userPickedPlaylist == null) {
                // userPickedPlaylist is null when user is making a new playlist
                activityMain.addPlaylist(RandomPlaylist(finalEditTextPlaylistName.getText().toString(),
                        userPickedSongs, activityMain.getMaxPercent(), false))
                for (song in userPickedSongs) {
                    song.setSelected(false)
                }
                viewModelUserPickedSongs.clearUserPickedSongs()
                activityMain.saveFile()
                activityMain.hideKeyboard(view)
                activityMain.popBackStack(this)
            } else {
                // userPickedPlaylist is not null when the user is editing a playlist
                val playlistNames = ArrayList<String?>()
                for (randomPlaylist in activityMain.getPlaylists()) {
                    playlistNames.add(randomPlaylist.getName())
                }
                if (userPickedPlaylist.getName() ==
                        finalEditTextPlaylistName.getText().toString() ||
                        !playlistNames.contains(finalEditTextPlaylistName.getText().toString())) {
                    userPickedPlaylist.setName(finalEditTextPlaylistName.getText().toString())
                    for (song in userPickedPlaylist.getSongs()) {
                        if (!userPickedSongs.contains(song)) {
                            userPickedPlaylist.remove(song)
                        }
                    }
                    for (song in userPickedSongs) {
                        userPickedPlaylist.add(song)
                        song.setSelected(false)
                    }
                    activityMain.saveFile()
                    activityMain.hideKeyboard(view)
                    activityMain.popBackStack(this)
                } else {
                    activityMain.showToast(R.string.duplicate_name_playlist)
                }
            }
        }
        viewModelActivityMain.setFabOnClickListener(onClickListenerFAB)
    }

    private fun indexOfPlaylistWName(playlistName: String?): Int {
        val activityMain: ActivityMain = requireActivity()
        var playlistIndex = -1
        var i = 0
        for (randomPlaylist in activityMain.getPlaylists()) {
            if (randomPlaylist.getName() == playlistName) {
                playlistIndex = i
            }
            i++
        }
        return playlistIndex
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(broadcastReceiver)
        viewModelActivityMain.setFabOnClickListener(null)
        binding = null
    }
}