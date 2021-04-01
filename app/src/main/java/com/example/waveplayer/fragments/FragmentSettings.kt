package com.example.waveplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.waveplayer.R
import com.example.waveplayer.activity_main.ActivityMain
import com.example.waveplayer.activity_main.ViewModelActivityMain
import com.example.waveplayer.databinding.FragmentSettingsBinding
import com.example.waveplayer.media_controller.MediaData
import kotlin.math.roundToInt

class FragmentSettings : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()

    private lateinit var mediaData: MediaData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaData = MediaData.getInstance(requireActivity().applicationContext)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        viewModelActivityMain.setFabOnClickListener{
            val nSongs = getNSongs()
            if (nSongs == -1) {
                return@setFabOnClickListener
            }
            val percentChangeUp = getPercentChangeUp()
            if (percentChangeUp == -1) {
                return@setFabOnClickListener
            }
            val percentChangeDown = getPercentChangeDown()
            if (percentChangeDown == -1) {
                return@setFabOnClickListener
            }
            updateSettings(nSongs, percentChangeUp, percentChangeDown)
            val navController: NavController = NavHostFragment.findNavController(this)
            if (navController.currentDestination?.id == R.id.FragmentSettings) {
                navController.popBackStack()
            }
        }
        viewModelActivityMain.showFab(true)
    }

    private fun getNSongs(): Int {
        val editTextNSongs: EditText = binding.editTextNSongs
        var nSongs = -1
        try {
            nSongs = editTextNSongs.text.toString().toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        if (nSongs < 1) {
            (requireActivity() as ActivityMain).showToast(R.string.max_percent_error)
            nSongs = -1
        }
        return nSongs
    }

    private fun getPercentChangeUp(): Int {
        val editTextPercentChangeUp: EditText = binding.editTextPercentChangeUp
        var percentChangeUp = -1
        try {
            percentChangeUp = editTextPercentChangeUp.text.toString().toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        if (percentChangeUp < 1 || percentChangeUp > 100) {
            (requireActivity() as ActivityMain).showToast(R.string.percent_change_error)
            percentChangeUp = -1
        }
        return percentChangeUp
    }

    private fun getPercentChangeDown(): Int {
        val editTextPercentChangeDown: EditText = binding.editTextPercentChangeDown
        var percentChangeDown = -1
        try {
            percentChangeDown = editTextPercentChangeDown.text.toString().toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        if (percentChangeDown < 1 || percentChangeDown > 100) {
            (requireActivity() as ActivityMain).showToast(R.string.percent_change_error)
            percentChangeDown = -1
        }
        return percentChangeDown
    }

    private fun updateSettings(nSongs: Int, percentChangeUp: Int, percentChangeDown: Int) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val maxPercent = 1.0 / nSongs.toDouble()
        mediaData.setMaxPercent(maxPercent)
        mediaData.setPercentChangeUp(percentChangeUp.toDouble() / 100.0)
        mediaData.setPercentChangeDown(percentChangeDown.toDouble() / 100.0)
        activityMain.saveFile()
    }

    private fun loadSettings() {
        val editTextNSongs: EditText = binding.editTextNSongs
        val editTextPercentChangeUp: EditText = binding.editTextPercentChangeUp
        val editTextPercentChangeDown: EditText = binding.editTextPercentChangeDown
        editTextNSongs.setText((1.0 / mediaData.getMaxPercent()).roundToInt().toString())
        editTextPercentChangeUp.setText((mediaData.getPercentChangeUp() * 100.0).roundToInt().toString())
        editTextPercentChangeDown.setText((mediaData.getPercentChangeDown() * 100.0).roundToInt().toString())
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