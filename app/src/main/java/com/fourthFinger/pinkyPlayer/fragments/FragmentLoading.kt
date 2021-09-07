package com.fourthFinger.pinkyPlayer.fragments

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.os.HandlerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentLoadingBinding
import com.fourthFinger.pinkyPlayer.media_controller.MediaData
import com.fourthFinger.pinkyPlayer.media_controller.ViewModelFragmentLoading
import kotlin.math.roundToInt

class FragmentLoading : Fragment() {

    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelFragmentLoading by activityViewModels<ViewModelFragmentLoading>()

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
        viewModelFragmentLoading.loadingProgress
            .observe(viewLifecycleOwner) { loadingProgress: Int ->
                val progressBar: ProgressBar = binding.progressBarLoading
                progressBar.post {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        progressBar.setProgress(loadingProgress, true)
                    } else {
                        progressBar.progress = loadingProgress
                    }
                }
            }
        viewModelFragmentLoading.loadingText
            .observe(viewLifecycleOwner) { loadingText: String? ->
                val textView: TextView = binding.textViewLoading
                textView.post { textView.text = loadingText }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        askForPermissionAndCreateMediaController()
    }

    private fun askForPermissionAndCreateMediaController() {
        val appContext = requireActivity().applicationContext
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    permissionGranted()
                } else {
                    Toast.makeText(
                        appContext,
                        R.string.permission_read_needed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        when {
            (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) -> {
                permissionGranted()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                Toast.makeText(
                    appContext,
                    R.string.permission_read_needed,
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun permissionGranted() {
        viewModelFragmentLoading.permissionGranted()
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