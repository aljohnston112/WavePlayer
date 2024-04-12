package com.fourthFinger.pinkyPlayer.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentLoadingBinding


class FragmentLoading : Fragment() {

    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>{
        ViewModelActivityMain.Factory
    }
    private val viewModelFragmentLoading by activityViewModels<ViewModelFragmentLoading>{
        ViewModelFragmentLoading.Factory
    }

    // TODO don't show loading screen when user denies permission
    // TODO rerun scan after user comes back from settings screen

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setUpObservers()
        requestPermissionsAndLoadMusicFiles()
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

    private fun requestPermissionsAndLoadMusicFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(Manifest.permission.READ_MEDIA_AUDIO)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Requests a permission.
     *
     * @param permission The [Manifest.permission] to request.
     */
    private fun requestPermission(permission: String) {

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if(permission != Manifest.permission.POST_NOTIFICATIONS) {
                    permissionGranted()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.permission_read_needed,
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts(
                    "package",
                    requireActivity().packageName,
                    null
                )
                intent.data = uri
                startActivity(intent)
            }
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                if(Manifest.permission.POST_NOTIFICATIONS != permission) {
                    permissionGranted()
                }
            }

            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    requireContext(),
                    R.string.permission_read_needed,
                    Toast.LENGTH_LONG
                ).show()
                // TODO these pollute the backstack
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts(
                    "package",
                    requireActivity().packageName,
                    null
                )
                intent.data = uri
                startActivity(intent)
            }

            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }

    }

    private fun permissionGranted() {
        viewModelFragmentLoading.permissionGranted(requireActivity())
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