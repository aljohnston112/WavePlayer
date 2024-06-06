package io.fourthFinger.pinkyPlayer.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import io.fourthFinger.pinkyPlayer.databinding.FragmentTitleBinding

class FragmentTitle : Fragment() {

    private var _binding: FragmentTitleBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain> {
        ViewModelActivityMain.Factory
    }
    private val viewModelFragmentTitle by viewModels<ViewModelFragmentTitle> {
        ViewModelFragmentTitle.Factory
    }

    private lateinit var getUri: ActivityResultLauncher<Uri?>

    private val onClickListenerFragmentTitleButtons: View.OnClickListener =
        View.OnClickListener { view: View ->
            if (view.id != R.id.button_folder_search) {
                viewModelFragmentTitle.buttonClicked(
                    view.id,
                    NavHostFragment.findNavController(this)
                )
            } else {
                getUri.launch(null)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getUri = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            // TODO why is this needed?
            binding.buttonFolderSearch.setOnClickListener(
                onClickListenerFragmentTitleButtons
            )
            if (uri != null) {
                viewModelFragmentTitle.createPlaylistFromFolder(
                    this,
                    uri
                )
            }
        }
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
        viewModelFragmentTitle.viewCreated()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}