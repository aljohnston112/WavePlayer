package com.fourthFinger.pinkyPlayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.KeyboardUtil
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentEditPlaylistBinding
import com.fourthFinger.pinkyPlayer.media_controller.MediaData
import com.fourthFinger.pinkyPlayer.media_controller.SaveFile
import java.util.*

class FragmentEditPlaylist : Fragment() {

    private var _binding: FragmentEditPlaylistBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>()
    private val viewModelFragmentEditPlaylist: ViewModelFragmentEditPlaylist by viewModels()

    private lateinit var mediaData: MediaData

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
        viewModelPlaylists.fragmentEditPlaylistViewCreated()
        binding.buttonEditSongs.setOnClickListener {
            viewModelFragmentEditPlaylist.editSongsClicked(
                NavHostFragment.findNavController(this)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateFAB()
        viewModelPlaylists.fragmentEditPlaylistOnResume()
    }

    private fun updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_black_24dp)
        viewModelActivityMain.setFABText(R.string.fab_save)
        viewModelActivityMain.showFab(true)

        val editTextPlaylistName: EditText = binding.editTextPlaylistName
        val userPickedPlaylist = viewModelPlaylists.getUserPickedPlaylist()
        val makingNewPlaylist = (userPickedPlaylist == null)
        editTextPlaylistName.setText(userPickedPlaylist!!.getName())

        val userPickedSongs = viewModelPlaylists.getUserPickedSongs().toMutableList()
        viewModelActivityMain.setFabOnClickListener { view: View ->
            if (validateInput()) {
                if (makingNewPlaylist) {
                    viewModelPlaylists.addNewPlaylistWithUserPickedSongs(
                        editTextPlaylistName.text.toString(),
                    )
                    cleanUp(view)
                } else if (!makingNewPlaylist) {
                    val playlistNames = ArrayList<String?>()
                    for (randomPlaylist in viewModelPlaylists.getPlaylists()) {
                        playlistNames.add(randomPlaylist.getName())
                    }
                    if (userPickedPlaylist.getName() ==
                        editTextPlaylistName.text.toString() ||
                        !playlistNames.contains(editTextPlaylistName.text.toString())
                    ) {
                        userPickedPlaylist.setName(editTextPlaylistName.text.toString())
                        for (song in userPickedPlaylist.getSongs()) {
                            if (!userPickedSongs.contains(song)) {
                                userPickedPlaylist.remove(song)
                            }
                        }
                        for (song in userPickedSongs) {
                            userPickedPlaylist.add(song)
                            song.setSelected(false)
                        }
                        cleanUp(view)
                    }
                }
            }
        }
    }

    private fun validateInput(): Boolean {
        val editTextPlaylistName: EditText = binding.editTextPlaylistName
        return viewModelPlaylists.validateInput(
            requireActivity().applicationContext,
            editTextPlaylistName.text.toString()
        )
    }

    private fun cleanUp(view: View) {
        SaveFile.saveFile(requireActivity().applicationContext)
        KeyboardUtil.hideKeyboard(view)
        NavUtil.popBackStack(this)
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