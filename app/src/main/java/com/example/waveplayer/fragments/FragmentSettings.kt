package com.example.waveplayer.fragments

import android.view.View
import androidx.fragment.app.Fragment
import com.example.waveplayer.databinding.FragmentSettingsBinding

class FragmentSettings : Fragment() {
    private var binding: FragmentSettingsBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var onClickListenerFAB: View.OnClickListener? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewModelActivityMain = ViewModelProvider(requireActivity()).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
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
        onClickListenerFAB = label@ View.OnClickListener { view: View? ->
            val nSongs = getNSongs()
            if (nSongs == -1) {
                return@label
            }
            val percentChangeUp = getPercentChangeUp()
            if (percentChangeUp == -1) {
                return@label
            }
            val percentChangeDown = getPercentChangeDown()
            if (percentChangeDown == -1) {
                return@label
            }
            updateSettings(nSongs, percentChangeUp, percentChangeDown)
            val navController: NavController = NavHostFragment.findNavController(this)
            if (navController.getCurrentDestination().getId() == R.id.FragmentSettings) {
                navController.popBackStack()
            }
        }
        viewModelActivityMain.setFabOnClickListener(onClickListenerFAB)
        viewModelActivityMain.showFab(true)
    }

    private fun getNSongs(): Int {
        val editTextNSongs: EditText = binding.editTextNSongs
        var nSongs = -1
        try {
            nSongs = editTextNSongs.getText().toString().toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        if (nSongs < 1) {
            val activityMain: ActivityMain = requireActivity() as ActivityMain
            activityMain.showToast(R.string.max_percent_error)
            nSongs = -1
        }
        return nSongs
    }

    private fun getPercentChangeUp(): Int {
        val editTextPercentChangeUp: EditText = binding.editTextPercentChangeUp
        var percentChangeUp = -1
        try {
            percentChangeUp = editTextPercentChangeUp.getText().toString().toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        if (percentChangeUp < 1 || percentChangeUp > 100) {
            val activityMain: ActivityMain = requireActivity() as ActivityMain
            activityMain.showToast(R.string.percent_change_error)
            percentChangeUp = -1
        }
        return percentChangeUp
    }

    private fun getPercentChangeDown(): Int {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val editTextPercentChangeDown: EditText = binding.editTextPercentChangeDown
        var percentChangeDown = -1
        try {
            percentChangeDown = editTextPercentChangeDown.getText().toString().toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        if (percentChangeDown < 1 || percentChangeDown > 100) {
            activityMain.showToast(R.string.percent_change_error)
            percentChangeDown = -1
        }
        return percentChangeDown
    }

    private fun updateSettings(nSongs: Int, percentChangeUp: Int, percentChangeDown: Int) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val maxPercent = 1.0 / nSongs as Double
        activityMain.setMaxPercent(maxPercent)
        activityMain.setPercentChangeUp(percentChangeUp as Double / 100.0)
        activityMain.setPercentChangeDown(percentChangeDown as Double / 100.0)
        activityMain.saveFile()
    }

    private fun loadSettings() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val editTextNSongs: EditText = binding.editTextNSongs
        val editTextPercentChangeUp: EditText = binding.editTextPercentChangeUp
        val editTextPercentChangeDown: EditText = binding.editTextPercentChangeDown
        editTextNSongs.setText(Math.round(1.0 / activityMain.getMaxPercent()) as Int.toString())
        editTextPercentChangeUp.setText(Math.round(activityMain.getPercentChangeUp() * 100.0) as Int.toString())
        editTextPercentChangeDown.setText(Math.round(activityMain.getPercentChangeDown() * 100.0) as Int.toString())
    }

    override fun onDestroy() {
        super.onDestroyView()
        viewModelActivityMain.setFabOnClickListener(null)
        onClickListenerFAB = null
        viewModelActivityMain = null
        binding = null
    }
}