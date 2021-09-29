package com.fourthFinger.pinkyPlayer.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentTitleBinding

class FragmentTitle : Fragment() {

    private var _binding: FragmentTitleBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelFragmentTitle by viewModels<ViewModelFragmentTitle>()
    private val viewModelUserPicks by activityViewModels<ViewModelUserPicks>()

    private lateinit var getUri: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getUri = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
            activityResultCallback
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTitleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModelFragmentTitle.onViewCreated()
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.app_name))
        viewModelActivityMain.showFab(false)
        setUpButtons()
    }

    private fun setUpButtons() {
        binding.buttonPlaylists.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonSongs.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonSettings.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonFolderSearch.setOnClickListener(onClickListenerFragmentTitleButtons)
    }

    private val onClickListenerFragmentTitleButtons: View.OnClickListener =
        View.OnClickListener { view: View ->
            when (view.id) {
                R.id.button_playlists -> {
                    viewModelFragmentTitle.playlistsClicked(
                        NavHostFragment.findNavController(this)
                    )
                }
                R.id.button_songs -> {
                    viewModelFragmentTitle.songsClicked(
                        NavHostFragment.findNavController(this)
                    )
                }
                R.id.button_settings -> {
                    viewModelFragmentTitle.settingsClicked(
                        NavHostFragment.findNavController(this)
                    )
                }
                R.id.button_folder_search -> {
                    // TODO why is this needed?
                    // binding.buttonFolderSearch.setOnClickListener(null)
                     getUri.launch(null)
                }
            }
        }

    private val activityResultCallback = ActivityResultCallback<Uri> { uri ->
        binding.buttonFolderSearch.setOnClickListener(
            onClickListenerFragmentTitleButtons
        )
        val playlist = viewModelFragmentTitle.createPlaylist(
            requireActivity().applicationContext,
            requireActivity().contentResolver,
            uri
        )
        viewModelUserPicks.playlistCreatedFromFolder(
            NavHostFragment.findNavController(this@FragmentTitle),
            playlist
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}