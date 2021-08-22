package com.fourthFinger.pinkyPlayer.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ViewModelUserPickedPlaylist
import com.fourthFinger.pinkyPlayer.ViewModelUserPickedSongs
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentEditPlaylistBinding
import com.fourthFinger.pinkyPlayer.media_controller.MediaData
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import java.util.*

class FragmentEditPlaylist : Fragment() {

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelUserPickedPlaylist by activityViewModels<ViewModelUserPickedPlaylist>()
    private val viewModelUserPickedSongs by activityViewModels<ViewModelUserPickedSongs>()
    private lateinit var mediaData: MediaData

    private var _binding: FragmentEditPlaylistBinding? = null
    private val binding get() = _binding!!

    private var broadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaData = MediaData.getInstance(requireActivity().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.edit_playlist))
        _binding = FragmentEditPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val randomPlaylist: RandomPlaylist? = viewModelUserPickedPlaylist.getUserPickedPlaylist()
        if (randomPlaylist != null) {
            for (song in randomPlaylist.getSongs()) {
                viewModelUserPickedSongs.addUserPickedSong(song)
            }
        }
        // TODO don't think this is needed
        //  updateFAB();
        binding.buttonEditSongs.setOnClickListener() {
            NavHostFragment.findNavController(this).navigate(
                FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs()
            )
        }

    }

    private fun setUpBroadcastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(
            activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected
            )
        )
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action: String? = intent.action
                if (action != null) {
                    if (action == resources.getString(
                            R.string.broadcast_receiver_action_service_connected
                        )
                    ) {
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
        val userPickedPlaylist = viewModelUserPickedPlaylist.getUserPickedPlaylist()
        val userPickedSongs = viewModelUserPickedSongs.getUserPickedSongs().toMutableList()
        // userPickedPlaylist is null when user is making a new playlist
        if (userPickedPlaylist != null) {
            // userPickedSongs.isEmpty() when the user is editing a playlist
            // TODO if user is editing a playlist, unselects all the songs and returns here, ERROR
            if (userPickedSongs.isEmpty()) {
                viewModelUserPickedPlaylist.getUserPickedPlaylist()?.getSongs()
                    ?.let { userPickedSongs.addAll(it) }
                finalEditTextPlaylistName.setText(userPickedPlaylist.getName())
            }
        }
        viewModelActivityMain.setFabOnClickListener { view: View ->
            val activityMain: ActivityMain = requireActivity() as ActivityMain
            val playlistIndex = indexOfPlaylistWName(finalEditTextPlaylistName.text.toString())
            if (userPickedSongs.size == 0) {
                activityMain.showToast(R.string.not_enough_songs_for_playlist)
            } else if (finalEditTextPlaylistName.length() == 0) {
                activityMain.showToast(R.string.no_name_playlist)
            } else if (playlistIndex != -1 && userPickedPlaylist == null) {
                activityMain.showToast(R.string.duplicate_name_playlist)
            } else if (userPickedPlaylist == null) {
                // userPickedPlaylist is null when user is making a new playlist
                mediaData.addPlaylist(
                    RandomPlaylist(
                        finalEditTextPlaylistName.text.toString(),
                        userPickedSongs,
                        false
                    )
                )
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
                for (randomPlaylist in mediaData.getPlaylists()) {
                    playlistNames.add(randomPlaylist.getName())
                }
                if (userPickedPlaylist.getName() ==
                    finalEditTextPlaylistName.text.toString() ||
                    !playlistNames.contains(finalEditTextPlaylistName.text.toString())
                ) {
                    userPickedPlaylist.setName(finalEditTextPlaylistName.text.toString())
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
    }

    private fun indexOfPlaylistWName(playlistName: String): Int {
        var playlistIndex = -1
        for ((i, randomPlaylist) in mediaData.getPlaylists().withIndex()) {
            if (randomPlaylist.getName() == playlistName) {
                playlistIndex = i
            }
        }
        return playlistIndex
    }

    override fun onStart() {
        super.onStart()
        setUpBroadcastReceiver()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelActivityMain.setFabOnClickListener(null)
    }

}