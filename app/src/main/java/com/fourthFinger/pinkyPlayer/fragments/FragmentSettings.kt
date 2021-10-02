package com.fourthFinger.pinkyPlayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentSettingsBinding

class FragmentSettings : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelSettings by activityViewModels<ViewModelSettings>()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelActivityMain.setFabOnClickListener(null)
    }

}