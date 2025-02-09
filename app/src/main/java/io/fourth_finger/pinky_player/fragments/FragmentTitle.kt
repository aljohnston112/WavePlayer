package io.fourth_finger.pinky_player.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.activity_main.MenuActionIndex
import io.fourth_finger.pinky_player.activity_main.ViewModelActivityMain
import io.fourth_finger.pinky_player.databinding.FragmentTitleBinding

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

    private fun setUpMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater
            ) {
                menuInflater.inflate(
                    R.menu.menu_toolbar,
                    menu
                )
                for (menuActionIndex in MenuActionIndex.entries) {
                    val menuItem = menu.getItem(menuActionIndex.ordinal)
                    val songInProgress = viewModelActivityMain.songInProgress.value == true
                    when (menuActionIndex) {
                        MenuActionIndex.MENU_ACTION_ADD_TO_PLAYLIST_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_QUEUE_INDEX -> {
                            menuItem.isVisible = songInProgress
                        }

                        MenuActionIndex.MENU_ACTION_SEARCH_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_ADD_TO_QUEUE_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_LOWER_PROBABILITIES_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_RESET_PROBABILITIES_INDEX -> {
                            menuItem.isVisible = false
                        }
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        })
    }

    private fun setUpButtons() {
        binding.buttonPlaylists.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonSongs.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonSettings.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonFolderSearch.setOnClickListener(onClickListenerFragmentTitleButtons)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpMenu()
        viewModelFragmentTitle.viewCreated()
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.app_name))
        viewModelActivityMain.showFab(false)
        setUpButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}