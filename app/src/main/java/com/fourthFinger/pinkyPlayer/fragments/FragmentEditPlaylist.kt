package com.fourthFinger.pinkyPlayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.KeyboardUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentEditPlaylistBinding

class FragmentEditPlaylist : Fragment() {

    private var _binding: FragmentEditPlaylistBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelUserPicks by activityViewModels<ViewModelUserPicks>()

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
        binding.buttonEditSongs.setOnClickListener {
            viewModelUserPicks.editSongsClicked(
                NavHostFragment.findNavController(this)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateFAB()
    }

    private fun updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_black_24dp)
        viewModelActivityMain.setFABText(R.string.fab_save)
        viewModelActivityMain.showFab(true)
        val editTextPlaylistName: EditText = binding.editTextPlaylistName
        viewModelUserPicks.getUserPickedPlaylist()?.let {
            if(editTextPlaylistName.text.isEmpty()) {
                editTextPlaylistName.setText(it.getName())
            }
        }
        viewModelActivityMain.setFabOnClickListener { view: View ->
            viewModelUserPicks.editPlaylistFabClicked(
                this,
                editTextPlaylistName.text.toString()
            )
            cleanUp(view)
        }
    }

    private fun cleanUp(view: View) {
        KeyboardUtil.hideKeyboard(view)
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