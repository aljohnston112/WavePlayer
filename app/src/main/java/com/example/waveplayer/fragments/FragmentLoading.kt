package com.example.waveplayer.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.os.HandlerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.waveplayer.R
import com.example.waveplayer.activity_main.ViewModelActivityMain
import com.example.waveplayer.databinding.FragmentLoadingBinding
import com.example.waveplayer.media_controller.MediaData
import com.example.waveplayer.media_controller.ViewModelFragmentLoading
import kotlin.math.roundToInt

private const val REQUEST_CODE_PERMISSION: Short = 245

class FragmentLoading : Fragment() {

    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelFragmentLoading by activityViewModels<ViewModelFragmentLoading>()

    private var handler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())
    private var runnableAskForPermission = Runnable { askForPermissionAndCreateMediaController() }

    private fun askForPermissionAndCreateMediaController() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val context = requireActivity().applicationContext
            if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_PERMISSION.toInt())
            } else {
                permissionGranted()
            }
        } else {
            permissionGranted()
        }
    }

    private fun permissionGranted() {
        MediaData.getInstance(requireContext().applicationContext).loadData(requireContext().applicationContext)
    }

    @SuppressLint("ShowToast")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val appContext = requireActivity().applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (
                    requestCode == REQUEST_CODE_PERMISSION.toInt() &&
                    grantResults.size > 1 &&
                    (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED)
            ) {
                val toast = if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(appContext, R.string.permission_read_needed, Toast.LENGTH_LONG)
                } else {
                    Toast.makeText(appContext, R.string.permission_write_needed, Toast.LENGTH_LONG)
                }
                toast.show()
                handler.postDelayed(runnableAskForPermission, 1000)
            } else {
                permissionGranted()
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        setUpObservers()
        _binding = FragmentLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setUpObservers() {
        viewModelFragmentLoading.getLoadingProgress().observe(viewLifecycleOwner) { loadingProgress: Double? ->
            if (loadingProgress != null) {
                val progressBar: ProgressBar = binding.progressBarLoading
                progressBar.post {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        progressBar.setProgress((loadingProgress * 100.0).roundToInt(), true)
                    } else {
                        progressBar.progress = (loadingProgress * 100.0).roundToInt()
                    }
                }
            }
        }
        viewModelFragmentLoading.getLoadingText().observe(viewLifecycleOwner) { loadingText: String? ->
            val textView: TextView = binding.textViewLoading
            textView.post { textView.text = loadingText }
        }
    }

    override fun onResume() {
        super.onResume()
        updateMainContent()
    }

    private fun updateMainContent() {
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.loading))
        viewModelActivityMain.showFab(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}