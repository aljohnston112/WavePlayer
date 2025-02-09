package io.fourth_finger.pinky_player.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.activity_main.MenuActionIndex
import io.fourth_finger.pinky_player.activity_main.ViewModelActivityMain
import io.fourth_finger.pinky_player.databinding.FragmentSettingsBinding

class FragmentSettings : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>{
        ViewModelActivityMain.Factory
    }
    private val viewModelSettings by activityViewModels<ViewModelSettings>{
        ViewModelSettings.Factory
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.settings))
        updateFAB()
        loadSettings()
    }

    private fun updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_black_24dp)
        viewModelActivityMain.setFABText(R.string.fab_save)
        viewModelActivityMain.setFabOnClickListener {
            val editTextNSongs: EditText = binding.editTextNSongs
            val editTextPercentChangeUp: EditText = binding.editTextPercentChangeUp
            val editTextPercentChangeDown: EditText = binding.editTextPercentChangeDown
            viewModelSettings.fabClicked(
                requireActivity().applicationContext,
                NavHostFragment.findNavController(this),
                editTextNSongs.text.toString().toInt(),
                editTextPercentChangeUp.text.toString().toInt(),
                editTextPercentChangeDown.text.toString().toInt()
            )
        }
        viewModelActivityMain.showFab(true)
    }

    private fun loadSettings() {
        val editTextNSongs: EditText = binding.editTextNSongs
        val editTextPercentChangeUp: EditText = binding.editTextPercentChangeUp
        val editTextPercentChangeDown: EditText = binding.editTextPercentChangeDown
        editTextNSongs.setText(viewModelSettings.getMaxNumberOfSongs())
        editTextPercentChangeUp.setText(viewModelSettings.getPercentChangeUp())
        editTextPercentChangeDown.setText(viewModelSettings.getPercentChangeDown())
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpMenu()
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